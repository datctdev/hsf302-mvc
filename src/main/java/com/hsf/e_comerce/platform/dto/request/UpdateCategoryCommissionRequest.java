package com.hsf.e_comerce.platform.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryCommissionRequest {

    @NotNull(message = "Commission không được để trống.")
    @DecimalMin(value = "0", message = "Commission phải từ 0%.")
    @DecimalMax(value = "100", message = "Commission không vượt quá 100%.")
    private BigDecimal commissionRate;
}
