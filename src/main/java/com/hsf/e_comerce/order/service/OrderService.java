package com.hsf.e_comerce.order.service;

import com.hsf.e_comerce.order.dto.request.CreateOrderRequest;
import com.hsf.e_comerce.order.dto.request.UpdateOrderRequest;
import com.hsf.e_comerce.order.dto.request.UpdateOrderStatusRequest;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.auth.entity.User;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    List<OrderStatus> getAllowedNextStatuses(OrderStatus current);
    
    OrderResponse createOrder(User user, CreateOrderRequest request);
    
    OrderResponse getOrderById(UUID orderId);
    
    OrderResponse getOrderByIdAndUser(UUID orderId, User user);
    
    OrderResponse getOrderByIdAndShop(UUID orderId, UUID shopId);
    
    List<OrderResponse> getOrdersByUser(User user);
    
    List<OrderResponse> getOrdersByShop(UUID shopId);
    
    List<OrderResponse> getOrdersByShopAndStatus(UUID shopId, OrderStatus status);
    
    List<OrderResponse> getAllOrders();
    
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, User user);
    
    OrderResponse cancelOrder(UUID orderId, User user);

    OrderResponse getOrderForPayment(UUID orderId, User currentUser);

    OrderResponse getOrderForEditCheckout(UUID orderId, User currentUser);

    void updateCheckoutInfo(UUID orderId, @Valid UpdateOrderRequest request, User user);

    long count();

    boolean markDeliveredByGhnRef(String ghnOrderCode, String clientOrderCode);

    /**
     * Tạo lại vận đơn GHN cho đơn (seller). Chỉ khi đơn chưa có ghnOrderCode và trạng thái CONFIRMED/PROCESSING/SHIPPED.
     */
    OrderResponse retryCreateGhnOrder(UUID orderId, User user);

    /**
     * Nhập mã vận đơn GHN thủ công (seller). Dùng khi đã tạo đơn trên GHN và muốn gắn mã vào đơn trong hệ thống.
     */
    OrderResponse setGhnOrderCodeManually(UUID orderId, String ghnOrderCode, User user);
}
