package com.hsf.e_comerce.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private UUID id; // For update, null for create

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 100, message = "Tên thuộc tính không được vượt quá 100 ký tự")
    private String name; // e.g., "Color", "Size"

    @NotBlank(message = "Giá trị thuộc tính không được để trống")
    @Size(max = 100, message = "Giá trị thuộc tính không được vượt quá 100 ký tự")
    private String value; // e.g., "Red", "XL"

    @Digits(integer = 10, fraction = 2, message = "Giá chênh lệch không hợp lệ")
    private BigDecimal priceModifier = BigDecimal.ZERO;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải >= 0")
    private Integer stockQuantity;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
    private String sku;
}
