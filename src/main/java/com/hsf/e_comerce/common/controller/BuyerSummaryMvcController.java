package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trang "Tổng quan của tôi" cho buyer: số đơn, tổng chi tiêu, đơn gần đây.
 * Route /my-summary – cần đăng nhập.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BuyerSummaryMvcController {

    private static final Set<OrderStatus> PAID_STATUSES = Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
    );

    private static final int RECENT_ORDERS_LIMIT = 5;

    private final OrderService orderService;

    @GetMapping("/my-summary")
    public String mySummary(@CurrentUser User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<OrderResponse> orders = orderService.getOrdersByUser(currentUser);
        long totalOrders = orders.size();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (OrderResponse o : orders) {
            if (PAID_STATUSES.contains(o.getStatus()) && o.getTotal() != null) {
                totalSpent = totalSpent.add(o.getTotal());
            }
        }
        List<OrderResponse> recentOrders = orders.stream().limit(RECENT_ORDERS_LIMIT).collect(Collectors.toList());

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("recentOrders", recentOrders);
        return "my-summary";
    }
}
