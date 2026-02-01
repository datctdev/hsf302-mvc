package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.product.valueobject.ProductStatus;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductMvcController {

    private final ProductService productService;
    private final ShopService shopService;

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Model model) {

        size = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        ProductStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ProductStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // keep null => all statuses
            }
        }

        Page<Product> productPage = productService.findAllForAdmin(shopId, statusEnum, pageable);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("shops", shopService.findAllShops());
        model.addAttribute("currentShopId", shopId);
        model.addAttribute("currentStatus", status);
        model.addAttribute("productStatuses", ProductStatus.values());
        return "admin/products";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        return updateProductStatus(id, ProductStatus.ARCHIVED, redirectAttributes,
                "Đã gỡ xuất bản sản phẩm.", "Không tìm thấy sản phẩm.");
    }

    @PostMapping("/{id}/publish")
    public String publish(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        return updateProductStatus(id, ProductStatus.PUBLISHED, redirectAttributes,
                "Đã xuất bản lại sản phẩm.", "Không tìm thấy sản phẩm.");
    }

    @PostMapping("/{id}/hide")
    public String hide(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            productService.setProductDeleted(id, true);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn sản phẩm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/restore")
    public String restore(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            productService.setProductDeleted(id, false);
            redirectAttributes.addFlashAttribute("success", "Đã khôi phục sản phẩm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
        }
        return "redirect:/admin/products";
    }

    private String updateProductStatus(UUID id, ProductStatus status, RedirectAttributes redirectAttributes,
                                       String successMessage, String errorMessage) {
        try {
            productService.setProductStatus(id, status);
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
        }
        return "redirect:/admin/products";
    }
}
