package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.dto.response.TopShopDashboardItem;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
import com.hsf.e_comerce.platform.dto.response.CommissionOverviewResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionResponse;
import com.hsf.e_comerce.platform.service.CommissionService;
import com.hsf.e_comerce.platform.service.CommissionStatisticsService;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import com.hsf.e_comerce.shop.service.ShopService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
    );

    private final UserService userService;
    private final SellerRequestService sellerRequestService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ReviewReportService reviewReportService;
    private final CommissionService commissionService;


    @GetMapping("/")
    public String hello(Model model) {
        model.addAttribute("slogan", "Mua sắm thông minh – Giá tốt mỗi ngày");
        model.addAttribute("sloganSubtext", "Khám phá hàng ngàn sản phẩm điện tử, công nghệ từ các shop uy tín. Giao hàng nhanh, bảo hành chính hãng.");
        try {
            Page<ProductResponse> featuredPage = productService.getPublishedProducts(
                    0, 8, null, null, null, null, null, "createdAt", "desc");
            List<ProductResponse> featuredProducts = featuredPage.getContent();
            model.addAttribute("featuredProducts", featuredProducts);
        } catch (Exception e) {
            model.addAttribute("featuredProducts", List.<ProductResponse>of());
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    // Register moved to AuthMvcController
    // Profile moved to AuthMvcController
    // Change password moved to AuthMvcController
    // Become seller moved to SellerMvcController
    // Seller shop moved to SellerMvcController
    // Seller products moved to SellerProductMvcController
    // Admin users moved to AdminUserMvcController
    // Admin seller requests moved to AdminSellerMvcController
    // Products moved to ProductMvcController

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate toDate,
            Model model) {
        // Load statistics for dashboard (không phụ thuộc kỳ)
        try {
            long totalUsers = userService.getAllUsers().size();
            long pendingRequests = sellerRequestService.getRequestsByStatus("PENDING").size();
            long totalShops = shopService.count();
            long totalOrders = orderService.count();

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("totalShops", totalShops);
            model.addAttribute("totalOrders", totalOrders);
        } catch (Exception e) {
            model.addAttribute("totalUsers", 0);
            model.addAttribute("pendingRequests", 0);
            model.addAttribute("totalShops", 0);
            model.addAttribute("totalOrders", 0);
        }

        // Thống kê theo thời gian: doanh thu các shop, hoa hồng (chỉ đơn CONFIRMED→DELIVERED, lọc theo fromDate/toDate)
        LocalDate baseDate = LocalDate.now();
        LocalDate firstDayThisMonth = baseDate.withDayOfMonth(1);
        LocalDate firstDayNextMonth = firstDayThisMonth.plusMonths(1);

        LocalDate firstDayLastMonth = firstDayThisMonth.minusMonths(1);
        LocalDate firstDayOfThisMonth = firstDayThisMonth;
        BigDecimal revenueThisMonth = BigDecimal.ZERO;
        BigDecimal revenueLastMonth = BigDecimal.ZERO;

        long ordersThisMonth = 0;
        long ordersLastMonth = 0;

        try {
            List<OrderResponse> orders = orderService.getAllOrders();

            for (OrderResponse o : orders) {

                if (o.getStatus() != OrderStatus.DELIVERED) continue;
                if (o.getDeliveredAt() == null) continue;

                LocalDate orderDate = o.getDeliveredAt().toLocalDate();

                BigDecimal tot = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
                BigDecimal ship = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                BigDecimal net = tot.subtract(ship);

                // Tháng này
                if (!orderDate.isBefore(firstDayThisMonth) && orderDate.isBefore(firstDayNextMonth)) {
                    revenueThisMonth = revenueThisMonth.add(net);
                    ordersThisMonth++;
                }

                // Tháng trước
                if (!orderDate.isBefore(firstDayLastMonth) && orderDate.isBefore(firstDayOfThisMonth)) {
                    revenueLastMonth = revenueLastMonth.add(net);
                    ordersLastMonth++;
                }
            }
        } catch (Exception e) {
            revenueThisMonth = BigDecimal.ZERO;
            revenueLastMonth = BigDecimal.ZERO;
        }
        BigDecimal revenueGrowth = BigDecimal.ZERO;

        if (revenueLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowth = revenueThisMonth
                    .subtract(revenueLastMonth)
                    .divide(revenueLastMonth, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        BigDecimal commissionThisMonth = BigDecimal.ZERO;
        BigDecimal commissionLastMonth = BigDecimal.ZERO;

        try {
            List<CommissionResponse> commissions =
                    commissionService.getCommissions(new CommissionFilterRequest());

            for (CommissionResponse c : commissions) {

                if (c.getCreatedAt() == null) continue;

                LocalDate d = c.getCreatedAt().toLocalDate();
                BigDecimal amount = c.getCommissionAmount() != null
                        ? c.getCommissionAmount()
                        : BigDecimal.ZERO;

                if (!d.isBefore(firstDayThisMonth) && d.isBefore(firstDayNextMonth)) {
                    commissionThisMonth = commissionThisMonth.add(amount);
                }

                if (!d.isBefore(firstDayLastMonth) && d.isBefore(firstDayOfThisMonth)) {
                    commissionLastMonth = commissionLastMonth.add(amount);
                }
            }
        } catch (Exception e) {
            commissionThisMonth = BigDecimal.ZERO;
            commissionLastMonth = BigDecimal.ZERO;
        }
        BigDecimal commissionGrowth = BigDecimal.ZERO;

        if (commissionLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            commissionGrowth = commissionThisMonth
                    .subtract(commissionLastMonth)
                    .divide(commissionLastMonth, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        // ================= TREND LINE =================
        try {

            LocalDate startDate = fromDate != null ? fromDate : baseDate.minusDays(6);
            LocalDate endDate = toDate != null ? toDate : baseDate;

            Map<LocalDate, BigDecimal> revenueMap = new LinkedHashMap<>();
            Map<LocalDate, BigDecimal> commissionMap = new LinkedHashMap<>();
            Map<LocalDate, Long> ordersMap = new LinkedHashMap<>();

            // Khởi tạo đủ ngày để không bị thiếu điểm trên chart
            LocalDate cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                revenueMap.put(cursor, BigDecimal.ZERO);
                commissionMap.put(cursor, BigDecimal.ZERO);
                ordersMap.put(cursor, 0L);
                cursor = cursor.plusDays(1);
            }

            // ===== ORDERS (Revenue + Count) =====
            List<OrderResponse> orders = orderService.getAllOrders();

            for (OrderResponse o : orders) {

                if (o.getStatus() != OrderStatus.DELIVERED) continue;
                if (o.getDeliveredAt() == null) continue;

                LocalDate d = o.getDeliveredAt().toLocalDate();
                if (d.isBefore(startDate) || d.isAfter(endDate)) continue;

                BigDecimal tot = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
                BigDecimal ship = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                BigDecimal net = tot.subtract(ship);

                revenueMap.put(d, revenueMap.get(d).add(net));
                ordersMap.put(d, ordersMap.get(d) + 1);
            }

            // ===== COMMISSION =====
            List<CommissionResponse> commissions =
                    commissionService.getCommissions(new CommissionFilterRequest());

            for (CommissionResponse c : commissions) {

                if (c.getCreatedAt() == null) continue;

                LocalDate d = c.getCreatedAt().toLocalDate();
                if (d.isBefore(startDate) || d.isAfter(endDate)) continue;

                BigDecimal amount = c.getCommissionAmount() != null
                        ? c.getCommissionAmount()
                        : BigDecimal.ZERO;

                commissionMap.put(d, commissionMap.get(d).add(amount));
            }

            // Convert sang list cho Thymeleaf
            List<String> trendLabels = new ArrayList<>();
            List<BigDecimal> trendRevenue = new ArrayList<>();
            List<BigDecimal> trendCommission = new ArrayList<>();
            List<Long> trendOrders = new ArrayList<>();

            for (LocalDate d : revenueMap.keySet()) {
                trendLabels.add(d.toString());
                trendRevenue.add(revenueMap.get(d));
                trendCommission.add(commissionMap.get(d));
                trendOrders.add(ordersMap.get(d));
            }

            model.addAttribute("trendLabels", trendLabels);
            model.addAttribute("trendRevenue", trendRevenue);
            model.addAttribute("trendCommission", trendCommission);
            model.addAttribute("trendOrders", trendOrders);

        } catch (Exception e) {
            model.addAttribute("trendLabels", List.of());
            model.addAttribute("trendRevenue", List.of());
            model.addAttribute("trendCommission", List.of());
            model.addAttribute("trendOrders", List.of());
        }
        // ================= TOP SHOP DASHBOARD =================
        try {

            Map<String, BigDecimal> revenueThisMonthByShop = new HashMap<>();
            Map<String, BigDecimal> revenueLastMonthByShop = new HashMap<>();
            Map<String, Long> ordersThisMonthByShop = new HashMap<>();

            List<OrderResponse> orders = orderService.getAllOrders();

            for (OrderResponse o : orders) {

                if (o.getStatus() != OrderStatus.DELIVERED) continue;
                if (o.getDeliveredAt() == null) continue;
                if (o.getShopName() == null) continue;

                String shop = o.getShopName();
                LocalDate d = o.getDeliveredAt().toLocalDate();

                BigDecimal tot = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
                BigDecimal ship = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                BigDecimal net = tot.subtract(ship);

                // Tháng này
                if (!d.isBefore(firstDayThisMonth) && d.isBefore(firstDayNextMonth)) {
                    revenueThisMonthByShop.put(
                            shop,
                            revenueThisMonthByShop.getOrDefault(shop, BigDecimal.ZERO).add(net)
                    );

                    ordersThisMonthByShop.put(
                            shop,
                            ordersThisMonthByShop.getOrDefault(shop, 0L) + 1
                    );
                }

                // Tháng trước
                if (!d.isBefore(firstDayLastMonth) && d.isBefore(firstDayThisMonth)) {
                    revenueLastMonthByShop.put(
                            shop,
                            revenueLastMonthByShop.getOrDefault(shop, BigDecimal.ZERO).add(net)
                    );
                }
            }

            List<TopShopDashboardItem> topShops = new ArrayList<>();

            for (String shop : revenueThisMonthByShop.keySet()) {

                BigDecimal thisMonth = revenueThisMonthByShop.getOrDefault(shop, BigDecimal.ZERO);
                BigDecimal lastMonth = revenueLastMonthByShop.getOrDefault(shop, BigDecimal.ZERO);

                BigDecimal growth = BigDecimal.ZERO;

                if (lastMonth.compareTo(BigDecimal.ZERO) > 0) {
                    growth = thisMonth
                            .subtract(lastMonth)
                            .divide(lastMonth, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }

                topShops.add(new TopShopDashboardItem(
                        shop,
                        thisMonth,
                        lastMonth,
                        growth,
                        ordersThisMonthByShop.getOrDefault(shop, 0L)
                ));
            }

            // 🔥 Top 5 doanh thu cao nhất tháng này
            List<TopShopDashboardItem> top5Shops = topShops.stream()
                    .sorted(Comparator.comparing(TopShopDashboardItem::getRevenueThisMonth).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            // 🚀 Shop tăng trưởng nhanh nhất
            TopShopDashboardItem fastestGrowthShop = topShops.stream()
                    .max(Comparator.comparing(TopShopDashboardItem::getGrowthPercent))
                    .orElse(null);

            model.addAttribute("top5Shops", top5Shops);
            model.addAttribute("fastestGrowthShop", fastestGrowthShop);

        } catch (Exception e) {
            model.addAttribute("top5Shops", List.of());
            model.addAttribute("fastestGrowthShop", null);
        }

        model.addAttribute("revenueThisMonth", revenueThisMonth);
        model.addAttribute("revenueLastMonth", revenueLastMonth);
        model.addAttribute("revenueGrowth", revenueGrowth);

        model.addAttribute("ordersThisMonth", ordersThisMonth);
        model.addAttribute("ordersLastMonth", ordersLastMonth);

        model.addAttribute("commissionThisMonth", commissionThisMonth);
        model.addAttribute("commissionLastMonth", commissionLastMonth);
        model.addAttribute("commissionGrowth", commissionGrowth);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("urlLast7", "/admin/dashboard?fromDate=" + baseDate.minusDays(7) + "&toDate=" + baseDate);
        model.addAttribute("urlLast30", "/admin/dashboard?fromDate=" + baseDate.minusDays(30) + "&toDate=" + baseDate);
        model.addAttribute("urlLast90", "/admin/dashboard?fromDate=" + baseDate.minusDays(90) + "&toDate=" + baseDate);
        model.addAttribute("urlAll", "/admin/dashboard");
        model.addAttribute(
                "reportedReviewCount",
                reviewReportService.countPendingReportedReviews()
        );
        BigDecimal totalCommission = BigDecimal.ZERO;
        long totalCommissionOrders = 0;

        try {
            List<CommissionResponse> commissions =
                    commissionService.getCommissions(new CommissionFilterRequest());

            for (CommissionResponse c : commissions) {

                if (c.getCreatedAt() == null) continue;

                LocalDate d = c.getCreatedAt().toLocalDate();

                if (fromDate != null && d.isBefore(fromDate)) continue;
                if (toDate != null && d.isAfter(toDate)) continue;

                totalCommission = totalCommission.add(
                        c.getCommissionAmount() != null
                                ? c.getCommissionAmount()
                                : BigDecimal.ZERO
                );

                totalCommissionOrders++;
            }

        } catch (Exception e) {
            totalCommission = BigDecimal.ZERO;
            totalCommissionOrders = 0;
        }

        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("totalCommissionOrders", totalCommissionOrders);

        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            BigDecimal totalRevenueShops = BigDecimal.ZERO;
            for (OrderResponse o : orders) {
                if (o.getStatus() != OrderStatus.DELIVERED) continue;
                LocalDateTime deliveredAt = o.getDeliveredAt();
                if (deliveredAt != null) {
                    LocalDate d = deliveredAt.toLocalDate();
                    if (fromDate != null && d.isBefore(fromDate)) continue;
                    if (toDate != null && d.isAfter(toDate)) continue;
                }
                BigDecimal tot = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
                BigDecimal ship = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                totalRevenueShops = totalRevenueShops.add(tot.subtract(ship));
            }
            model.addAttribute("totalRevenueShops", totalRevenueShops);
        } catch (Exception e) {
            model.addAttribute("totalRevenueShops", BigDecimal.ZERO);
        }

        // Hoạt động gần đây: trích từ orders + seller_requests, N bản ghi mới nhất
        try {
            List<RecentActivityItem> activities = new ArrayList<>();
            List<OrderResponse> orders = orderService.getAllOrders();
            int orderLimit = Math.min(5, orders.size());
            for (int i = 0; i < orderLimit; i++) {
                OrderResponse o = orders.get(i);
                String title = "Đơn #" + (o.getOrderNumber() != null ? o.getOrderNumber() : o.getId().toString())
                        + " – " + (o.getShopName() != null ? o.getShopName() : "")
                        + " – " + (o.getStatus() != null ? o.getStatus().name() : "");
                activities.add(new RecentActivityItem("ORDER", title, o.getCreatedAt(), "/admin/orders/" + o.getId()));
            }
            List<SellerRequestResponse> reqs = sellerRequestService.getAllRequests();
            reqs.sort(Comparator.comparing(SellerRequestResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            int reqLimit = Math.min(5, reqs.size());
            for (int i = 0; i < reqLimit; i++) {
                SellerRequestResponse r = reqs.get(i);
                String title = "Yêu cầu seller – " + (r.getShopName() != null ? r.getShopName() : "") + " – " + (r.getStatus() != null ? r.getStatus() : "");
                activities.add(new RecentActivityItem("SELLER_REQUEST", title, r.getCreatedAt(), "/admin/seller-requests"));
            }
            activities.sort(Comparator.comparing(RecentActivityItem::getAt, Comparator.nullsLast(Comparator.reverseOrder())));
            List<RecentActivityItem> recentActivities = activities.stream().limit(10).collect(Collectors.toList());
            model.addAttribute("recentActivities", recentActivities);
        } catch (Exception e) {
            model.addAttribute("recentActivities", List.<RecentActivityItem>of());
        }

        return "admin/dashboard";
    }

    /** Một dòng trong "Hoạt động gần đây": đơn hàng hoặc yêu cầu seller. */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class RecentActivityItem {
        private final String type;
        private final String title;
        private final LocalDateTime at;
        private final String linkUrl;
    }

    // Admin products moved to AdminProductMvcController
    // Admin orders moved to AdminOrderMvcController
    // Seller orders moved to SellerOrderMvcController
    // Seller statistics moved to SellerStatisticsMvcController

    // Handle favicon requests to avoid warnings
    @GetMapping("/favicon.ico")
    public String favicon() {
        return "forward:/"; // Redirect to home, browser will handle missing favicon gracefully
    }

    // Handle .well-known requests (Chrome DevTools, etc.) to avoid warnings
    @GetMapping("/.well-known/**")
    public void wellKnown(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}