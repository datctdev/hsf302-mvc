package com.hsf.e_comerce.admin.controller;

import com.hsf.e_comerce.order.dto.response.ShopRankingItem;
import com.hsf.e_comerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Trang bảng xếp hạng các shop trên nền tảng (admin).
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShopRankingController {

    private final OrderService orderService;

    @GetMapping("/shop-ranking")
    public String shopRanking(Model model) {
        List<ShopRankingItem> ranking = orderService.getShopRanking();
        model.addAttribute("shopRanking", ranking);
        return "admin/shop-ranking";
    }
}
