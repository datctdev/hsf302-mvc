package com.hsf.e_comerce.platform.controller;

import com.hsf.e_comerce.platform.dto.request.UpdateCategoryCommissionRequest;
import com.hsf.e_comerce.platform.service.CategoryCommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/commission/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryCommissionController {

    private final CategoryCommissionService service;

    // 1️⃣ GET /admin/commission/categories
    @GetMapping
    public String listCategoryCommission(org.springframework.ui.Model model) {
        model.addAttribute("categoryCommissions", service.getAllCategoryCommissions());
        return "admin/category-commission";
    }

    // 2️⃣ POST /admin/commission/categories/{categoryId}
    @PostMapping("/{categoryId}")
    public String updateCommission(
            @PathVariable UUID categoryId,
            @Valid @ModelAttribute UpdateCategoryCommissionRequest request) {

        service.setCommissionForCategory(categoryId, request.getCommissionRate());

        return "redirect:/admin/commission/categories";
    }
}
