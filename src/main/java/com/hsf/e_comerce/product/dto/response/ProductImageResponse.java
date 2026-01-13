package com.hsf.e_comerce.product.dto.response;

import com.hsf.e_comerce.product.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private UUID id;
    private String imageUrl;
    private Boolean isThumbnail;
    private Integer displayOrder;
    private LocalDateTime createdAt;

    public static ProductImageResponse convertToResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(image.getIsThumbnail())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
