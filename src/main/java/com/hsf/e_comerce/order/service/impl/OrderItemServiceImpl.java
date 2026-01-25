package com.hsf.e_comerce.order.service.impl;

import com.hsf.e_comerce.cart.dto.response.CartItemResponse;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.order.repository.OrderItemRepository;
import com.hsf.e_comerce.order.service.OrderItemService;
import com.hsf.e_comerce.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Override
    public Map<UUID, List<CartItemResponse>> getItemsByOrder(UUID orderId) {

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems == null || orderItems.isEmpty()) {
            throw new RuntimeException("Order không có item");
        }

        return orderItems.stream()
                .filter(oi -> oi.getProduct() != null
                        && oi.getProduct().getShop() != null)
                .collect(Collectors.groupingBy(
                        oi -> oi.getProduct().getShop().getId(),
                        Collectors.mapping(
                                CartItemResponse::fromOrderItem,
                                Collectors.toList()
                        )
                ));
    }

}

