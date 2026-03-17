package com.hsf.e_comerce.order.service.impl;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartItemRepository;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.request.CreateOrderRequest;
import com.hsf.e_comerce.order.dto.request.UpdateOrderRequest;
import com.hsf.e_comerce.order.dto.request.UpdateOrderStatusRequest;
import com.hsf.e_comerce.order.dto.response.OrderItemResponse;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.dto.response.OrderTrackingResponse;
import com.hsf.e_comerce.order.dto.response.ProductSalesItem;
import com.hsf.e_comerce.order.dto.response.RevenueSummaryResponse;
import com.hsf.e_comerce.order.dto.response.ShopRankingItem;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.order.repository.OrderItemRepository;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.order.valueobject.OrderStatusTransition;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.platform.entity.Commission;
import com.hsf.e_comerce.platform.repository.CommissionRepository;
import com.hsf.e_comerce.platform.service.CategoryCommissionService;
import com.hsf.e_comerce.platform.service.CommissionService;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.review.repository.ReviewReportRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shipping.service.GHNService;
import com.hsf.e_comerce.shipping.service.ShippingService;
import com.hsf.e_comerce.shipping.dto.request.GHNCreateOrderRequest;
import com.hsf.e_comerce.shipping.dto.response.GHNCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopRepository shopRepository;
    private final ProductImageRepository productImageRepository;
    private final GHNService ghnService;
    private final ShippingService shippingService;
    private final ReviewRepository reviewRepository;
    private final PlatformSettingService platformSettingService;
    private final ReviewReportRepository reviewReportRepository;
    private final CommissionService commissionService;
    private final CategoryCommissionService categoryCommissionService;
    private final CommissionRepository commissionRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(User user, CreateOrderRequest request) {
        if (request.getShopId() == null) {
            throw new CustomException("Shop ID là bắt buộc khi đặt một shop. Dùng Đặt tất cả để tạo đơn theo từng shop.");
        }
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new CustomException("Shop không tồn tại."));
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new CustomException("Giỏ hàng trống."));
        List<CartItem> shopCartItems = cart.getItems().stream()
                .filter(item -> item.getProduct().getShop().getId().equals(shop.getId()))
                .collect(Collectors.toList());
        if (shopCartItems.isEmpty()) {
            throw new CustomException("Không có sản phẩm nào từ shop này trong giỏ hàng.");
        }
        return createOrderForShop(user, shop, shopCartItems, request,
                request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public List<OrderResponse> createOrdersFromCart(User user, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new CustomException("Giỏ hàng trống."));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CustomException("Giỏ hàng trống.");
        }
        List<CartItem> itemsToOrder = new ArrayList<>(cart.getItems());
        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            itemsToOrder = cart.getItems().stream()
                    .filter(item -> request.getCartItemIds().contains(item.getId()))
                    .toList();
            if (itemsToOrder.size() != request.getCartItemIds().size()) {
                throw new CustomException("Một số món đã chọn không còn trong giỏ. Vui lòng thử lại.");
            }
        }
        Map<Shop, List<CartItem>> itemsByShop = itemsToOrder.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getShop(), LinkedHashMap::new, Collectors.toList()));
        Integer toDistrictId = request.getShippingDistrictId();
        String toWardCode = request.getShippingWardCode();
        List<OrderResponse> orders = new ArrayList<>();
        for (Map.Entry<Shop, List<CartItem>> entry : itemsByShop.entrySet()) {
            BigDecimal shippingFee = BigDecimal.ZERO;
            if (toDistrictId != null && toWardCode != null && !toWardCode.isBlank()) {
                shippingFee = shippingService.calculateShippingFeeForShop(
                        entry.getKey(), entry.getValue(), toDistrictId, toWardCode);
            }
            OrderResponse orderResponse = createOrderForShop(user, entry.getKey(), entry.getValue(), request, shippingFee);
            orders.add(orderResponse);
        }
        return orders;
    }

    /** Tạo một đơn cho một shop, xóa các cart item của shop đó khỏi giỏ. */
    private OrderResponse createOrderForShop(User user, Shop shop, List<CartItem> shopCartItems,
                                             CreateOrderRequest request, BigDecimal shippingFee) {
        if (shop.getUser() != null && shop.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không thể đặt mua hàng của chính shop mình (shop: " + shop.getName() + "). Vui lòng xóa sản phẩm này khỏi giỏ hàng.");
        }
        Cart cart = shopCartItems.isEmpty() ? null : shopCartItems.get(0).getCart();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        for (CartItem cartItem : shopCartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            if (variant == null) {
                throw new CustomException("Sản phẩm " + product.getName() + " chưa có biến thể.");
            }
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new CustomException("Sản phẩm " + product.getName() + " không đủ số lượng. Còn lại: " + variant.getStockQuantity());
            }
            BigDecimal unitPrice = product.getBasePrice().add(
                    variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
            BigDecimal rate = product.getCategory() != null
                    ? categoryCommissionService.getCommissionByCategory(product.getCategory().getId())
                    : platformSettingService.getCommissionRate();
            BigDecimal itemCommission = itemTotal.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalCommission = totalCommission.add(itemCommission);
        }
        String orderNumber = generateOrderNumber();
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setShop(shop);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShippingName(request.getShippingName());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDistrict(request.getShippingDistrict());
        order.setShippingWard(request.getShippingWard());
        order.setShippingDistrictId(request.getShippingDistrictId());
        order.setShippingWardCode(request.getShippingWardCode());
        order.setNotes(request.getNotes());
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        order.setPlatformCommission(totalCommission);
        order.setCommissionRate(null);
        order.calculateTotal();
        order = orderRepository.save(order);
        for (CartItem cartItem : shopCartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductName(product.getName());
            orderItem.setVariantName(variant.getName());
            orderItem.setVariantValue(variant.getValue());
            orderItem.setQuantity(cartItem.getQuantity());
            BigDecimal unitPrice = product.getBasePrice().add(
                    variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO);
            orderItem.setUnitPrice(unitPrice);
            orderItem.calculateTotalPrice();
            order.getItems().add(orderItem);
            orderItemRepository.save(orderItem);
        }
        if (cart != null) {
            for (CartItem cartItem : shopCartItems) {
                cart.getItems().remove(cartItem);
                cartItemRepository.delete(cartItem);
            }
            cartRepository.save(cart);
        }
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTrackingByOrderNumber(String orderNumber, User optionalUser) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return OrderTrackingResponse.builder()
                    .found(false)
                    .message("Vui lòng nhập mã đơn hàng (ví dụ: ORD-20250101-0001).")
                    .build();
        }
        Optional<Order> opt = orderRepository.findByOrderNumber(orderNumber.trim());
        if (opt.isEmpty()) {
            return OrderTrackingResponse.builder()
                    .found(false)
                    .message("Không tìm thấy đơn hàng với mã: " + orderNumber.trim() + ".")
                    .build();
        }
        Order order = opt.get();
        String statusDisplay = statusToDisplay(order.getStatus());
        OrderTrackingResponse.OrderTrackingResponseBuilder b = OrderTrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .statusDisplay(statusDisplay)
                .deliveredAt(order.getDeliveredAt())
                .found(true)
                .message("Đơn hàng " + order.getOrderNumber() + ": " + statusDisplay + ".");
        return b.build();
    }

    private static String statusToDisplay(OrderStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case CONFIRMED -> "Đã xác nhận";
            case PROCESSING -> "Đang xử lý";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng";
            case CANCELLED -> "Đã hủy";
            case REFUNDED -> "Đã hoàn tiền";
            default -> status.name();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAndUser(UUID orderId, User user) {
        Order order = orderRepository.findByIdAndUserWithItems(orderId, user)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền xem đơn hàng này.");
        }
        
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAndShop(UUID orderId, UUID shopId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));
        
        if (!order.getShop().getId().equals(shopId)) {
            throw new CustomException("Đơn hàng không thuộc về shop này.");
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new CustomException("Bạn không có quyền xem đơn hàng này.");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(User user) {
        List<Order> orders = orderRepository.findByUserIdWithItems(user.getId());
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByIdsAndUser(List<UUID> orderIds, User user) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        List<Order> orders = orderRepository.findByIdInAndUserId(orderIds, user.getId());
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByShop(UUID shopId) {
//        List<Order> orders = orderRepository.findByShopIdWithItems(shopId);
        List<Order> orders = orderRepository
                .findByShopIdAndStatusNotOrderByCreatedAtDesc(shopId, OrderStatus.PENDING_PAYMENT);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByShopAndStatus(UUID shopId, OrderStatus status) {

        if (status == OrderStatus.PENDING_PAYMENT) {
            return List.of();
        }

        List<Order> orders = orderRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shopId, status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllOrders();
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));

        // Check permissions: Admin can update any order, Seller can only update their shop's orders
        boolean isAdmin = user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
        boolean isSeller = user.getRole() != null && "ROLE_SELLER".equals(user.getRole().getName());
        
        if (!isAdmin && !(isSeller && order.getShop().getUser().getId().equals(user.getId()))) {
            throw new CustomException("Bạn không có quyền cập nhật đơn hàng này.");
        }

        // Ràng buộc chuyển trạng thái theo ma trận (không cho nhảy cóc, ví dụ CONFIRMED → DELIVERED)
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();
        if (!OrderStatusTransition.isAllowed(currentStatus, newStatus)) {
            throw new CustomException("Không thể chuyển từ " + currentStatus + " sang " + newStatus + ". Chỉ chấp nhận các bước: " +
                    OrderStatusTransition.getAllowedNextStatuses(currentStatus));
        }

        // If cancelling, restore stock and cancel GHN order
        if (newStatus == OrderStatus.CANCELLED &&  currentStatus == OrderStatus.CONFIRMED && order.isStockDeducted()) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null) {
                    item.getVariant().setStockQuantity(
                            item.getVariant().getStockQuantity() + item.getQuantity()
                    );
                }
            }
            
            // Cancel GHN order if exists
            if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().isEmpty()) {
                try {
                    ghnService.cancelOrder(order.getGhnOrderCode());
                    log.info("Đã hủy đơn GHN: {}", order.getGhnOrderCode());
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn GHN: {}", e.getMessage());
                    // Không throw exception để không block việc hủy đơn trong hệ thống
                }
            }
        }

        // Tạo đơn GHN khi seller xác nhận đơn (PENDING_PAYMENT -> CONFIRMED) – flow “seller confirm trước, buyer thanh toán sau”
        if (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.CONFIRMED) {
            tryCreateGHNOrder(order);
        }
        // Tạo đơn GHN khi seller bắt đầu xử lý/giao hàng (CONFIRMED -> PROCESSING/SHIPPED) – flow đã thanh toán (COD/VNPay)
        if ((newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.SHIPPING)
                && currentStatus == OrderStatus.CONFIRMED) {
            tryCreateGHNOrder(order);
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        order = orderRepository.save(order);

        // 🔥 TẠO COMMISSION KHI GIAO HÀNG THÀNH CÔNG
        if (newStatus == OrderStatus.DELIVERED) {

            commissionService.createCommission(order);
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));

        // Only buyer can cancel their own order
        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền hủy đơn hàng này.");
        }

        // Only allow cancellation if order is PENDING or CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new CustomException("Không thể hủy đơn hàng ở trạng thái " + order.getStatus());
        }

        // Restore stock
        if (order.getStatus() == OrderStatus.CONFIRMED && order.isStockDeducted()) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null) {
                    item.getVariant().setStockQuantity(
                            item.getVariant().getStockQuantity() + item.getQuantity()
                    );
                }
            }
        }

        // Cancel GHN order if exists
        if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().isEmpty()) {
            try {
                ghnService.cancelOrder(order.getGhnOrderCode());
                log.info("Đã hủy đơn GHN: {}", order.getGhnOrderCode());
            } catch (Exception e) {
                log.error("Lỗi khi hủy đơn GHN: {}", e.getMessage());
                // Không throw exception để không block việc hủy đơn trong hệ thống
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForPayment(UUID orderId, User currentUser) {

        Order order = orderRepository
                .findByIdAndUserWithItems(orderId, currentUser)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderForEditCheckout(UUID orderId, User user) {

        Order order = orderRepository
                .findByIdAndUserWithItems(orderId, user)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException("Không thể chỉnh sửa đơn hàng ở trạng thái này");
        }

        return mapToResponse(order);
    }

    @Transactional
    @Override
    public void updateCheckoutInfo(UUID orderId, UpdateOrderRequest request, User user) {

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException("Không thể cập nhật đơn hàng ở trạng thái này");
        }

        // SHIPPING INFO (Lưu địa chỉ mới)
        order.setShippingName(request.getShippingName());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDistrictId(request.getShippingDistrictId());
        order.setShippingWardCode(request.getShippingWardCode());
        order.setNotes(request.getNotes());

        // TỰ ĐỘNG TÍNH LẠI PHÍ SHIP TỪ GHN DỰA TRÊN ĐỊA CHỈ MỚI
        BigDecimal newShippingFee = BigDecimal.ZERO;
        if (request.getShippingDistrictId() != null && request.getShippingWardCode() != null && !request.getShippingWardCode().isBlank()) {
            try {
                // Tạo một List<CartItem> ảo từ OrderItem để sử dụng chung hàm tính phí của ShippingService
                List<CartItem> simulatedCartItems = order.getItems().stream().map(orderItem -> {
                    CartItem item = new CartItem();
                    item.setProduct(orderItem.getProduct());
                    item.setQuantity(orderItem.getQuantity());
                    return item;
                }).toList();

                newShippingFee = shippingService.calculateShippingFeeForShop(
                        order.getShop(),
                        simulatedCartItems,
                        request.getShippingDistrictId(),
                        request.getShippingWardCode()
                );
                log.info("Đã tính lại phí ship tự động cho đơn {}: {} VNĐ", order.getOrderNumber(), newShippingFee);
            } catch (Exception e) {
                log.error("Không thể tự động tính phí ship qua GHN cho đơn {}: {}", order.getOrderNumber(), e.getMessage());
                // Nếu GHN lỗi, giữ nguyên phí ship cũ hoặc lấy phí từ request làm fallback
                newShippingFee = request.getShippingFee() != null ? request.getShippingFee() : order.getShippingFee();
            }
        } else {
            // Nếu không có địa chỉ rõ ràng, dùng phí từ UI đẩy lên
            newShippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;
        }

        order.setShippingFee(newShippingFee);

        // TÍNH LẠI TOTAL (Subtotal + New Shipping Fee)
        BigDecimal subtotal = order.getSubtotal();
        order.setTotal(subtotal.add(newShippingFee));

        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return orderRepository.count();
    }

    @Override
    public List<OrderStatus> getAllowedNextStatuses(OrderStatus current) {
        return OrderStatusTransition.getAllowedNextStatuses(current);
    }

    @Override
    @Transactional
    public boolean markDeliveredByGhnRef(String ghnOrderCode, String clientOrderCode) {
        Optional<Order> byGhn = ghnOrderCode != null && !ghnOrderCode.isBlank()
                ? orderRepository.findByGhnOrderCode(ghnOrderCode.trim()) : Optional.empty();
        Optional<Order> byClient = clientOrderCode != null && !clientOrderCode.isBlank()
                ? orderRepository.findByOrderNumber(clientOrderCode.trim()) : Optional.empty();
        Order order = byGhn.or(() -> byClient).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.SHIPPING) {
            return false;
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);

        commissionService.createCommission(order);

        log.info("GHN webhook: đơn {} đã set DELIVERED (ghnOrderCode={}, clientOrderCode={})",
                order.getOrderNumber(), ghnOrderCode, clientOrderCode);
        return true;
    }

    @Override
    @Transactional
    public OrderResponse retryCreateGhnOrder(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));
        if (user.getRole() == null || !"ROLE_SELLER".equals(user.getRole().getName())) {
            throw new CustomException("Chỉ seller mới được tạo vận đơn GHN.");
        }
        if (!order.getShop().getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền với đơn hàng này.");
        }
        if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().isEmpty()) {
            throw new CustomException("Đơn đã có mã vận đơn GHN: " + order.getGhnOrderCode());
        }
        OrderStatus s = order.getStatus();
        if (s != OrderStatus.CONFIRMED && s != OrderStatus.PROCESSING && s != OrderStatus.SHIPPING) {
            throw new CustomException("Chỉ tạo vận đơn khi đơn ở trạng thái Đã xác nhận, Đang xử lý hoặc Đã giao cho GHN.");
        }
        tryCreateGHNOrder(order);
        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse setGhnOrderCodeManually(UUID orderId, String ghnOrderCode, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));
        if (user.getRole() == null || !"ROLE_SELLER".equals(user.getRole().getName())) {
            throw new CustomException("Chỉ seller mới được nhập mã vận đơn GHN.");
        }
        if (!order.getShop().getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền với đơn hàng này.");
        }
        if (ghnOrderCode == null || ghnOrderCode.isBlank()) {
            throw new CustomException("Mã vận đơn GHN không được để trống.");
        }
        order.setGhnOrderCode(ghnOrderCode.trim());
        order = orderRepository.save(order);
        log.info("Seller đã nhập mã GHN thủ công: order {} -> ghnOrderCode={}", order.getOrderNumber(), ghnOrderCode.trim());
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public void markReceivedByBuyer(UUID orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại."));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new CustomException("Chỉ có thể xác nhận khi đơn đã giao.");
        }

        if (order.isReceivedByBuyer()) {
            throw new CustomException("Đơn hàng đã được xác nhận trước đó.");
        }

        order.setReceivedByBuyer(true);
        order.setReceivedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseNumber = "ORD-" + datePrefix + "-";
        
        // Find the highest sequence number for today
        int sequence = 1;
        String orderNumber = baseNumber + String.format("%04d", sequence);
        
        while (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            sequence++;
            orderNumber = baseNumber + String.format("%04d", sequence);
        }
        
        return orderNumber;
    }

    private OrderResponse mapToResponse(Order order) {
        // Get product images for order items
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    String productImageUrl = null;
                    Optional<ProductImage> thumbnail = productImageRepository.findByProductAndIsThumbnailTrue(item.getProduct());
                    if (thumbnail.isPresent()) {
                        productImageUrl = thumbnail.get().getImageUrl();
                    } else {
                        List<ProductImage> images = productImageRepository.findByProductOrderByDisplayOrderAsc(item.getProduct());
                        if (!images.isEmpty()) {
                            productImageUrl = images.get(0).getImageUrl();
                        }
                    }

                    boolean hasReviewedReport =
                            reviewReportRepository.existsReviewedReport(
                                    order.getUser().getId(),
                                    item.getProduct().getId(),
                                    order.getId()
                            );

                    boolean canShowReview =
                            order.getStatus() == OrderStatus.DELIVERED
                                    && order.isReceivedByBuyer()
                                    && !hasReviewedReport;

                    boolean isReviewed = reviewRepository.existsByUserIdAndProductIdAndSubOrderId(
                            order.getUser().getId(),
                            item.getProduct().getId(),
                            order.getId()
                    );

                    return OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                            .productName(item.getProductName())
                            .variantName(item.getVariantName())
                            .variantValue(item.getVariantValue())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .productImageUrl(productImageUrl)
                            .isReviewed(isReviewed)
                            .canShowReview(canShowReview)
                            .build();
                })
                .collect(Collectors.toList());

        Commission commission = commissionRepository
                .findByOrderId(order.getId())
                .orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getEmail())
                .shopId(order.getShop().getId())
                .shopName(order.getShop().getName())
                .status(order.getStatus())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingDistrict(order.getShippingDistrict())
                .shippingWard(order.getShippingWard())
                .notes(order.getNotes())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .ghnOrderCode(order.getGhnOrderCode())
                .platformCommission(commission != null ? commission.getTotalCommission() : BigDecimal.ZERO)
                .commissionRate(order.getCommissionRate())
                .items(itemResponses)
                .receivedByBuyer(order.isReceivedByBuyer())
                .receivedAt(order.getReceivedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }

    /**
     * Tạo đơn GHN nếu chưa có ghnOrderCode; không throw, chỉ log lỗi để không block cập nhật trạng thái.
     */
    private void tryCreateGHNOrder(Order order) {
        if (order.getGhnOrderCode() == null || order.getGhnOrderCode().isEmpty()) {
            try {
                GHNCreateOrderRequest ghnRequest = buildGHNCreateOrderRequest(order);
                GHNCreateOrderResponse ghnResponse = ghnService.createOrder(ghnRequest);
                order.setGhnOrderCode(ghnResponse.getOrder_code());
                log.info("Đã tạo đơn GHN: {} cho order: {}", ghnResponse.getOrder_code(), order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi tạo đơn GHN cho order {}: {} (district_id={}, ward_code={})",
                        order.getOrderNumber(), e.getMessage(),
                        order.getShippingDistrictId(), order.getShippingWardCode());
                // Không throw – seller có thể tạo vận đơn GHN thủ công sau
            }
        }
    }

    /**
     * Build GHN Create Order Request from Order entity
     */
    private GHNCreateOrderRequest buildGHNCreateOrderRequest(Order order) {
        // Calculate total weight from order items
        int totalWeight = order.getItems().stream()
                .mapToInt(item -> {
                    int productWeight = item.getProduct().getWeight() != null 
                            ? item.getProduct().getWeight() : 500; // Default 500g
                    return productWeight * item.getQuantity();
                })
                .sum();

        // Get shop's district_id and ward_code (default if not set)
        Shop shop = order.getShop();
        Integer fromDistrictId = getShopDistrictId(shop);
        String fromWardCode = getShopWardCode(shop);
        
        if (fromDistrictId == null || fromWardCode == null || fromWardCode.isBlank()) {
            fromDistrictId = 1442;   // Quận 1, HCM (GHN district_id)
            fromWardCode = "21012";  // Phường Bến Nghé (GHN ward_code)
            log.warn("Shop {} chưa có địa chỉ GHN đầy đủ (district_id/ward_code), dùng mặc định Quận 1 – Bến Nghé", shop.getName());
        }

        // Validate shipping address codes
        if (order.getShippingDistrictId() == null || order.getShippingWardCode() == null) {
            throw new CustomException("Đơn hàng chưa có đầy đủ thông tin địa chỉ giao hàng (district_id, ward_code).");
        }

        // GHN bắt buộc "Tên hàng hoá" – gửi items với name, quantity, weight từng dòng
        List<GHNCreateOrderRequest.GHNItem> ghnItems = order.getItems().stream()
                .map(item -> {
                    int productWeight = item.getProduct().getWeight() != null
                            ? item.getProduct().getWeight() : 500;
                    int itemWeight = productWeight * item.getQuantity();
                    String productName = item.getProductName() != null && !item.getProductName().isBlank()
                            ? item.getProductName() : "Sản phẩm";
                    return GHNCreateOrderRequest.GHNItem.builder()
                            .name(productName)
                            .quantity(item.getQuantity())
                            .weight(itemWeight)
                            .length(20)
                            .width(20)
                            .height(10)
                            .build();
                })
                .collect(Collectors.toList());

        return GHNCreateOrderRequest.builder()
                .payment_type_id(1) // 1 = người gửi trả (shop trả phí ship)
                .note(order.getNotes() != null ? order.getNotes() : "")
                .required_note("CHOTHUHANG") // Cho thử hàng
                .from_district_id(fromDistrictId)   // Địa chỉ lấy hàng (tránh FROM_ADDRESS_CONVERT_FAIL)
                .from_ward_code(fromWardCode)
                .to_name(order.getShippingName())
                .to_phone(order.getShippingPhone())
                .to_address(order.getShippingAddress())
                .to_ward_code(order.getShippingWardCode())
                .to_district_id(order.getShippingDistrictId())
                .weight(totalWeight)
                .length(20)
                .width(20)
                .height(10)
                .service_type_id(2) // Hàng nhẹ
                .insurance_value(order.getSubtotal() != null ? order.getSubtotal().intValue() : 0)
                .client_order_code(order.getOrderNumber())
                .items(ghnItems)
                .build();
    }

    /**
     * Get shop's district_id
     */
    private Integer getShopDistrictId(Shop shop) {
        return shop.getDistrictId();
    }

    /**
     * Get shop's ward_code
     */
    private String getShopWardCode(Shop shop) {
        return shop.getWardCode();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummaryByShop(UUID shopId) {

        BigDecimal revenue = orderRepository.getRevenueByShop(shopId);
        List<OrderStatus> estimatedStatuses = List.of(
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPING,
                OrderStatus.DELIVERED,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING
        );

        BigDecimal estimatedRevenue =
                orderRepository.getEstimatedRevenueByShop(shopId, estimatedStatuses);

        return RevenueSummaryResponse.builder()
                .revenue(revenue)
                .estimatedRevenue(estimatedRevenue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFeeForExistingOrder(UUID orderId, Integer toDistrictId, String toWardCode, User user) {
        Order order = orderRepository.findByIdAndUserWithItems(orderId, user)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng"));

        // Tạo CartItem "ảo" từ OrderItem để dùng lại hàm tính phí ship của GHN
        List<CartItem> simulatedItems = order.getItems().stream().map(oi -> {
            CartItem ci = new CartItem();
            ci.setProduct(oi.getProduct());
            ci.setQuantity(oi.getQuantity());
            ci.setVariant(oi.getVariant()); // Bổ sung Variant phòng hờ lỗi
            return ci;
        }).toList();

        return shippingService.calculateShippingFeeForShop(order.getShop(), simulatedItems, toDistrictId, toWardCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopRankingItem> getShopRanking() {
        List<Object[]> rows = orderRepository.getShopRankingByRevenue();
        List<ShopRankingItem> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            UUID shopId = (UUID) row[0];
            String shopName = (String) row[1];
            BigDecimal totalRevenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            long orderCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            result.add(ShopRankingItem.builder()
                    .rank(rank++)
                    .shopId(shopId)
                    .shopName(shopName)
                    .totalRevenue(totalRevenue)
                    .orderCount(orderCount)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSalesItem> getProductSalesByShop(UUID shopId) {
        List<Object[]> rows = orderItemRepository.getProductSalesByShop(shopId);
        List<ProductSalesItem> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID productId = (UUID) row[0];
            String productName = (String) row[1];
            long quantitySold = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            result.add(ProductSalesItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantitySold(quantitySold)
                    .build());
        }
        return result;
    }

}
