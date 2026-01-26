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
}
