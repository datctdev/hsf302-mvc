package com.hsf.e_comerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {

    private UUID id; // For update, null for create

    @NotBlank(message = "URL ảnh không được để trống")
    @Size(max = 255, message = "URL ảnh không được vượt quá 255 ký tự")
    private String imageUrl;

    private Boolean isThumbnail = false;

    private Integer displayOrder = 0;
}
