package com.hsf.e_comerce.product.dto.response;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductCategoryMappingRepository;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.product.repository.ProductVariantRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private String name;
    private String description;
    private String sku;
    private String status;
    private BigDecimal basePrice;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
    private UUID categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse convertToResponse(
            Product product,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            ProductCategoryMappingRepository categoryMappingRepository) {
        
        List<ProductVariantResponse> variants = variantRepository.findByProduct(product).stream()
                .map(ProductVariantResponse::convertToResponse)
                .collect(Collectors.toList());

        List<ProductImageResponse> images = imageRepository.findByProductOrderByDisplayOrderAsc(product).stream()
                .map(ProductImageResponse::convertToResponse)
                .collect(Collectors.toList());

        UUID categoryId = null;
        String categoryName = null;
        var categoryMapping = categoryMappingRepository.findByProduct(product).stream()
                .findFirst();
        if (categoryMapping.isPresent()) {
            ProductCategory category = categoryMapping.get().getCategory();
            categoryId = category.getId();
            categoryName = category.getName();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .status(product.getStatus().name())
                .basePrice(product.getBasePrice())
                .variants(variants)
                .images(images)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
