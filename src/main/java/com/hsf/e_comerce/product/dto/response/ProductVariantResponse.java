package com.hsf.e_comerce.product.dto.response;

import com.hsf.e_comerce.product.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private UUID id;
    private String name;
    private String value;
    private BigDecimal priceModifier;
    private Integer stockQuantity;
    private String sku;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVariantResponse convertToResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .name(variant.getName())
                .value(variant.getValue())
                .priceModifier(variant.getPriceModifier())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }
}
