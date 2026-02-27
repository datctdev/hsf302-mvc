package com.hsf.e_comerce.platform.service.impl;

import com.hsf.e_comerce.platform.dto.response.CommissionByCategoryResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionByMonthResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionOverviewResponse;
import com.hsf.e_comerce.platform.dto.response.TopSellerCommissionResponse;
import com.hsf.e_comerce.platform.repository.CommissionRepository;
import com.hsf.e_comerce.platform.service.CommissionStatisticsService;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommissionStatisticsServiceImpl
        implements CommissionStatisticsService {

    private final CommissionRepository commissionRepository;
    private final ShopService shopService;

    @Override
    public CommissionOverviewResponse getOverview() {

        List<Object[]> results = commissionRepository.getOverviewStatistics();

        if (results.isEmpty()) {
            return CommissionOverviewResponse.builder()
                    .totalCommission(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .averageCommissionRate(BigDecimal.ZERO)
                    .build();
        }

        Object[] result = results.get(0);

        if (result == null || result.length < 3) {
            return CommissionOverviewResponse.builder()
                    .totalCommission(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .averageCommissionRate(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal totalCommission = (BigDecimal) result[0];
        Long totalOrders = ((Number) result[1]).longValue();
        BigDecimal avgRate = (BigDecimal) result[2];

        return CommissionOverviewResponse.builder()
                .totalCommission(totalCommission)
                .totalOrders(totalOrders)
                .averageCommissionRate(avgRate)
                .build();
    }

    @Override
    public List<CommissionByMonthResponse> getByMonth() {

        return commissionRepository.getCommissionByMonth()
                .stream()
                .map(r -> CommissionByMonthResponse.builder()
                        .year((Integer) r[0])
                        .month((Integer) r[1])
                        .totalCommission((BigDecimal) r[2])
                        .build())
                .toList();
    }

    @Override
    public List<CommissionByCategoryResponse> getByCategory() {

        return commissionRepository.getCommissionByCategory()
                .stream()
                .map(r -> CommissionByCategoryResponse.builder()
                        .categoryId((UUID) r[0])
                        .categoryName((String) r[1])
                        .totalCommission((BigDecimal) r[2])
                        .build())
                .toList();
    }

    @Override
    public List<TopSellerCommissionResponse> getTopSellers(int limit) {

        return commissionRepository
                .getTopSellerCommission(PageRequest.of(0, limit))
                .stream()
                .map(r -> {
                    UUID sellerId = (UUID) r[0];
                    return TopSellerCommissionResponse.builder()
                            .sellerId(sellerId)
                            .shopName(shopService.findByUserId(sellerId))
                            .totalCommission((BigDecimal) r[1])
                            .build();
                })
                .toList();
    }
}
