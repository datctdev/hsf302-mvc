package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.response.OrderItemResponse;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.dto.response.ProductSalesItem;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerStatisticsMvcController {

    private final OrderService orderService;
    private final ShopService shopService;

    @GetMapping("/statistics")
    public String statistics(
            @CurrentUser User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

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

        // Lấy tất cả đơn hàng của Shop
        List<OrderResponse> allOrders = orderService.getOrdersByShop(shopId);

        // 1. Xử lý Logic Lọc Ngày Tháng (Mặc định 30 ngày gần nhất)
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(29);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        // Lọc đơn hàng nằm trong khoảng thời gian đã chọn
        List<OrderResponse> filteredOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        !o.getCreatedAt().isBefore(startDateTime) &&
                        !o.getCreatedAt().isAfter(endDateTime))
                .toList();

        // 2. Tính toán các chỉ số thẻ Card
        long totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalNetIncome = BigDecimal.ZERO;
        BigDecimal estimatedRevenue = BigDecimal.ZERO;

        Map<OrderStatus, Long> orderCountByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatus.put(s, 0L);
        }

        Set<OrderStatus> estimatedStatuses = Set.of(
                OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderStatus.PENDING
        );

        for (OrderResponse o : filteredOrders) {
            OrderStatus status = o.getStatus();
            orderCountByStatus.put(status, orderCountByStatus.get(status) + 1);

            if (status != OrderStatus.PENDING_PAYMENT) totalOrders++;

            if (status == OrderStatus.DELIVERED) {
                totalRevenue = totalRevenue.add(o.getTotal());
                BigDecimal commission = o.getPlatformCommission() != null ? o.getPlatformCommission() : BigDecimal.ZERO;
                BigDecimal shipFee = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;

                totalCommission = totalCommission.add(commission);
                // Thực nhận = Tổng thu - Hoa hồng - Phí ship
                totalNetIncome = totalNetIncome.add(o.getTotal().subtract(commission).subtract(shipFee));
            }

            if (estimatedStatuses.contains(status)) {
                estimatedRevenue = estimatedRevenue.add(o.getTotal());
            }
        }

        Map<String, Long> orderCountByStatusStr = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            orderCountByStatusStr.put(s.name(), orderCountByStatus.get(s));
        }

        // 3. Xử lý Dữ liệu cho Biểu đồ Doanh thu (Chart.js)
        Map<String, BigDecimal> dailyRevenueMap = new LinkedHashMap<>();
        long numOfDays = ChronoUnit.DAYS.between(start, end);
        for (int i = 0; i <= numOfDays; i++) {
            dailyRevenueMap.put(start.plusDays(i).format(DateTimeFormatter.ofPattern("dd/MM")), BigDecimal.ZERO);
        }

        filteredOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getDeliveredAt() != null)
                .forEach(o -> {
                    String dateStr = o.getDeliveredAt().format(DateTimeFormatter.ofPattern("dd/MM"));
                    if (dailyRevenueMap.containsKey(dateStr)) {
                        dailyRevenueMap.put(dateStr, dailyRevenueMap.get(dateStr).add(o.getTotal()));
                    }
                });

        // Convert List to JSON Strings for Frontend
        String labelsJson = dailyRevenueMap.keySet().stream().map(l -> "\"" + l + "\"").collect(Collectors.joining(",", "[", "]"));
        String dataJson = dailyRevenueMap.values().stream().map(BigDecimal::toString).collect(Collectors.joining(",", "[", "]"));

        // 4. Lọc Sản phẩm bán chạy trong khoảng thời gian
        Map<UUID, ProductSalesItem> salesMap = new HashMap<>();
        filteredOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .forEach(o -> {
                    for (OrderItemResponse item : o.getItems()) {
                        UUID pId = item.getProductId();
                        ProductSalesItem salesItem = salesMap.getOrDefault(pId, ProductSalesItem.builder()
                                .productId(pId).productName(item.getProductName()).quantitySold(0L).build());
                        salesItem.setQuantitySold(salesItem.getQuantitySold() + item.getQuantity());
                        salesMap.put(pId, salesItem);
                    }
                });

        List<ProductSalesItem> productSales = salesMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getQuantitySold(), a.getQuantitySold()))
                .limit(10).toList();

        // 5. Đẩy dữ liệu ra giao diện
        model.addAttribute("paramStartDate", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("paramEndDate", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("chartLabels", labelsJson);
        model.addAttribute("chartData", dataJson);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("estimatedRevenue", estimatedRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("totalNetIncome", totalNetIncome);
        model.addAttribute("orderCountByStatus", orderCountByStatusStr);
        model.addAttribute("orderStatusNames", Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
        model.addAttribute("productSales", productSales);

        return "seller/statistics";
    }
}