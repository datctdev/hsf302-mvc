package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
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
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
        LocalDate today = LocalDate.now();
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("urlLast7", "/admin/dashboard?fromDate=" + today.minusDays(7) + "&toDate=" + today);
        model.addAttribute("urlLast30", "/admin/dashboard?fromDate=" + today.minusDays(30) + "&toDate=" + today);
        model.addAttribute("urlLast90", "/admin/dashboard?fromDate=" + today.minusDays(90) + "&toDate=" + today);
        model.addAttribute("urlAll", "/admin/dashboard");
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            BigDecimal totalRevenueShops = BigDecimal.ZERO;
            BigDecimal totalCommission = BigDecimal.ZERO;
            for (OrderResponse o : orders) {
                if (!REVENUE_STATUSES.contains(o.getStatus())) continue;
                LocalDateTime createdAt = o.getCreatedAt();
                if (createdAt != null) {
                    LocalDate d = createdAt.toLocalDate();
                    if (fromDate != null && d.isBefore(fromDate)) continue;
                    if (toDate != null && d.isAfter(toDate)) continue;
                }
                BigDecimal tot = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
                BigDecimal ship = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                totalRevenueShops = totalRevenueShops.add(tot.subtract(ship));
                if (o.getPlatformCommission() != null) {
                    totalCommission = totalCommission.add(o.getPlatformCommission());
                }
            }
            model.addAttribute("totalRevenueShops", totalRevenueShops);
            model.addAttribute("totalCommission", totalCommission);
        } catch (Exception e) {
            model.addAttribute("totalRevenueShops", BigDecimal.ZERO);
            model.addAttribute("totalCommission", BigDecimal.ZERO);
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