package com.hsf.e_comerce.order.service.impl;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartItemRepository;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.request.CreateOrderRequest;
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
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopRepository shopRepository;
    private final ProductImageRepository productImageRepository;

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
        order.setStatus(OrderStatus.PENDING);
        order.setShippingName(request.getShippingName());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDistrict(request.getShippingDistrict());
        order.setShippingWard(request.getShippingWard());
        order.setNotes(request.getNotes());
        order.setSubtotal(subtotal);
        order.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
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
        for (CartItem cartItem : shopCartItems) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        }
        cartRepository.save(cart);

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
        Order order = orderRepository.findById(orderId)
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
        List<Order> orders = orderRepository.findByShopIdWithItems(shopId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByShopAndStatus(UUID shopId, OrderStatus status) {
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
            currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new CustomException("Không thể hủy đơn hàng ở trạng thái " + currentStatus);
        }

        // If cancelling, restore stock
        if (newStatus == OrderStatus.CANCELLED && 
            (currentStatus == OrderStatus.PENDING || currentStatus == OrderStatus.CONFIRMED)) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null) {
                    item.getVariant().setStockQuantity(
                            item.getVariant().getStockQuantity() + item.getQuantity()
                    );
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
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
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

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        return mapToResponse(order);
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
                    // Get product image (thumbnail or first image)
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
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
