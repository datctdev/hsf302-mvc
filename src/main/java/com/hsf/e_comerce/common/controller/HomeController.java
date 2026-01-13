package com.hsf.e_comerce.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String hello(Model model) {
        model.addAttribute("message", "Chào mừng đến với E-commerce!");
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/profile")
    public String profile() {
        return "auth/profile";
    }

    @GetMapping("/change-password")
    public String changePassword() {
        return "auth/change-password";
    }

    @GetMapping("/become-seller")
    public String becomeSeller() {
        return "seller/become-seller";
    }

    @GetMapping("/admin/seller-requests")
    public String adminSellerRequests() {
        return "admin/seller-requests";
    }

    @GetMapping("/seller/shop")
    public String sellerShop() {
        return "seller/shop";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "coming-soon";
    }

    @GetMapping("/admin/products")
    public String adminProducts() {
        return "coming-soon";
    }

    @GetMapping("/admin/orders")
    public String adminOrders() {
        return "coming-soon";
    }

    @GetMapping("/seller/products")
    public String sellerProducts() {
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
}
