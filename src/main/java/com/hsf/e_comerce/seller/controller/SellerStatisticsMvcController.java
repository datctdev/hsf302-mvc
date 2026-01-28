package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerStatisticsMvcController {

    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final OrderService orderService;
    private final ShopService shopService;

    @GetMapping("/statistics")
    public String statistics(@CurrentUser User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        ShopResponse shop;
        try {
            shop = shopService.getShopByUserId(currentUser.getId());
        } catch (CustomException e) {
            return "redirect:/seller/become-seller";
        }
        model.addAttribute("shopName", shop.getName());
        UUID shopId = shop.getId();

        List<OrderResponse> orders = orderService.getOrdersByShop(shopId);

        long totalOrders = orders.size();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        Map<OrderStatus, Long> orderCountByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatus.put(s, 0L);
        }

        for (OrderResponse o : orders) {
            orderCountByStatus.merge(o.getStatus(), 1L, Long::sum);
            if (REVENUE_STATUSES.contains(o.getStatus())) {
                if (o.getTotal() != null) {
                    totalRevenue = totalRevenue.add(o.getTotal());
                }
                if (o.getPlatformCommission() != null) {
                    totalCommission = totalCommission.add(o.getPlatformCommission());
                }
            }
        }

        // Thymeleaf gặp lỗi khi Map key là enum; dùng Map<String,Long> key = status.name()
        Map<String, Long> orderCountByStatusStr = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatusStr.put(s.name(), orderCountByStatus.getOrDefault(s, 0L));
        }
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("orderCountByStatus", orderCountByStatusStr);
        model.addAttribute("orderStatusNames", Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
        return "seller/statistics";
    }
}
