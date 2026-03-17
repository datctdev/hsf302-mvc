package com.hsf.e_comerce.chatbot.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.chatbot.dto.ChatbotOptionDto;
import com.hsf.e_comerce.chatbot.dto.ChatbotProductCardDto;
import com.hsf.e_comerce.chatbot.dto.ChatbotResponseDto;
import com.hsf.e_comerce.chatbot.dto.ChatbotInteractRequest;
import com.hsf.e_comerce.chatbot.entity.ChatbotNode;
import com.hsf.e_comerce.chatbot.parser.SearchIntent;
import com.hsf.e_comerce.chatbot.parser.SearchQueryParser;
import com.hsf.e_comerce.chatbot.entity.ChatbotOption;
import com.hsf.e_comerce.chatbot.repository.ChatbotNodeRepository;
import com.hsf.e_comerce.chatbot.repository.ChatbotOptionRepository;
import com.hsf.e_comerce.chatbot.service.ChatbotService;
import com.hsf.e_comerce.chatbot.valueobject.ChatbotNodeType;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.product.dto.response.CategoryResponse;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.recommendation.service.RecommendationService;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final String SESSION_CURRENT_NODE = "chatbotCurrentNode";
    private static final String SESSION_ROOT_NODE = "chatbotRootNode";
    private static final String SESSION_LIVE_CHAT = "chatbotLiveChat";
    private static final String SESSION_SEARCH_CATEGORY_ID = "chatbotSearchCategoryId";

    private final ChatbotNodeRepository nodeRepository;
    private final ChatbotOptionRepository optionRepository;
    private final ProductService productService;
    private final RecommendationService recommendationService;
    private final SellerRequestService sellerRequestService;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponseDto init(HttpSession session, User principal) {
        String current = (String) session.getAttribute(SESSION_CURRENT_NODE);
        String root = (String) session.getAttribute(SESSION_ROOT_NODE);
        String roleContext = getRoleContext(principal);
        String rootNodeId = getRootNodeId(roleContext);

        if (current == null || current.isBlank()) {
            session.setAttribute(SESSION_ROOT_NODE, rootNodeId);
            session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
            current = rootNodeId;
        }

        ChatbotNode node = nodeRepository.findById(current).orElse(null);
        if (node == null) {
            session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
            node = nodeRepository.findById(rootNodeId).orElseThrow(() -> new IllegalStateException("Root node not found: " + rootNodeId));
        }

        return buildResponse(session, node, principal);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponseDto interact(HttpSession session, ChatbotInteractRequest request, User principal) {
        String currentNodeId = (String) session.getAttribute(SESSION_CURRENT_NODE);
        String rootNodeId = (String) session.getAttribute(SESSION_ROOT_NODE);
        if (rootNodeId == null) rootNodeId = getRootNodeId(getRoleContext(principal));

        if (request.getText() != null && !request.getText().isBlank()) {
            return handleTextInput(session, currentNodeId, rootNodeId, request.getText().trim(), principal);
        }

        String action = request.getAction();
        if (action == null || action.isBlank()) {
            return init(session, principal);
        }

        ChatbotNode node = nodeRepository.findById(currentNodeId).orElse(null);
        if (node == null) {
            session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
            return init(session, principal);
        }

        List<ChatbotOption> options = optionRepository.findByNodeIdOrderBySortOrderAsc(currentNodeId);
        ChatbotOption chosen = options.stream()
                .filter(o -> action.equals(o.getActionPayload()))
                .findFirst()
                .orElse(null);

        if (chosen == null) {
            return buildResponse(session, node, principal);
        }

        if ("HUMAN_HANDOFF".equals(action)) {
            session.setAttribute(SESSION_LIVE_CHAT, true);
            return ChatbotResponseDto.builder()
                    .messageText("Bạn đã chuyển sang chat với nhân viên. Vui lòng nhập tin nhắn bên dưới.")
                    .options(Collections.emptyList())
                    .productCards(Collections.emptyList())
                    .humanHandoffRequired(true)
                    .liveChatSessionId(session.getId())
                    .inputExpected(false)
                    .build();
        }

        if ("BACK_TO_MENU".equals(action)) {
            String nextId = chosen.getNextNodeId();
            if (nextId == null || nextId.isBlank()) nextId = rootNodeId;
            session.setAttribute(SESSION_CURRENT_NODE, nextId);
            ChatbotNode nextNode = nodeRepository.findById(nextId).orElse(null);
            if (nextNode != null) return buildResponse(session, nextNode, principal);
            return init(session, principal);
        }

        if ("POLICY_SHIPPING".equals(action)) {
            session.setAttribute(SESSION_CURRENT_NODE, chosen.getNextNodeId());
            return buildResponse(session, nodeRepository.findById(chosen.getNextNodeId()).orElse(null), principal);
        }

        if ("KYC_STATUS".equals(action)) {
            String kycMessage = resolveKycStatus(principal);
            ChatbotNode kycResult = nodeRepository.findById("NODE_KYC_RESULT").orElse(null);
            if (kycResult != null) {
                session.setAttribute(SESSION_CURRENT_NODE, "NODE_KYC_RESULT");
                return ChatbotResponseDto.builder()
                        .messageText(kycMessage)
                        .options(buildOptionsForNode("NODE_KYC_RESULT", rootNodeId))
                        .productCards(Collections.emptyList())
                        .humanHandoffRequired(false)
                        .inputExpected(false)
                        .build();
            }
        }

        if ("ONBOARDING".equals(action)) {
            session.setAttribute(SESSION_CURRENT_NODE, chosen.getNextNodeId());
            return buildResponse(session, nodeRepository.findById(chosen.getNextNodeId()).orElse(null), principal);
        }

        if ("SELLER_STATS".equals(action)) {
            String statsMessage = resolveSellerStats(principal);
            session.setAttribute(SESSION_CURRENT_NODE, "NODE_SELLER_STATS_RESULT");
            List<ChatbotOptionDto> backOptions = buildOptionsForNode("NODE_SELLER_STATS_RESULT", rootNodeId);
            return ChatbotResponseDto.builder()
                    .messageText(statsMessage)
                    .options(backOptions)
                    .productCards(Collections.emptyList())
                    .humanHandoffRequired(false)
                    .inputExpected(false)
                    .build();
        }

        if ("SEARCH_PRODUCTS".equals(action)) {
            session.setAttribute(SESSION_SEARCH_CATEGORY_ID, null);
            session.setAttribute(SESSION_CURRENT_NODE, chosen.getNextNodeId());
            ChatbotNode nextNode = nodeRepository.findById(chosen.getNextNodeId()).orElse(null);
            return buildResponse(session, nextNode, principal);
        }

        if ("SEARCH_ALL".equals(action)) {
            session.setAttribute(SESSION_SEARCH_CATEGORY_ID, null);
            session.setAttribute(SESSION_CURRENT_NODE, chosen.getNextNodeId());
            ChatbotNode priceNode = nodeRepository.findById(chosen.getNextNodeId()).orElse(null);
            return buildResponse(session, priceNode, principal);
        }

        if ("SEARCH_CATEGORY".equals(action) && request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            session.setAttribute(SESSION_SEARCH_CATEGORY_ID, request.getCategoryId());
            session.setAttribute(SESSION_CURRENT_NODE, "NODE_SEARCH_PRICE");
            return buildResponse(session, nodeRepository.findById("NODE_SEARCH_PRICE").orElse(null), principal);
        }

        if (action.startsWith("SEARCH_PRICE_")) {
            UUID categoryId = null;
            String catIdStr = (String) session.getAttribute(SESSION_SEARCH_CATEGORY_ID);
            if (catIdStr != null && !catIdStr.isBlank()) {
                try { categoryId = UUID.fromString(catIdStr); } catch (Exception ignored) {}
            }
            BigDecimal min = null, max = null;
            switch (action) {
                case "SEARCH_PRICE_UNDER_1M" -> { min = BigDecimal.ZERO; max = new BigDecimal("1000000"); }
                case "SEARCH_PRICE_1M_5M" -> { min = new BigDecimal("1000000"); max = new BigDecimal("5000000"); }
                case "SEARCH_PRICE_OVER_5M" -> { min = new BigDecimal("5000000"); max = null; }
                default -> {}
            }
            Page<ProductResponse> page = productService.getPublishedProducts(0, 8, null, categoryId, null, min, max, null, null);
            List<ChatbotProductCardDto> cards = page.getContent().stream()
                    .map(p -> toProductCard(p))
                    .collect(Collectors.toList());
            String msg = cards.isEmpty() ? "Không tìm thấy sản phẩm nào trong khoảng giá này." : "Dưới đây là một số sản phẩm phù hợp:";
            session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
            List<ChatbotOptionDto> menuOptions = buildOptionsForNode(rootNodeId, rootNodeId);
            return ChatbotResponseDto.builder()
                    .messageText(msg)
                    .options(menuOptions)
                    .productCards(cards)
                    .humanHandoffRequired(false)
                    .inputExpected(false)
                    .build();
        }

        if (chosen.getNextNodeId() != null && !chosen.getNextNodeId().isBlank()) {
            session.setAttribute(SESSION_CURRENT_NODE, chosen.getNextNodeId());
            ChatbotNode nextNode = nodeRepository.findById(chosen.getNextNodeId()).orElse(null);
            return buildResponse(session, nextNode, principal);
        }

        return buildResponse(session, node, principal);
    }

    private ChatbotResponseDto handleTextInput(HttpSession session, String currentNodeId, String rootNodeId, String text, User principal) {
        ChatbotNode node = nodeRepository.findById(currentNodeId).orElse(null);
        if (node == null || node.getNodeType() != ChatbotNodeType.INPUT_EXPECTED) {
            session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
            return init(session, principal);
        }

        if ("NODE_SEARCH_KEYWORD".equals(currentNodeId)) {
            if (text.isBlank()) {
                return ChatbotResponseDto.builder()
                        .messageText("Vui lòng nhập từ khóa tìm kiếm (có thể kèm giá, VD: laptop dưới 30 triệu).")
                        .options(buildOptionsForNode(currentNodeId, rootNodeId))
                        .productCards(Collections.emptyList())
                        .humanHandoffRequired(false)
                        .inputExpected(true)
                        .inputHint("VD: áo thun, laptop dưới 30 triệu, laptop hãng asus")
                        .build();
            }
            SearchIntent intent = SearchQueryParser.parse(text);
            String keyword = intent.getKeyword() != null && !intent.getKeyword().isBlank()
                    ? intent.getKeyword().trim() : null;
            if (keyword == null && intent.getMinPrice() == null && intent.getMaxPrice() == null) {
                return ChatbotResponseDto.builder()
                        .messageText("Vui lòng nhập từ khóa sản phẩm (VD: laptop, áo thun) hoặc kèm giá (VD: laptop dưới 30 triệu).")
                        .options(buildOptionsForNode(currentNodeId, rootNodeId))
                        .productCards(Collections.emptyList())
                        .humanHandoffRequired(false)
                        .inputExpected(true)
                        .inputHint("VD: laptop dưới 30 triệu, laptop hãng asus")
                        .build();
            }
            Page<ProductResponse> page = productService.searchForChatbot(
                    keyword,
                    intent.getMinPrice(),
                    intent.getMaxPrice(),
                    null,
                    0, 8);
            recommendationService.recordSearch(session.getId(), principal != null ? principal.getId() : null, keyword, null, intent.getMinPrice(), intent.getMaxPrice());
            List<ChatbotProductCardDto> cards = page.getContent().stream()
                    .map(this::toProductCard)
                    .collect(Collectors.toList());
            // Giữ tại NODE_SEARCH_KEYWORD để user có thể tìm tiếp hoặc chủ động bấm "Về menu"
            List<ChatbotOptionDto> searchOptions = buildOptionsForNode("NODE_SEARCH_KEYWORD", rootNodeId);
            String confirmation = intent.toConfirmationSummary();
            String msg;
            if (cards.isEmpty()) {
                msg = buildNoResultMessage(intent);
            } else {
                msg = (confirmation != null ? "Đang tìm: " + confirmation + ".\n\n" : "")
                        + "Dưới đây là một số sản phẩm phù hợp:";
            }
            return ChatbotResponseDto.builder()
                    .messageText(msg)
                    .options(searchOptions)
                    .productCards(cards)
                    .humanHandoffRequired(false)
                    .inputExpected(true)
                    .inputHint("VD: laptop dưới 30 triệu, laptop hãng asus")
                    .build();
        }

        session.setAttribute(SESSION_CURRENT_NODE, rootNodeId);
        return init(session, principal);
    }

    private String buildNoResultMessage(SearchIntent intent) {
        StringBuilder sb = new StringBuilder("Không tìm thấy sản phẩm nào");
        if (intent.getKeyword() != null && !intent.getKeyword().isBlank()) {
            sb.append(" cho \"").append(intent.getKeyword()).append("\"");
        }
        if (intent.getMinPrice() != null || intent.getMaxPrice() != null) {
            if (intent.getMinPrice() != null && intent.getMaxPrice() != null) {
                sb.append(" trong khoảng ").append(formatPrice(intent.getMinPrice()))
                        .append(" - ").append(formatPrice(intent.getMaxPrice()));
            } else if (intent.getMaxPrice() != null) {
                sb.append(" dưới ").append(formatPrice(intent.getMaxPrice()));
            } else {
                sb.append(" trên ").append(formatPrice(intent.getMinPrice()));
            }
        }
        sb.append(".");
        sb.append("\n\nThử bỏ bớt từ khóa hoặc mở rộng khoảng giá.");
        return sb.toString();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "";
        DecimalFormat df = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(java.util.Locale.US));
        return df.format(price) + " đ";
    }

    private ChatbotResponseDto buildSearchCategoryResponse(HttpSession session, User principal) {
        List<CategoryResponse> categories = productService.findAllCategory();
        List<ChatbotOptionDto> options = new ArrayList<>();
        for (CategoryResponse c : categories) {
            options.add(ChatbotOptionDto.builder()
                    .buttonLabel(c.getName())
                    .actionPayload("SEARCH_CATEGORY")
                    .nextNodeId("NODE_SEARCH_PRICE")
                    .categoryId(c.getId() != null ? c.getId().toString() : null)
                    .build());
        }
        options.add(ChatbotOptionDto.builder()
                .buttonLabel("Xem tất cả danh mục")
                .actionPayload("SEARCH_ALL")
                .nextNodeId("NODE_SEARCH_PRICE")
                .build());
        options.add(ChatbotOptionDto.builder()
                .buttonLabel("Về menu")
                .actionPayload("BACK_TO_MENU")
                .nextNodeId((String) session.getAttribute(SESSION_ROOT_NODE))
                .build());

        return ChatbotResponseDto.builder()
                .messageText("Chọn danh mục hoặc bỏ qua để xem tất cả.")
                .options(options)
                .productCards(Collections.emptyList())
                .humanHandoffRequired(false)
                .inputExpected(false)
                .build();
    }

    private ChatbotResponseDto buildResponse(HttpSession session, ChatbotNode node, User principal) {
        if (node == null) return init(session, principal);

        boolean inputExpected = node.getNodeType() == ChatbotNodeType.INPUT_EXPECTED;
        List<ChatbotOptionDto> options = new ArrayList<>();

        if ("NODE_SEARCH_CATEGORY".equals(node.getId())) {
            return buildSearchCategoryResponse(session, principal);
        }

        options = buildOptionsForNode(node.getId(), (String) session.getAttribute(SESSION_ROOT_NODE));

        String inputHint = null;
        if (inputExpected && "NODE_SEARCH_KEYWORD".equals(node.getId())) {
            inputHint = "VD: áo thun, laptop";
        }
        return ChatbotResponseDto.builder()
                .messageText(node.getMessageText())
                .options(options)
                .productCards(Collections.emptyList())
                .humanHandoffRequired(false)
                .inputExpected(inputExpected)
                .inputHint(inputHint)
                .build();
    }

    private List<ChatbotOptionDto> buildOptionsForNode(String nodeId, String rootNodeId) {
        List<ChatbotOption> opts = optionRepository.findByNodeIdOrderBySortOrderAsc(nodeId);
        return opts.stream()
                .map(o -> {
                    String next = o.getNextNodeId();
                    if ("BACK_TO_MENU".equals(o.getActionPayload()) && (next == null || next.isBlank())) next = rootNodeId;
                    return ChatbotOptionDto.builder()
                            .buttonLabel(o.getButtonLabel())
                            .actionPayload(o.getActionPayload())
                            .nextNodeId(next != null ? next : rootNodeId)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ChatbotProductCardDto toProductCard(ProductResponse p) {
        String thumb = null;
        if (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().get(0).getImageUrl() != null) {
            String url = p.getImages().get(0).getImageUrl();
            thumb = url.startsWith("http") ? url : baseUrl + (url.startsWith("/") ? url : "/" + url);
        }
        String productUrl = baseUrl + "/products/" + p.getId();
        return ChatbotProductCardDto.builder()
                .id(p.getId())
                .name(p.getName())
                .basePrice(p.getBasePrice())
                .productUrl(productUrl)
                .thumbnailUrl(thumb)
                .build();
    }

    private String resolveKycStatus(User principal) {
        if (principal == null) return "Bạn cần đăng nhập để kiểm tra KYC.";
        try {
            SellerRequestResponse req = sellerRequestService.getRequestByUserId(principal.getId());
            if (req == null) return "Bạn chưa gửi yêu cầu trở thành Seller. Vào Cài đặt / Đăng ký bán hàng để bắt đầu.";
            String status = req.getStatus();
            if ("APPROVED".equals(status)) return "KYC / Đơn đăng ký Seller của bạn đã được duyệt. Bạn có thể quản lý shop tại mục Seller.";
            if ("PENDING".equals(status)) return "Đơn đăng ký Seller của bạn đang chờ duyệt. Vui lòng đợi Admin xử lý.";
            if ("REJECTED".equals(status)) return "Đơn đăng ký Seller đã bị từ chối. Lý do: " + (req.getRejectionReason() != null ? req.getRejectionReason() : "Không nêu rõ.") + " Bạn có thể nộp lại sau.";
            return "Trạng thái: " + status;
        } catch (Exception e) {
            return "Bạn chưa gửi yêu cầu trở thành Seller.";
        }
    }

    private String resolveSellerStats(User principal) {
        if (principal == null) return "Bạn cần đăng nhập.";
        Optional<Shop> shopOpt = shopRepository.findByUserId(principal.getId());
        if (shopOpt.isEmpty()) return "Bạn chưa có shop.";
        UUID shopId = shopOpt.get().getId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = orderRepository.countByShopIdAndCreatedAtAfter(shopId, startOfDay);
        return "Đơn hàng mới trong ngày hôm nay: " + count + ".";
    }

    private String getRoleContext(User principal) {
        if (principal == null) return "GUEST";
        if (principal.getRole() == null) return "GUEST";
        String role = principal.getRole().getName();
        if ("ROLE_ADMIN".equals(role)) return "ADMIN";
        if ("ROLE_SELLER".equals(role)) return "SELLER";
        if ("ROLE_BUYER".equals(role)) return "BUYER";
        return "GUEST";
    }

    private String getRootNodeId(String roleContext) {
        return switch (roleContext) {
            case "BUYER" -> "NODE_GREETING_BUYER";
            case "SELLER" -> "NODE_GREETING_SELLER";
            case "ADMIN" -> "NODE_GREETING_BUYER";
            default -> "NODE_GREETING_GUEST";
        };
    }
}
