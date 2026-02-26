package com.hsf.e_comerce.platform.service;

import com.hsf.e_comerce.platform.dto.response.CommissionByCategoryResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionByMonthResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionOverviewResponse;
import com.hsf.e_comerce.platform.dto.response.TopSellerCommissionResponse;

import java.util.List;

public interface CommissionStatisticsService {

    CommissionOverviewResponse getOverview();

    List<CommissionByMonthResponse> getByMonth();

    List<CommissionByCategoryResponse> getByCategory();

    List<TopSellerCommissionResponse> getTopSellers(int limit);
}
