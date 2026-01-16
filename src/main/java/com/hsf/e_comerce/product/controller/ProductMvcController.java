package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import com.hsf.e_comerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductMvcController {

    private final ProductService productService;
    private final ProductCategoryRepository categoryRepository;

    @GetMapping
    public String getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            Model model) {
        
        Page<ProductResponse> products = productService.getPublishedProducts(
                page, size, search, categoryId, shopId, minPrice, maxPrice, sortBy, sortDir
        );
        
        model.addAttribute("products", products.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("shopId", shopId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        // Load categories
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .parentId(category.getParent() != null ? category.getParent().getId() : null)
                        .build())
                .collect(Collectors.toList());
        model.addAttribute("categories", categories);
        
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable UUID id, Model model) {
        try {
            ProductResponse product = productService.getPublishedProductById(id);
            model.addAttribute("product", product);
            return "products/detail";
        } catch (Exception e) {
            model.addAttribute("error", "Không tìm thấy sản phẩm");
            return "redirect:/products";
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CategoryResponse {
        private UUID id;
        private String name;
        private UUID parentId;
    }
}
