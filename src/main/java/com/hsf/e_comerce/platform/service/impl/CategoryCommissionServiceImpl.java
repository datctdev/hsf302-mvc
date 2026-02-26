package com.hsf.e_comerce.platform.service.impl;

import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.platform.dto.response.CategoryCommissionResponse;
import com.hsf.e_comerce.platform.entity.CategoryCommission;
import com.hsf.e_comerce.platform.repository.CategoryCommissionRepository;
import com.hsf.e_comerce.platform.service.CategoryCommissionService;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryCommissionServiceImpl implements CategoryCommissionService {

    private final CategoryCommissionRepository repository;
    private final ProductCategoryRepository categoryRepository;
    private final PlatformSettingService platformSettingService;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCommissionByCategory(UUID categoryId) {
        return repository.findByCategoryId(categoryId)
                .map(CategoryCommission::getCommissionRate)
                .orElse(platformSettingService.getCommissionRate()); // fallback global
    }

    @Override
    @Transactional
    public void setCommissionForCategory(UUID categoryId, BigDecimal rate) {

        if (rate.compareTo(BigDecimal.ZERO) < 0 ||
                rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CustomException("Commission phải từ 0 đến 100.");
        }

        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException("Category không tồn tại."));

        CategoryCommission commission = repository.findByCategoryId(categoryId)
                .orElse(CategoryCommission.builder()
                        .category(category)
                        .build());

        commission.setCommissionRate(rate);
        repository.save(commission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryCommissionResponse> getAllCategoryCommissions() {

        BigDecimal defaultRate = platformSettingService.getCommissionRate();

        return categoryRepository.findAll().stream()
                .map(category -> {

                    BigDecimal rate = repository.findByCategoryId(category.getId())
                            .map(CategoryCommission::getCommissionRate)
                            .orElse(defaultRate);

                    return CategoryCommissionResponse.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .commissionRate(rate)
                            .build();
                })
                .toList();
    }
}
