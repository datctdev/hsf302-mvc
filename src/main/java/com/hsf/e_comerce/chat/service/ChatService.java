package com.hsf.e_comerce.chat.service;

import com.hsf.e_comerce.chat.config.ChatProperties;
import com.hsf.e_comerce.chat.dto.ChatResponse;
import com.hsf.e_comerce.chat.dto.ProductSuggestionDto;
import com.hsf.e_comerce.product.dto.response.CategoryResponse;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ProductService productService;
    private final ChatProperties chatProperties;
    @Qualifier("ollamaRestTemplate")
    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int MAX_SUGGESTIONS = 8;
    private static final String FALLBACK_REPLY = "Chào anh/chị! Em có thể giúp tìm sản phẩm phù hợp. Anh/chị cứ nói nhu cầu (ví dụ: \"tôi cần tìm điện thoại\", \"áo nam\", \"giày thể thao\") hoặc xem danh mục tại trang Sản phẩm ạ.";
    private static final String CONVERSATIONAL_FALLBACK = "Dạ chào anh/chị! Em là trợ lý mua sắm. Anh/chị cần tìm sản phẩm gì hoặc có câu hỏi gì cứ nhắn em ạ.";

    /** Tin nhắn chỉ chào hỏi / nói chuyện ngoài lề — không coi là tìm sản phẩm. */
    private static final Set<String> CONVERSATIONAL_PHRASES = Set.of(
            "chào", "xin chào", "chào bạn", "chào shop", "chào em", "chào anh", "chào chị", "em chào", "anh chào",
            "hello", "hi", "hey", "halo", "alô", "alo",
            "cảm ơn", "cám ơn", "thanks", "thank you", "cảm ơn bạn", "cảm ơn em",
            "tạm biệt", "bye", "bai", "hẹn gặp lại", "see you",
            "khỏe không", "bạn khỏe không", "có ai không", "có người không",
            "ok", "oke", "ừ", "ừa", "vâng", "dạ", "ạ"
    );

    private static final List<String> INTENT_PREFIXES = List.of(
            "tôi cần tìm ", "tìm giúp tôi ", "tìm giúp ", "cho tôi ", "cho tớ ", "tôi muốn tìm ",
            "muốn mua ", "cần mua ", "tôi cần mua ", "bạn có ", "có bán ", "có ", "tìm giúp em ",
            "em cần tìm ", "anh cần tìm ", "chị cần tìm ", "giúp tôi tìm ", "giúp em tìm ",
            "xem giúp ", "cho xem ", "gợi ý ", "gợi ý giúp ", "tư vấn ", "cần tìm "
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "tôi", "tớ", "em", "anh", "chị", "mình", "tao", "bạn", "các", "này", "đó", "kia",
            "cần", "tìm", "mua", "xem", "có", "không", "ạ", "nhé", "ơi", "một", "cái", "chiếc",
            "cho", "giúp", "với", "và", "hay", "hoặc", "đi", "nha", "nè"
    );
    private static final Pattern PATTERN_CO_X_KHONG = Pattern.compile("(?i)có\\s+(.+?)\\s+không\\s*$");

    /** True nếu tin nhắn chỉ là chào hỏi / cảm ơn / tạm biệt / nói chuyện ngoài lề, không phải yêu cầu tìm sản phẩm. */
    private boolean isConversationalOnly(String message) {
        if (message == null || message.isBlank()) return true;
        String n = message.trim().toLowerCase().replaceAll("\\s+", " ");
        if (n.isEmpty()) return true;
        if (CONVERSATIONAL_PHRASES.contains(n)) return true;
        if (n.length() <= 20 && CONVERSATIONAL_PHRASES.stream().anyMatch(p -> n.equals(p) || n.startsWith(p + " ") || n.startsWith(p + "!"))) return true;
        if (n.matches("^(chào|hello|hi|hey)\\s+.*") && n.length() <= 30) return true;
        return false;
    }

    private String extractSearchKeyword(String message) {
        if (message == null || message.isBlank()) return "";
        String s = message.trim();
        if (s.length() > 500) s = s.substring(0, 500);
        String lower = s.toLowerCase();
        for (String prefix : INTENT_PREFIXES) {
            if (lower.startsWith(prefix)) {
                s = s.substring(prefix.length()).trim();
                lower = s.toLowerCase();
                break;
            }
        }
        var matcher = PATTERN_CO_X_KHONG.matcher(s);
        if (matcher.find()) s = matcher.group(1).trim();
        String[] words = s.split("\\s+");
        StringBuilder kept = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (!STOP_WORDS.contains(w.toLowerCase())) kept.append(w).append(" ");
        }
        s = kept.toString().trim();
        return s.isEmpty() ? message.trim() : s;
    }

    private String toAbsoluteUrl(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    public ChatResponse chat(String userMessage) {
        String raw = userMessage != null ? userMessage.trim() : "";

        if (isConversationalOnly(raw)) {
            String reply = replyConversational(raw);
            return ChatResponse.builder()
                    .reply(reply)
                    .productSuggestions(Collections.emptyList())
                    .build();
        }

        String keyword = extractSearchKeyword(raw);
        if (keyword.length() > 500) keyword = keyword.substring(0, 500);

        List<CategoryResponse> categories = productService.findAllCategory();
        List<ProductResponse> searchResults = keyword.isEmpty()
                ? Collections.emptyList()
                : productService.searchProducts(keyword, 0, MAX_SUGGESTIONS).getContent();
        // Fallback 1: từ khóa nhiều từ -> thử từ đầu tiên (vd. "chuột máy tính" -> "chuột")
        if (searchResults.isEmpty() && keyword != null && !keyword.isBlank() && keyword.contains(" ")) {
            String firstWord = keyword.trim().split("\\s+")[0];
            if (!firstWord.isEmpty()) {
                searchResults = productService.searchProducts(firstWord, 0, MAX_SUGGESTIONS).getContent();
            }
        }
        // Fallback 2: vẫn trống -> thử lấy sản phẩm theo danh mục trùng tên (vd. danh mục "Chuột")
        if (searchResults.isEmpty() && keyword != null && !keyword.isBlank()) {
            String term = keyword.contains(" ") ? keyword.trim().split("\\s+")[0] : keyword.trim();
            java.util.Optional<CategoryResponse> cat = categories.stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(term.toLowerCase()))
                    .findFirst();
            if (cat.isPresent()) {
                searchResults = productService.getPublishedProducts(
                        0, MAX_SUGGESTIONS, null, cat.get().getId(), null, null, null, null, null
                ).getContent();
            }
        }

        List<ProductSuggestionDto> suggestions = searchResults.stream()
                .map(p -> {
                    String thumb = null;
                    if (p.getImages() != null && !p.getImages().isEmpty()) {
                        String url = p.getImages().get(0).getImageUrl();
                        thumb = toAbsoluteUrl(url);
                    }
                    return ProductSuggestionDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .basePrice(p.getBasePrice())
                            .categoryName(p.getCategoryName())
                            .productUrl(baseUrl + "/products/" + p.getId())
                            .thumbnailUrl(thumb)
                            .build();
                })
                .collect(Collectors.toList());

        String reply = replyForProducts(userMessage, categories, searchResults, keyword, suggestions);

        return ChatResponse.builder()
                .reply(reply)
                .productSuggestions(suggestions)
                .build();
    }

    private boolean useOllama() {
        String url = chatProperties.getOllamaBaseUrl();
        return url != null && !url.isBlank();
    }

    private String replyConversational(String userMessage) {
        if (useOllama()) {
            try {
                return callOllamaConversational(userMessage);
            } catch (Exception e) {
                logOllamaFailure(e, "conversational");
            }
        }
        return CONVERSATIONAL_FALLBACK;
    }

    private String replyForProducts(String userMessage, List<CategoryResponse> categories, List<ProductResponse> searchResults, String keyword, List<ProductSuggestionDto> suggestions) {
        if (useOllama()) {
            try {
                return callOllama(userMessage, categories, searchResults);
            } catch (Exception e) {
                logOllamaFailure(e, "products");
            }
        }
        return buildFallbackReply(keyword, suggestions);
    }

    /** Log lỗi Ollama và gợi ý pull model nếu 404 (model chưa có). */
    private void logOllamaFailure(Exception e, String context) {
        String msg = e.getMessage();
        log.warn("Ollama {} call failed: {}", context, msg);
        if (msg != null && msg.contains("404") && msg.contains("not found")) {
            log.info("Ollama model chưa được pull. Chạy: docker exec app-ollama ollama pull {}", chatProperties.getOllamaModel());
        }
    }

    /** Gọi Ollama (local) — API /api/chat. Prompt: không liệt kê sản phẩm trong tin nhắn, chỉ nói ngắn để khách xem thẻ bên dưới. */
    @SuppressWarnings("unchecked")
    private String callOllama(String userMessage, List<CategoryResponse> categories, List<ProductResponse> products) throws Exception {
        String categoryList = categories.stream().map(CategoryResponse::getName).collect(Collectors.joining(", "));
        String productContext = products.stream().limit(10)
                .map(p -> "- " + p.getName() + " (" + (p.getCategoryName() != null ? p.getCategoryName() : "N/A") + "): " + p.getBasePrice() + " VND.")
                .collect(Collectors.joining("\n"));
        String systemPrompt = "Bạn là trợ lý mua sắm. Xưng em, gọi khách anh/chị. QUAN TRỌNG: Trả lời BẰNG TIẾNG VIỆT, chỉ 1-2 câu ngắn. "
                + "KHÔNG được liệt kê tên sản phẩm hay giá trong tin nhắn. KHÔNG hỏi thêm \"anh/chị quan tâm loại nào\" hay \"cho thêm yêu cầu\". "
                + "Chỉ cần nói kiểu: \"Dạ em gợi ý anh/chị xem các sản phẩm bên dưới, bấm vào từng thẻ để xem chi tiết ạ.\" "
                + "Sản phẩm sẽ hiển thị dưới dạng thẻ bấm được ngay dưới tin nhắn, khách tự chọn. Danh mục có: " + categoryList + ". Sản phẩm tìm được (chỉ để tham khảo, không nhắc lại trong tin):\n" + productContext;
        return callOllamaChat(systemPrompt, userMessage, 120);
    }

    @SuppressWarnings("unchecked")
    private String callOllamaConversational(String userMessage) throws Exception {
        String systemPrompt = "Bạn là trợ lý mua sắm. Khách đang chào hoặc nói chuyện. Đáp lễ 1-2 câu BẰNG TIẾNG VIỆT. Nhắc có thể giúp tìm sản phẩm khi cần.";
        return callOllamaChat(systemPrompt, userMessage, 80);
    }

    @SuppressWarnings("unchecked")
    private String callOllamaChat(String systemPrompt, String userMessage, int maxTokens) throws Exception {
        String url = chatProperties.getOllamaBaseUrl().replaceAll("/$", "") + "/api/chat";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatProperties.getOllamaModel());
        body.put("stream", false);
        body.put("options", Map.of("num_predict", maxTokens));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response;
        try {
            response = ollamaRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            throw new RuntimeException(ex.getStatusCode() + " " + (ex.getResponseBodyAsString() != null ? ex.getResponseBodyAsString() : ""));
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Ollama API returned " + response.getStatusCode());
        }
        Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> msg = (Map<String, Object>) json.get("message");
        Object content = msg != null ? msg.get("content") : null;
        return content != null ? content.toString().trim() : (maxTokens > 200 ? FALLBACK_REPLY : CONVERSATIONAL_FALLBACK);
    }

    private String buildFallbackReply(String keyword, List<ProductSuggestionDto> suggestions) {
        if (suggestions.isEmpty()) {
            return keyword.isEmpty()
                    ? FALLBACK_REPLY
                    : "Dạ em đã tìm nhưng chưa thấy sản phẩm nào khớp với \"" + keyword + "\". Anh/chị thử từ khóa gần giống hoặc xem toàn bộ danh mục tại trang Sản phẩm ạ.";
        }
        return "Dạ em tìm được " + suggestions.size() + " sản phẩm liên quan đến \"" + keyword + "\". Anh/chị xem bên dưới, bấm vào từng sản phẩm để xem chi tiết và đặt hàng ạ.";
    }
}
