package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final SellerRequestService sellerRequestService;

    @GetMapping("/")
    public String hello(Model model) {
        model.addAttribute("message", "Chào mừng đến với E-commerce!");
        // currentUser is automatically added by GlobalControllerAdvice
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
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
    public String adminDashboard(Model model) {
        // Load statistics for dashboard
        try {
            long totalUsers = userService.getAllUsers().size();
            long pendingRequests = sellerRequestService.getRequestsByStatus("PENDING").size();

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("totalShops", 0); // TODO: Implement when ShopService is available
            model.addAttribute("totalOrders", 0); // TODO: Implement when OrderService is available
        } catch (Exception e) {
            // If error, set defaults
            model.addAttribute("totalUsers", 0);
            model.addAttribute("pendingRequests", 0);
            model.addAttribute("totalShops", 0);
            model.addAttribute("totalOrders", 0);
        }

        // currentUser is automatically added by GlobalControllerAdvice
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String adminProducts() {
        return "coming-soon";
    }

    @GetMapping("/admin/orders")
    public String adminOrders() {
        return "coming-soon";
    }

    @GetMapping("/seller/orders")
    public String sellerOrders() {
        return "coming-soon";
    }

    @GetMapping("/seller/statistics")
    public String sellerStatistics() {
        return "coming-soon";
    }

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