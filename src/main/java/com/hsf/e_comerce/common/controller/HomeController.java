package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.common.dto.response.AdminDashboardData;
import com.hsf.e_comerce.common.service.AdminDashboardService;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.recommendation.service.RecommendationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final RecommendationService recommendationService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/")
    public String hello(HttpSession session, @CurrentUser User currentUser, Model model) {
        model.addAttribute("slogan", "Mua sắm thông minh – Giá tốt mỗi ngày");
        model.addAttribute("sloganSubtext", "Khám phá hàng ngàn sản phẩm điện tử, công nghệ từ các shop uy tín. Giao hàng nhanh, bảo hành chính hãng.");
        try {
            List<ProductResponse> recommended = recommendationService.getRecommendationsForUser(
                    session != null ? session.getId() : null, currentUser != null ? currentUser.getId() : null, 8);
            model.addAttribute("recommendedProducts", recommended);
        } catch (Exception e) {
            model.addAttribute("recommendedProducts", List.<ProductResponse>of());
        }
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
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate toDate,
            Model model) {
        AdminDashboardData data = adminDashboardService.getDashboardData(fromDate, toDate);

        model.addAttribute("trendLabels", data.getTrendLabels());
        model.addAttribute("trendRevenue", data.getTrendRevenue());
        model.addAttribute("trendCommission", data.getTrendCommission());
        model.addAttribute("trendOrderCounts", data.getTrendOrderCounts());
        model.addAttribute("trendUsers", data.getTrendUsers());
        model.addAttribute("countDelivered", data.getCountDelivered());
        model.addAttribute("countConfirmed", data.getCountConfirmed());
        model.addAttribute("countProcessing", data.getCountProcessing());
        model.addAttribute("countShipping", data.getCountShipping());
        model.addAttribute("countPendingPayment", data.getCountPendingPayment());
        model.addAttribute("countCancelled", data.getCountCancelled());
        model.addAttribute("aovThisMonth", data.getAovThisMonth());
        model.addAttribute("revenueThisMonth", data.getRevenueThisMonth());
        model.addAttribute("revenueGrowth", data.getRevenueGrowth());
        model.addAttribute("ordersThisMonth", data.getOrdersThisMonth());
        model.addAttribute("commissionThisMonth", data.getCommissionThisMonth());
        model.addAttribute("commissionGrowth", data.getCommissionGrowth());
        model.addAttribute("top5Shops", data.getTop5Shops());
        model.addAttribute("fastestGrowthShop", data.getFastestGrowthShop());
        model.addAttribute("topBuyers", data.getTopBuyers());
        model.addAttribute("recentOrders", data.getRecentOrders());
        model.addAttribute("totalUsers", data.getTotalUsers());
        model.addAttribute("totalShops", data.getTotalShops());
        model.addAttribute("fromDate", data.getFromDate());
        model.addAttribute("toDate", data.getToDate());
        model.addAttribute("urlLast1", data.getUrlLast1());
        model.addAttribute("urlLast7", data.getUrlLast7());
        model.addAttribute("urlLast30", data.getUrlLast30());
        model.addAttribute("urlLast90", data.getUrlLast90());
        model.addAttribute("categoryStats", data.getCategoryStats());

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