package com.hsf.e_comerce.seller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class SellerIncomeSummaryResponse {

    private BigDecimal totalCommission;
    private BigDecimal totalNetIncome;
}
