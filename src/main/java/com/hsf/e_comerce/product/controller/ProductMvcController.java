package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.product.dto.response.CategoryResponse;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.dto.response.ProductVariantResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.dto.response.ReviewSummaryDTO;
import com.hsf.e_comerce.review.service.ReviewService;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductMvcController {

    private final ProductService productService;
    private final ReviewService reviewService;

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
        List<CategoryResponse> categories = productService.findAllCategory();
        model.addAttribute("categories", categories);
        
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int reviewPage,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasImages,
            @RequestParam(required = false) Boolean hasComments,
            @RequestParam(defaultValue = "newest") String sortBy,
            @CurrentUser User currentUser,
            Model model) {
        try {
            // 1. Load Product
            ProductResponse product = productService.getPublishedProductById(id);
            model.addAttribute("product", product);

            // 2. Load Reviews (Danh sách review bên dưới)
            Page<ReviewResponse> reviews = reviewService.getProductReviews(
                    id, reviewPage, 5, rating, hasImages, sortBy, currentUser
            );
            model.addAttribute("reviews", reviews);

            // 3. Load Thống Kê (Review Summary)
            ReviewSummaryDTO reviewSummary = reviewService.getReviewSummary(id);
            model.addAttribute("reviewSummary", reviewSummary);

            // Map thêm avgRating riêng lẻ để tránh lỗi nếu view cũ còn dùng
            model.addAttribute("avgRating", reviewSummary.getAverageRating());

            // 4. Truyền lại tham số lọc để highlight nút bấm
            model.addAttribute("currentRating", rating);
            model.addAttribute("currentHasImages", hasImages);
            model.addAttribute("currentHasComments", hasComments);
            model.addAttribute("currentSort", sortBy);

            // Biến currentUser để check quyền báo cáo
            model.addAttribute("currentUser", currentUser);

            // 5. Variants
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                Map<String, List<ProductVariantResponse>> variantGroups =
                        product.getVariants().stream()
                                .collect(Collectors.groupingBy(ProductVariantResponse::getName));
                model.addAttribute("variantGroups", variantGroups);
            }

            return "products/detail";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Không tìm thấy sản phẩm");
            return "redirect:/products";
        }
    }
}