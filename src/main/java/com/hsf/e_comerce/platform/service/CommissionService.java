package com.hsf.e_comerce.platform.service;

import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
import com.hsf.e_comerce.platform.dto.response.CommissionDetailResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CommissionService {

    List<CommissionResponse> getCommissions(CommissionFilterRequest filter);

    void createCommission(Order order);

    CommissionDetailResponse getByOrderId(UUID orderId);
}
