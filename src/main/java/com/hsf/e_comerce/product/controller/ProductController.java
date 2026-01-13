package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import com.hsf.e_comerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        
        Page<ProductResponse> products = productService.getPublishedProducts(
                page, size, search, categoryId, shopId, minPrice, maxPrice, sortBy, sortDir
        );
        
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getPublishedProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<ProductResponse> products = productService.searchProducts(q, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/shops/{shopId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByShop(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<ProductResponse> products = productService.getPublishedProductsByShop(shopId, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .parentId(category.getParent() != null ? category.getParent().getId() : null)
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(categories);
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
