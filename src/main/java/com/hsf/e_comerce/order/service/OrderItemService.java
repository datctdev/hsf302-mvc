package com.hsf.e_comerce.order.service;

import com.hsf.e_comerce.cart.dto.response.CartItemResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderItemService {
    Map<UUID, List<CartItemResponse>> getItemsByOrder(UUID orderId);

    BigDecimal calculateSubtotal(Shop shop, List<CartItemResponse> cartItemResponses);
}
