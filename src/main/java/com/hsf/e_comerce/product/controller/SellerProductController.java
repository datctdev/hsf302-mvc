package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.dto.response.MessageResponse;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.product.dto.request.CreateProductRequest;
import com.hsf.e_comerce.product.dto.request.UpdateProductRequest;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerProductController {

    private final ProductService productService;
    private final ShopRepository shopRepository;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @CurrentUser User user,
            @Valid @RequestBody CreateProductRequest request) {
        
        UUID shopId = getShopIdByUser(user);
        ProductResponse response = productService.createProduct(shopId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @CurrentUser User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        
        UUID shopId = getShopIdByUser(user);
        ProductResponse response = productService.updateProduct(shopId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(
            @CurrentUser User user,
            @PathVariable UUID id) {
        
        UUID shopId = getShopIdByUser(user);
        productService.deleteProduct(shopId, id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Xóa sản phẩm thành công")
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @CurrentUser User user,
            @RequestParam(required = false) String status) {
        
        UUID shopId = getShopIdByUser(user);
        
        List<ProductResponse> products;
        if (status != null && !status.isEmpty()) {
            products = productService.getProductsByShopAndStatus(shopId, status);
        } else {
            products = productService.getAllProductsByShop(shopId);
        }
        
        return ResponseEntity.ok(products);
    }

    private UUID getShopIdByUser(User user) {
        return shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException("Bạn chưa có shop. Vui lòng đăng ký làm seller trước."))
                .getId();
    }
}
