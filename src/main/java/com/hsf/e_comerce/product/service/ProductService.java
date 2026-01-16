package com.hsf.e_comerce.product.service;

import com.hsf.e_comerce.product.dto.request.CreateProductRequest;
import com.hsf.e_comerce.product.dto.request.UpdateProductRequest;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    
    ProductResponse createProduct(UUID shopId, CreateProductRequest request);
    
    ProductResponse updateProduct(UUID shopId, UUID productId, UpdateProductRequest request);
    
    void deleteProduct(UUID shopId, UUID productId);
    
    ProductResponse getProductById(UUID productId);
    
    List<ProductResponse> getAllProductsByShop(UUID shopId);
    
    List<ProductResponse> getProductsByShopAndStatus(UUID shopId, String status);
    
    // Public methods
    Page<ProductResponse> getPublishedProducts(
        int page,
        int size,
        String search,
        UUID categoryId,
        UUID shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        String sortDir
    );
    
    ProductResponse getPublishedProductById(UUID productId);
    
    Page<ProductResponse> searchProducts(String keyword, int page, int size);
    
    Page<ProductResponse> getPublishedProductsByShop(UUID shopId, int page, int size);
}
