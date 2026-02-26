package com.hsf.e_comerce.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CommissionByMonthResponse {

    private Integer year;
    private Integer month;
    private BigDecimal totalCommission;
}