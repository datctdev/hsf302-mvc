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
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.order.repository.OrderItemRepository;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shipping.service.GHNService;
import com.hsf.e_comerce.shipping.dto.request.GHNCreateOrderRequest;
import com.hsf.e_comerce.shipping.dto.response.GHNCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final ReviewRepository reviewRepository;
    private final PlatformSettingService platformSettingService;

    @Override
    @Transactional
    public OrderResponse createOrder(User user, CreateOrderRequest request) {
        // Validate shop exists
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new CustomException("Shop không tồn tại."));

        // Get user's cart
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new CustomException("Giỏ hàng trống."));

        // Filter cart items by shop
        List<CartItem> shopCartItems = cart.getItems().stream()
                .filter(item -> item.getProduct().getShop().getId().equals(shop.getId()))
                .collect(Collectors.toList());

        if (shopCartItems.isEmpty()) {
            throw new CustomException("Không có sản phẩm nào từ shop này trong giỏ hàng.");
        }

        // Validate stock and prepare order items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : shopCartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            if (variant == null) {
                throw new CustomException("Sản phẩm " + product.getName() + " chưa có biến thể.");
            }

            // Check stock
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new CustomException("Sản phẩm " + product.getName() + " không đủ số lượng. Còn lại: " + variant.getStockQuantity());
            }

            // Calculate item total
            BigDecimal unitPrice = product.getBasePrice().add(
                    variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO
            );
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order
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
        order.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
        // Hoa hồng nền tảng: platform_commission = subtotal * (commission_rate / 100)
        BigDecimal commissionRate = platformSettingService.getCommissionRate();
        BigDecimal platformCommission = subtotal.multiply(commissionRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.setPlatformCommission(platformCommission != null ? platformCommission : BigDecimal.ZERO);
        order.setCommissionRate(commissionRate != null ? commissionRate.doubleValue() : null);
        order.calculateTotal();

        order = orderRepository.save(order);

        // Create order items and reduce stock
        for (CartItem cartItem : shopCartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            // Reduce stock
            int newStock = variant.getStockQuantity() - cartItem.getQuantity();
            variant.setStockQuantity(newStock);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductName(product.getName());
            orderItem.setVariantName(variant.getName());
            orderItem.setVariantValue(variant.getValue());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal unitPrice = product.getBasePrice().add(
                    variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO
            );
            orderItem.setUnitPrice(unitPrice);
            orderItem.calculateTotalPrice();

            order.getItems().add(orderItem);
            orderItemRepository.save(orderItem);
        }

        // Remove cart items from cart
//        for (CartItem cartItem : shopCartItems) {
//            cart.getItems().remove(cartItem);
//            cartItemRepository.delete(cartItem);
//        }
//        cartRepository.save(cart);

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
    public List<OrderResponse> getOrdersByShop(UUID shopId) {
//        List<Order> orders = orderRepository.findByShopIdWithItems(shopId);
        List<Order> orders = orderRepository
                .findByShopIdAndStatusNot(shopId, OrderStatus.PENDING_PAYMENT);
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

        List<Order> orders = orderRepository.findByShopIdAndStatus(shopId, status);
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

        // Validate status transition
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Only allow cancellation if order is PENDING or CONFIRMED
        if (newStatus == OrderStatus.CANCELLED && 
            currentStatus != OrderStatus.PENDING_PAYMENT && currentStatus != OrderStatus.CONFIRMED) {
            throw new CustomException("Không thể hủy đơn hàng ở trạng thái " + currentStatus);
        }

        // If cancelling, restore stock and cancel GHN order
        if (newStatus == OrderStatus.CANCELLED && 
            (currentStatus == OrderStatus.PENDING_PAYMENT || currentStatus == OrderStatus.CONFIRMED)) {
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

        // Create GHN order when seller confirms (PENDING -> CONFIRMED)
        if (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.CONFIRMED) {
            if (order.getGhnOrderCode() == null || order.getGhnOrderCode().isEmpty()) {
                try {
                    GHNCreateOrderRequest ghnRequest = buildGHNCreateOrderRequest(order);
                    GHNCreateOrderResponse ghnResponse = ghnService.createOrder(ghnRequest);
                    order.setGhnOrderCode(ghnResponse.getOrder_code());
                    log.info("Đã tạo đơn GHN: {} cho order: {}", ghnResponse.getOrder_code(), order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Lỗi khi tạo đơn GHN cho order {}: {}", order.getOrderNumber(), e.getMessage());
                    // Không throw exception để không block việc confirm đơn
                    // Seller có thể tạo đơn GHN thủ công sau
                }
            }
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

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
            throw new CustomException("Không thể cập nhật đơn hàng");
        }

        // ✅ SHIPPING INFO
        order.setShippingName(request.getShippingName());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDistrictId(request.getShippingDistrictId());
        order.setShippingWardCode(request.getShippingWardCode());
        order.setNotes(request.getNotes());

        // 🔥 FIX QUAN TRỌNG
        order.setShippingFee(request.getShippingFee());

        // 🔥 TÍNH LẠI TOTAL
        BigDecimal subtotal = order.getSubtotal();
        BigDecimal shippingFee = request.getShippingFee() != null
                ? request.getShippingFee()
                : BigDecimal.ZERO;

        order.setTotal(subtotal.add(shippingFee));

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

                    boolean isReviewed = reviewRepository.existsByUserIdAndProductIdAndSubOrderIdAndStatus(
                            order.getUser().getId(),
                            item.getProduct().getId(),
                            order.getId(),
                            com.hsf.e_comerce.review.valueobject.ReviewStatus.ACTIVE
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
                            .build();
                })
                .collect(Collectors.toList());

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
                .platformCommission(order.getPlatformCommission() != null ? order.getPlatformCommission() : BigDecimal.ZERO)
                .commissionRate(order.getCommissionRate())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
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
        
        if (fromDistrictId == null || fromWardCode == null) {
            fromDistrictId = 1442; // Quận 1, HCM
            fromWardCode = "21012"; // Phường Bến Nghé
            log.warn("Shop {} chưa có địa chỉ đầy đủ, sử dụng giá trị mặc định", shop.getName());
        }

        // Validate shipping address codes
        if (order.getShippingDistrictId() == null || order.getShippingWardCode() == null) {
            throw new CustomException("Đơn hàng chưa có đầy đủ thông tin địa chỉ giao hàng (district_id, ward_code).");
        }

        return GHNCreateOrderRequest.builder()
                .payment_type_id(1) // 1 = người gửi trả (shop trả phí ship)
                .note(order.getNotes() != null ? order.getNotes() : "")
                .required_note("CHOTHUHANG") // Cho thử hàng
                .to_name(order.getShippingName())
                .to_phone(order.getShippingPhone())
                .to_address(order.getShippingAddress())
                .to_ward_code(order.getShippingWardCode())
                .to_district_id(String.valueOf(order.getShippingDistrictId())) // GHN API expects String
                .weight(totalWeight)
                .length(20) // Default dimensions (cm)
                .width(20)
                .height(10)
                .service_type_id(2) // Hàng nhẹ
                .insurance_value(order.getSubtotal().intValue()) // Khai giá = tổng tiền sản phẩm
                .client_order_code(order.getOrderNumber()) // Mã đơn hàng của hệ thống
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
}
