package com.hsf.e_comerce.platform.service;

import com.hsf.e_comerce.platform.dto.response.CategoryCommissionResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CategoryCommissionService {

    BigDecimal getCommissionByCategory(UUID categoryId);

    void setCommissionForCategory(UUID categoryId, BigDecimal rate);

    List<CategoryCommissionResponse> getAllCategoryCommissions();
}
