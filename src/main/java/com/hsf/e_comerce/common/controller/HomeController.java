package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.dto.response.TopShopDashboardItem;
import com.hsf.e_comerce.kyc.repository.KycSessionRepository;
import com.hsf.e_comerce.kyc.valueobject.KycStatus;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final SellerRequestService sellerRequestService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ReviewReportService reviewReportService;
    private final CommissionService commissionService;

    private final KycSessionRepository kycSessionRepository;
    private final CommissionStatisticsService commissionStatisticsService;

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

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        // ================= 1. XỬ LÝ KỲ BÁO CÁO =================
        LocalDate endDate = toDate != null ? toDate : LocalDate.now();
        LocalDate startDate = fromDate != null ? fromDate : endDate.minusDays(30);

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysBetween);

        // ================= 2. KHỞI TẠO BIẾN ĐẾM =================
        BigDecimal revenueCurrent = BigDecimal.ZERO;
        BigDecimal revenuePrev = BigDecimal.ZERO;
        long ordersCurrent = 0, ordersPrev = 0;

        long countDelivered = 0, countConfirmed = 0, countProcessing = 0,
                countShipping = 0, countPendingPayment = 0, countCancelled = 0;

        Map<String, BigDecimal> revenueCurrentByShop = new HashMap<>();
        Map<String, Long> ordersCurrentByShop = new HashMap<>();
        Map<String, BigDecimal> revenuePrevByShop = new HashMap<>();
        Map<String, BigDecimal> topBuyersMap = new HashMap<>();

        Map<LocalDate, BigDecimal> revenueMap = new TreeMap<>();
        Map<LocalDate, BigDecimal> commissionMap = new TreeMap<>();
        Map<LocalDate, Long> orderCountMap = new TreeMap<>();
        Map<LocalDate, Long> userCountMap = new TreeMap<>(); // MỚI: Biểu đồ User

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            revenueMap.put(cursor, BigDecimal.ZERO);
            commissionMap.put(cursor, BigDecimal.ZERO);
            orderCountMap.put(cursor, 0L);
            userCountMap.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }

        // ================= 3. LỌC ĐƠN HÀNG =================
        List<OrderResponse> recentOrders = new ArrayList<>();
        try {
            List<OrderResponse> orders = orderService.getAllOrders();

            recentOrders = orders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(7)
                    .collect(Collectors.toList());

            for (OrderResponse o : orders) {
                LocalDate orderDate = o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : null;
                if (orderDate == null) continue;

                if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                    if (o.getStatus() == OrderStatus.DELIVERED) countDelivered++;
                    else if (o.getStatus() == OrderStatus.CONFIRMED) countConfirmed++;
                    else if (o.getStatus() == OrderStatus.PROCESSING) countProcessing++;
                    else if (o.getStatus() == OrderStatus.SHIPPING) countShipping++;
                    else if (o.getStatus() == OrderStatus.PENDING_PAYMENT) countPendingPayment++;
                    else if (o.getStatus() == OrderStatus.CANCELLED) countCancelled++;

                    orderCountMap.put(orderDate, orderCountMap.getOrDefault(orderDate, 0L) + 1);

                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        BigDecimal net = (o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                                .subtract(o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO);
                        revenueCurrent = revenueCurrent.add(net);
                        ordersCurrent++;

                        String shop = o.getShopName() != null ? o.getShopName() : "Unknown";
                        revenueCurrentByShop.put(shop, revenueCurrentByShop.getOrDefault(shop, BigDecimal.ZERO).add(net));
                        ordersCurrentByShop.put(shop, ordersCurrentByShop.getOrDefault(shop, 0L) + 1);

                        String buyer = o.getUserName() != null ? o.getUserName() : "Khách ẩn danh";
                        topBuyersMap.put(buyer, topBuyersMap.getOrDefault(buyer, BigDecimal.ZERO).add(net));

                        revenueMap.put(orderDate, revenueMap.getOrDefault(orderDate, BigDecimal.ZERO).add(net));
                    }
                } else if (!orderDate.isBefore(prevStartDate) && !orderDate.isAfter(prevEndDate)) {
                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        BigDecimal net = (o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                                .subtract(o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO);
                        revenuePrev = revenuePrev.add(net);
                    }
                }
            }
        } catch (Exception e) {}

        // ================= 4. ĐẾM SỐ LƯỢNG USER ĐĂNG KÝ (MỚI) =================
        try {
            var allUsers = userService.getAllUsers();
            model.addAttribute("totalUsers", allUsers.size());
            model.addAttribute("totalShops", shopService.count());

            for (var u : allUsers) {
                LocalDate regDate = u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate() : null;
                if (regDate != null && !regDate.isBefore(startDate) && !regDate.isAfter(endDate)) {
                    userCountMap.put(regDate, userCountMap.getOrDefault(regDate, 0L) + 1);
                }
            }
        } catch (Exception e) {}

        // ================= 5. TÍNH HOA HỒNG =================
        BigDecimal commissionCurrent = BigDecimal.ZERO;
        BigDecimal commissionPrev = BigDecimal.ZERO;
        try {
            List<CommissionResponse> commissions = commissionService.getCommissions(new CommissionFilterRequest());
            for (CommissionResponse c : commissions) {
                LocalDate d = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null;
                if (d == null) continue;
                BigDecimal amount = c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO;

                if (!d.isBefore(startDate) && !d.isAfter(endDate)) {
                    commissionCurrent = commissionCurrent.add(amount);
                    commissionMap.put(d, commissionMap.getOrDefault(d, BigDecimal.ZERO).add(amount));
                } else if (!d.isBefore(prevStartDate) && !d.isAfter(prevEndDate)) {
                    commissionPrev = commissionPrev.add(amount);
                }
            }
        } catch (Exception e) {}

        // ================= 6. AOV & TĂNG TRƯỞNG =================
        BigDecimal aovCurrent = ordersCurrent > 0
                ? revenueCurrent.divide(BigDecimal.valueOf(ordersCurrent), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal revenueGrowth = revenuePrev.compareTo(BigDecimal.ZERO) > 0
                ? revenueCurrent.subtract(revenuePrev).divide(revenuePrev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        BigDecimal commissionGrowth = commissionPrev.compareTo(BigDecimal.ZERO) > 0
                ? commissionCurrent.subtract(commissionPrev).divide(commissionPrev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        // ================= 7. XỬ LÝ TOP (SHOP & 100 BUYERS) =================
        try {
            List<TopShopDashboardItem> topShops = new ArrayList<>();
            for (String shop : revenueCurrentByShop.keySet()) {
                BigDecimal thisP = revenueCurrentByShop.get(shop);
                BigDecimal lastP = revenuePrevByShop.getOrDefault(shop, BigDecimal.ZERO);
                BigDecimal growth = lastP.compareTo(BigDecimal.ZERO) > 0 ? thisP.subtract(lastP).divide(lastP, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
                topShops.add(new TopShopDashboardItem(shop, thisP, lastP, growth, ordersCurrentByShop.getOrDefault(shop, 0L)));
            }
            model.addAttribute("top5Shops", topShops.stream().sorted(Comparator.comparing(TopShopDashboardItem::getRevenueThisMonth).reversed()).limit(5).collect(Collectors.toList()));
            model.addAttribute("fastestGrowthShop", topShops.stream().max(Comparator.comparing(TopShopDashboardItem::getGrowthPercent)).orElse(null));

            // Lấy Top 100 Buyers
            List<Map.Entry<String, BigDecimal>> top100Buyers = topBuyersMap.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(100) // ĐÃ TĂNG LÊN 100
                    .collect(Collectors.toList());
            model.addAttribute("topBuyers", top100Buyers);
        } catch (Exception e) {}

        // ================= 8. GÁN DỮ LIỆU RA MODEL =================
        List<String> stringLabels = revenueMap.keySet().stream().map(LocalDate::toString).collect(Collectors.toList());
        model.addAttribute("trendLabels", stringLabels);
        model.addAttribute("trendRevenue", new ArrayList<>(revenueMap.values()));
        model.addAttribute("trendCommission", new ArrayList<>(commissionMap.values()));
        model.addAttribute("trendOrderCounts", new ArrayList<>(orderCountMap.values()));
        model.addAttribute("trendUsers", new ArrayList<>(userCountMap.values())); // Data biểu đồ User

        model.addAttribute("countDelivered", countDelivered);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countProcessing", countProcessing);
        model.addAttribute("countShipping", countShipping);
        model.addAttribute("countPendingPayment", countPendingPayment);
        model.addAttribute("countCancelled", countCancelled);

        model.addAttribute("aovThisMonth", aovCurrent);
        model.addAttribute("revenueThisMonth", revenueCurrent);
        model.addAttribute("revenueGrowth", revenueGrowth);
        model.addAttribute("ordersThisMonth", ordersCurrent);
        model.addAttribute("commissionThisMonth", commissionCurrent);
        model.addAttribute("commissionGrowth", commissionGrowth);
        model.addAttribute("recentOrders", recentOrders);

        LocalDate baseDate = LocalDate.now();
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("urlLast1", "/admin/dashboard?fromDate=" + baseDate + "&toDate=" + baseDate);
        model.addAttribute("urlLast7", "/admin/dashboard?fromDate=" + baseDate.minusDays(7) + "&toDate=" + baseDate);
        model.addAttribute("urlLast30", "/admin/dashboard?fromDate=" + baseDate.minusDays(30) + "&toDate=" + baseDate);
        model.addAttribute("urlLast90", "/admin/dashboard?fromDate=" + baseDate.minusDays(90) + "&toDate=" + baseDate);

        model.addAttribute("categoryStats", commissionStatisticsService.getByCategory());

        return "admin/dashboard";
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class RecentActivityItem {
        private final String type;
        private final String title;
        private final LocalDateTime at;
        private final String linkUrl;
    }

    @GetMapping("/favicon.ico")
    public String favicon() { return "forward:/"; }

    @GetMapping("/.well-known/**")
    public void wellKnown(HttpServletResponse response) { response.setStatus(HttpServletResponse.SC_NOT_FOUND); }
}