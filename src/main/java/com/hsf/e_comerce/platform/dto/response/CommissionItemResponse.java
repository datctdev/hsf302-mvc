package com.hsf.e_comerce.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CommissionItemResponse {

    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
}
