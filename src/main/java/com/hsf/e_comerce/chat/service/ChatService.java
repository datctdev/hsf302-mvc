package com.hsf.e_comerce.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.e_comerce.chat.config.ChatProperties;
import com.hsf.e_comerce.chat.dto.ChatMessageDto;
import com.hsf.e_comerce.chat.dto.ChatResponse;
import com.hsf.e_comerce.chat.dto.IntentResult;
import com.hsf.e_comerce.chat.dto.ProductSuggestionDto;
import com.hsf.e_comerce.chat.store.ChatSessionStore;
import com.hsf.e_comerce.product.dto.response.CategoryResponse;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_SUGGESTIONS = 8;
    private static final String FALLBACK_REPLY = "Chào anh/chị! Em có thể giúp tìm sản phẩm phù hợp. Anh/chị cứ nói nhu cầu (ví dụ: \"tôi cần tìm điện thoại\", \"áo nam\", \"giày thể thao\") hoặc xem danh mục tại trang Sản phẩm ạ.";
    private static final String CONVERSATIONAL_FALLBACK = "Dạ chào anh/chị! Em là trợ lý mua sắm. Anh/chị cần tìm sản phẩm gì hoặc có câu hỏi gì cứ nhắn em ạ.";
    /** Fallback khi khách tâm sự / xin an ủi nhưng Ollama không trả lời (tắt hoặc lỗi). */
    private static final String EMOTIONAL_FALLBACK = "Dạ em rất tiếc khi nghe điều đó. Em là trợ lý mua sắm nên không thể an ủi sâu, nhưng khi anh/chị cần tìm sản phẩm gì cứ nhắn em ạ.";

    /** Tin nhắn có từ khóa tâm sự / xin an ủi → dùng fallback đồng cảm khi không có reply từ model. */
    private static final Set<String> EMOTIONAL_KEYWORDS = Set.of(
            "an ủi", "an ui", "anủi", "mâu thuẫn", "mau thuan", "đánh nhau", "danh nhau", "buồn", "buon",
            "tâm sự", "tam su", "khó chịu", "kho chiu", "stress", "an ủi tôi", "comfort"
    );

    private boolean looksEmotional(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.trim().toLowerCase().replaceAll("\\s+", " ");
        return EMOTIONAL_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /** Trả fallback phù hợp: nếu tin nhắn mang tính tâm sự/an ủi thì dùng EMOTIONAL_FALLBACK, còn không thì CONVERSATIONAL_FALLBACK. */
    private String getConversationalFallback(String userMessage) {
        return looksEmotional(userMessage) ? EMOTIONAL_FALLBACK : CONVERSATIONAL_FALLBACK;
    }

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
            "xem giúp ", "cho xem ", "gợi ý ", "gợi ý giúp ", "tư vấn ", "cần tìm ",
            "mình cần ", "mình muốn ", "cho mình xem ", "bán ", "còn "
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "tôi", "tớ", "em", "anh", "chị", "mình", "tao", "bạn", "các", "này", "đó", "kia",
            "cần", "tìm", "mua", "xem", "có", "không", "ạ", "nhé", "ơi", "một", "cái", "chiếc",
            "cho", "giúp", "với", "và", "hay", "hoặc", "đi", "nha", "nè"
    );
    private static final Pattern PATTERN_CO_X_KHONG = Pattern.compile("(?i)có\\s+(.+?)\\s+không\\s*$");

    private final ProductService productService;
    private final ChatProperties chatProperties;
    private final ChatSessionStore sessionStore;
    @Qualifier("ollamaRestTemplate")
    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // --- Intent: hiểu ngữ cảnh (Ollama) hoặc rule-based fallback ---

    /** Phân loại intent từ tin nhắn và lịch sử hội thoại. Ưu tiên Ollama để hiểu ngữ cảnh, fallback rule-based. */
    private IntentResult classifyIntent(String message, List<ChatMessageDto> history) {
        if (useOllama()) {
            try {
                IntentResult fromOllama = callOllamaClassify(message, history);
                if (fromOllama != null) return fromOllama;
            } catch (Exception e) {
                logOllamaFailure(e, "classify");
            }
        }
        return ruleBasedIntent(message);
    }

    private IntentResult ruleBasedIntent(String message) {
        if (message == null || message.isBlank()) return IntentResult.builder().intent("chao").keyword("").build();
        if (isConversationalOnly(message)) return IntentResult.builder().intent("chao").keyword("").build();
        String keyword = extractSearchKeyword(message);
        return IntentResult.builder().intent("tim_san_pham").keyword(keyword != null ? keyword : "").build();
    }

    private static final String SYSTEM_CLASSIFY_CONTEXT = "Bạn là bộ phân tích Ý ĐỊNH và NGỮ CẢNH của khách trong cửa hàng. "
            + "Nhiệm vụ: đọc tin nhắn HIỆN TẠI và (nếu có) LỊCH SỬ hội thoại phía trên, HIỂU Ý khách muốn gì, không chỉ match từ khóa. "
            + "Trả lời ĐÚNG 1 dòng JSON, không giải thích: {\"intent\": \"chao\" | \"tim_san_pham\" | \"khac\", \"keyword\": \"...\"}. "
            + "Quy tắc: "
            + "intent=chao: chỉ chào hỏi, cảm ơn, tạm biệt, xã giao thuần (không có nhu cầu mua/tìm). "
            + "intent=tim_san_pham: khách có ý tìm/mua/xem sản phẩm (dù nói trực tiếp hay ngầm hiểu từ ngữ cảnh). keyword = cụm từ dùng để tìm trên kho (1-5 từ). "
            + "Ví dụ HIỂU NGỮ CẢNH: \"vừa đi làm về mệt cần mua gì uống\" -> intent=tim_san_pham, keyword=\"nước giải khát đồ uống\"; "
            + "\"con sắp đi học cần mua cặp\" -> intent=tim_san_pham, keyword=\"cặp sách balo\"; "
            + "\"cho xem thêm\" hoặc \"còn loại nào khác\" (sau khi đã nói về sạc dự phòng) -> intent=tim_san_pham, keyword=\"sạc dự phòng\"; "
            + "\"rẻ hơn được không\" (sau khi đã hỏi áo) -> intent=tim_san_pham, keyword=\"áo\". "
            + "intent=khac: câu hỏi/tâm sự không liên quan mua sắm (xin an ủi, chuyện đời sống...). keyword để trống. "
            + "keyword luôn là chuỗi, viết thường không dấu hoặc có dấu đều được, có thể rỗng.";

    @SuppressWarnings("unchecked")
    private IntentResult callOllamaClassify(String userMessage, List<ChatMessageDto> history) throws Exception {
        String url = chatProperties.getOllamaBaseUrl().replaceAll("/$", "") + "/api/chat";
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_CLASSIFY_CONTEXT));
        if (history != null && !history.isEmpty()) {
            int from = Math.max(0, history.size() - 6);
            for (int i = from; i < history.size(); i++) {
                ChatMessageDto m = history.get(i);
                if (m.getRole() != null && m.getContent() != null)
                    messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatProperties.getOllamaModel());
        body.put("stream", false);
        body.put("options", Map.of("num_predict", 220));
        body.put("messages", messages);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = ollamaRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
        Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> msg = (Map<String, Object>) json.get("message");
        Object content = msg != null ? msg.get("content") : null;
        if (content == null) return null;
        String raw = content.toString().trim();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}') + 1;
        if (start < 0 || end <= start) return null;
        Map<String, Object> parsed = objectMapper.readValue(raw.substring(start, end), Map.class);
        String intent = Optional.ofNullable(parsed.get("intent")).map(Object::toString).map(String::toLowerCase).orElse("khac");
        if (!List.of("chao", "tim_san_pham", "khac").contains(intent)) intent = "khac";
        String keyword = Optional.ofNullable(parsed.get("keyword")).map(Object::toString).map(String::trim).orElse("");
        return IntentResult.builder().intent(intent).keyword(keyword).build();
    }

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

    /** Lấy sản phẩm theo danh mục có tên trùng/chứa từ khóa (hoặc từ đầu tiên). Gộp từ nhiều category, giới hạn tổng số. */
    private List<ProductResponse> findProductsByCategoryMatch(List<CategoryResponse> categories, String keyword, int maxTotal) {
        if (categories == null || keyword == null || keyword.isBlank() || maxTotal <= 0) return Collections.emptyList();
        String term = keyword.contains(" ") ? keyword.trim().split("\\s+")[0] : keyword.trim();
        if (term.isEmpty()) return Collections.emptyList();
        String termLower = term.toLowerCase();
        List<UUID> matchingCategoryIds = categories.stream()
                .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(termLower))
                .map(CategoryResponse::getId)
                .distinct()
                .limit(5)
                .toList();
        if (matchingCategoryIds.isEmpty()) return Collections.emptyList();
        Set<UUID> seenIds = new java.util.LinkedHashSet<>();
        List<ProductResponse> combined = new ArrayList<>();
        int perCat = Math.max(2, (maxTotal + matchingCategoryIds.size() - 1) / matchingCategoryIds.size());
        for (UUID categoryId : matchingCategoryIds) {
            if (combined.size() >= maxTotal) break;
            List<ProductResponse> page = productService.getPublishedProducts(0, perCat, null, categoryId, null, null, null, null, null).getContent();
            for (ProductResponse p : page) {
                if (p != null && p.getId() != null && seenIds.add(p.getId())) {
                    combined.add(p);
                    if (combined.size() >= maxTotal) break;
                }
            }
        }
        return combined;
    }

    /** Gộp kết quả FTS + category, ưu tiên FTS, loại trùng theo id, giới hạn MAX_SUGGESTIONS. */
    private List<ProductResponse> mergeSearchAndCategoryResults(List<ProductResponse> fromSearch, List<ProductResponse> fromCategory) {
        Set<UUID> seen = new java.util.LinkedHashSet<>();
        List<ProductResponse> out = new ArrayList<>();
        for (ProductResponse p : fromSearch) {
            if (p != null && p.getId() != null && seen.add(p.getId())) out.add(p);
            if (out.size() >= MAX_SUGGESTIONS) return out;
        }
        for (ProductResponse p : fromCategory) {
            if (p != null && p.getId() != null && seen.add(p.getId())) out.add(p);
            if (out.size() >= MAX_SUGGESTIONS) return out;
        }
        return out;
    }

    // --- Chat chính: session + history ---

    public ChatResponse chat(String sessionId, String userMessage) {
        String raw = userMessage != null ? userMessage.trim() : "";
        if (raw.isEmpty()) {
            return ChatResponse.builder().reply(CONVERSATIONAL_FALLBACK).productSuggestions(Collections.emptyList()).build();
        }

        List<ChatMessageDto> history = sessionStore.getRecent(sessionId, MAX_HISTORY_MESSAGES);

        IntentResult intentResult = classifyIntent(raw, history);
        boolean isConversational = "chao".equals(intentResult.getIntent()) || "khac".equals(intentResult.getIntent());

        if (isConversational) {
            String reply = replyConversational(raw, history);
            sessionStore.append(sessionId, "user", raw);
            sessionStore.append(sessionId, "assistant", reply);
            return ChatResponse.builder().reply(reply).productSuggestions(Collections.emptyList()).build();
        }

        String keyword = intentResult.getKeyword() != null ? intentResult.getKeyword().trim() : "";
        if (keyword.length() > 500) keyword = keyword.substring(0, 500);
        if (keyword.isEmpty()) keyword = extractSearchKeyword(raw);
        if (keyword.length() > 500) keyword = keyword.substring(0, 500);

        List<CategoryResponse> categories = productService.findAllCategory();
        List<ProductResponse> fromFts = keyword.isEmpty() ? Collections.emptyList() : productService.searchProducts(keyword, 0, MAX_SUGGESTIONS).getContent();
        if (fromFts.isEmpty() && keyword != null && !keyword.isBlank() && keyword.contains(" ")) {
            String firstWord = keyword.trim().split("\\s+")[0];
            if (!firstWord.isEmpty()) fromFts = productService.searchProducts(firstWord, 0, MAX_SUGGESTIONS).getContent();
        }
        List<ProductResponse> fromCategory = findProductsByCategoryMatch(categories, keyword, MAX_SUGGESTIONS);
        List<ProductResponse> searchResults = mergeSearchAndCategoryResults(fromFts, fromCategory);

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

        String reply = replyForProducts(raw, categories, searchResults, keyword, suggestions, history);
        sessionStore.append(sessionId, "user", raw);
        sessionStore.append(sessionId, "assistant", reply);
        return ChatResponse.builder().reply(reply).productSuggestions(suggestions).build();
    }

    private boolean useOllama() {
        String url = chatProperties.getOllamaBaseUrl();
        return url != null && !url.isBlank();
    }

    private String replyConversational(String userMessage, List<ChatMessageDto> history) {
        if (useOllama()) {
            try {
                String reply = callOllamaConversational(userMessage, history);
                if (CONVERSATIONAL_FALLBACK.equals(reply) && looksEmotional(userMessage))
                    return EMOTIONAL_FALLBACK;
                return reply;
            } catch (Exception e) {
                logOllamaFailure(e, "conversational");
            }
        }
        return getConversationalFallback(userMessage);
    }

    private String replyForProducts(String userMessage, List<CategoryResponse> categories, List<ProductResponse> searchResults, String keyword, List<ProductSuggestionDto> suggestions, List<ChatMessageDto> history) {
        if (useOllama()) {
            try {
                return callOllamaForProducts(userMessage, categories, searchResults, history);
            } catch (Exception e) {
                logOllamaFailure(e, "products");
            }
        }
        return buildFallbackReply(keyword, suggestions);
    }

    private void logOllamaFailure(Exception e, String context) {
        String msg = e.getMessage();
        log.warn("Ollama {} call failed: {}", context, msg);
        if (msg != null && msg.contains("404") && msg.contains("not found")) {
            log.info("Ollama model chưa được pull. Chạy: ollama pull {}", chatProperties.getOllamaModel());
        }
    }

    /** Build messages cho Ollama: [system] + history + user. */
    private List<Map<String, String>> buildOllamaMessages(String systemPrompt, List<ChatMessageDto> history, String currentUserMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessageDto m : history) {
            if (m.getRole() != null && m.getContent() != null)
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        messages.add(Map.of("role", "user", "content", currentUserMessage));
        return messages;
    }

    private static final String SYSTEM_CONVERSATIONAL = "Bạn là trợ lý mua sắm thân thiện. Xưng em, gọi khách anh/chị. Trả lời BẰNG TIẾNG VIỆT, ngắn gọn 1-2 câu, TỰ NHIÊN theo đúng ngữ cảnh và ý của khách (đừng trả lời chung chung). "
            + "Đọc kỹ tin nhắn và lịch sử hội thoại: nếu khách chào/cảm ơn thì đáp lễ; nếu chia sẻ chuyện cá nhân hoặc xin an ủi thì đồng cảm ngắn rồi nhắc em có thể giúp mua sắm khi cần; nếu khách hỏi gì cụ thể thì trả lời đúng trọng tâm. "
            + "Luôn có câu trả lời, không để trống. Không bịa thông tin.";

    private String callOllamaConversational(String userMessage, List<ChatMessageDto> history) throws Exception {
        List<Map<String, String>> messages = buildOllamaMessages(SYSTEM_CONVERSATIONAL, history, userMessage);
        return callOllamaChat(messages, 80);
    }

    private static final String SYSTEM_PRODUCTS_TEMPLATE = "Bạn là trợ lý mua sắm. Xưng em, gọi khách anh/chị. Trả lời BẰNG TIẾNG VIỆT, 1-2 câu ngắn, TỰ NHIÊN theo ngữ cảnh (vd: khách vừa hỏi sạc thì nói gợi ý về sạc, đừng nói chung chung). "
            + "QUY TẮC: KHÔNG liệt kê tên sản phẩm hay giá trong tin. KHÔNG hỏi thêm. Nói ngắn để khách xem thẻ bên dưới. "
            + "Danh mục có: %s. Sản phẩm tìm được (chỉ tham khảo, không nhắc trong tin):%n%s";

    private String callOllamaForProducts(String userMessage, List<CategoryResponse> categories, List<ProductResponse> products, List<ChatMessageDto> history) throws Exception {
        String categoryList = categories.stream().map(CategoryResponse::getName).collect(Collectors.joining(", "));
        String productContext = products.stream().limit(10)
                .map(p -> "- " + p.getName() + " (" + (p.getCategoryName() != null ? p.getCategoryName() : "N/A") + "): " + p.getBasePrice() + " VND.")
                .collect(Collectors.joining("\n"));
        String systemPrompt = String.format(SYSTEM_PRODUCTS_TEMPLATE, categoryList, productContext);
        List<Map<String, String>> messages = buildOllamaMessages(systemPrompt, history, userMessage);
        return callOllamaChat(messages, 120);
    }

    @SuppressWarnings("unchecked")
    private String callOllamaChat(List<Map<String, String>> messages, int maxTokens) throws Exception {
        String url = chatProperties.getOllamaBaseUrl().replaceAll("/$", "") + "/api/chat";
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatProperties.getOllamaModel());
        body.put("stream", false);
        body.put("options", Map.of("num_predict", maxTokens));
        body.put("messages", messages);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = ollamaRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
            throw new RuntimeException("Ollama API returned " + response.getStatusCode());
        Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> msg = (Map<String, Object>) json.get("message");
        Object content = msg != null ? msg.get("content") : null;
        String text = content != null ? content.toString().trim() : "";
        if (text.isEmpty()) return maxTokens > 200 ? FALLBACK_REPLY : CONVERSATIONAL_FALLBACK;
        return text;
    }

    private String buildFallbackReply(String keyword, List<ProductSuggestionDto> suggestions) {
        if (suggestions.isEmpty()) {
            return keyword.isEmpty() ? FALLBACK_REPLY
                    : "Dạ em đã tìm nhưng chưa thấy sản phẩm nào khớp với \"" + keyword + "\". Anh/chị thử từ khóa gần giống hoặc xem toàn bộ danh mục tại trang Sản phẩm ạ.";
        }
        return "Dạ em tìm được " + suggestions.size() + " sản phẩm liên quan đến \"" + keyword + "\". Anh/chị xem bên dưới, bấm vào từng sản phẩm để xem chi tiết và đặt hàng ạ.";
    }

    // --- Streaming ---

    public void streamChat(String sessionId, String userMessage, SseEmitter emitter) {
        String raw = userMessage != null ? userMessage.trim() : "";
        try {
            if (raw.isEmpty()) {
                sendDone(emitter, CONVERSATIONAL_FALLBACK, Collections.emptyList());
                return;
            }
            List<ChatMessageDto> history = sessionStore.getRecent(sessionId, MAX_HISTORY_MESSAGES);
            IntentResult intentResult = classifyIntent(raw, history);
            boolean isConversational = "chao".equals(intentResult.getIntent()) || "khac".equals(intentResult.getIntent());

            if (isConversational) {
                String reply = streamOllamaReply(SYSTEM_CONVERSATIONAL, history, raw, 80, emitter);
                if (reply == null || reply.isBlank()) reply = getConversationalFallback(raw);
                sessionStore.append(sessionId, "user", raw);
                sessionStore.append(sessionId, "assistant", reply);
                sendDone(emitter, reply, Collections.emptyList());
                return;
            }

            String keyword = intentResult.getKeyword() != null ? intentResult.getKeyword().trim() : "";
            if (keyword.length() > 500) keyword = keyword.substring(0, 500);
            if (keyword.isEmpty()) keyword = extractSearchKeyword(raw);
            if (keyword.length() > 500) keyword = keyword.substring(0, 500);

            List<CategoryResponse> categories = productService.findAllCategory();
            List<ProductResponse> fromFts = keyword.isEmpty() ? Collections.emptyList() : productService.searchProducts(keyword, 0, MAX_SUGGESTIONS).getContent();
            if (fromFts.isEmpty() && keyword != null && !keyword.isBlank() && keyword.contains(" ")) {
                String firstWord = keyword.trim().split("\\s+")[0];
                if (!firstWord.isEmpty()) fromFts = productService.searchProducts(firstWord, 0, MAX_SUGGESTIONS).getContent();
            }
            List<ProductResponse> fromCategory = findProductsByCategoryMatch(categories, keyword, MAX_SUGGESTIONS);
            List<ProductResponse> searchResults = mergeSearchAndCategoryResults(fromFts, fromCategory);

            List<ProductSuggestionDto> suggestions = searchResults.stream()
                    .map(p -> {
                        String thumb = null;
                        if (p.getImages() != null && !p.getImages().isEmpty())
                            thumb = toAbsoluteUrl(p.getImages().get(0).getImageUrl());
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

            String systemPrompt = String.format(SYSTEM_PRODUCTS_TEMPLATE,
                    categories.stream().map(CategoryResponse::getName).collect(Collectors.joining(", ")),
                    searchResults.stream().limit(10)
                            .map(p -> "- " + p.getName() + " (" + (p.getCategoryName() != null ? p.getCategoryName() : "N/A") + "): " + p.getBasePrice() + " VND.")
                            .collect(Collectors.joining("\n")));
            String reply = streamOllamaReply(systemPrompt, history, raw, 120, emitter);
            if (reply == null || reply.isBlank()) reply = buildFallbackReply(keyword, suggestions);
            sessionStore.append(sessionId, "user", raw);
            sessionStore.append(sessionId, "assistant", reply);
            sendDone(emitter, reply, suggestions);
        } catch (Exception e) {
            log.warn("streamChat failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("Không thể kết nối trợ lý. Bạn thử lại sau."));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    private void sendDone(SseEmitter emitter, String reply, List<ProductSuggestionDto> suggestions) {
        try {
            Map<String, Object> done = new HashMap<>();
            done.put("reply", reply);
            done.put("productSuggestions", suggestions);
            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    /** Gọi Ollama stream, gửi từng chunk qua emitter, trả về full reply. */
    private String streamOllamaReply(String systemPrompt, List<ChatMessageDto> history, String userMessage, int maxTokens, SseEmitter emitter) {
        if (!useOllama()) return null;
        List<Map<String, String>> messages = buildOllamaMessages(systemPrompt, history, userMessage);
        String url = chatProperties.getOllamaBaseUrl().replaceAll("/$", "") + "/api/chat";
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatProperties.getOllamaModel());
        body.put("stream", true);
        body.put("options", Map.of("num_predict", maxTokens));
        body.put("messages", messages);
        try {
            byte[] bodyBytes = objectMapper.writeValueAsBytes(body);
            String fullReply = ollamaRestTemplate.execute(URI.create(url), HttpMethod.POST, req -> {
                req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                req.getBody().write(bodyBytes);
            }, response -> {
                StringBuilder sb = new StringBuilder();
                try (var reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && !line.isBlank()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> json = objectMapper.readValue(line, Map.class);
                        Object chunk = json.get("response");
                        if (chunk != null && !chunk.toString().isEmpty()) {
                            sb.append(chunk.toString());
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(chunk.toString()));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                return sb.toString();
            });
            return fullReply != null ? fullReply.trim() : null;
        } catch (Exception e) {
            logOllamaFailure(e, "stream");
            return null;
        }
    }
}
