package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.dto.response.RevenueSummaryResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.platform.service.CommissionService;
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
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
    );

    private final OrderService orderService;
    private final ShopService shopService;
    private final CommissionService commissionService;

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

        long totalOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.PENDING_PAYMENT)
                .count();
        BigDecimal totalCommission =
                commissionService.getTotalCommissionBySeller(currentUser.getId());

        BigDecimal totalNetIncome =
                commissionService.getTotalNetIncomeBySeller(currentUser.getId());
        // 1. Init
        Map<OrderStatus, Long> orderCountByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatus.put(s, 0L);
        }

        // 2. Count
        for (OrderResponse o : orders) {
            OrderStatus status = o.getStatus();
            orderCountByStatus.put(status,
                    orderCountByStatus.getOrDefault(status, 0L) + 1);
        }

        // 3. Convert sang String Map (SAU KHI count)
        Map<String, Long> orderCountByStatusStr = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatusStr.put(s.name(),
                    orderCountByStatus.getOrDefault(s, 0L));
        }

        RevenueSummaryResponse revenueSummary =
                orderService.getRevenueSummaryByShop(shopId);

        model.addAttribute("totalRevenue", revenueSummary.getRevenue());
        model.addAttribute("estimatedRevenue", revenueSummary.getEstimatedRevenue());
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("totalNetIncome", totalNetIncome);
        model.addAttribute("orderCountByStatus", orderCountByStatusStr);
        model.addAttribute("orderStatusNames", Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
        return "seller/statistics";
    }
}
