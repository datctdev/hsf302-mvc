package com.hsf.e_comerce.platform.repository;

import com.hsf.e_comerce.platform.entity.CategoryCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryCommissionRepository extends JpaRepository<CategoryCommission, UUID> {

    Optional<CategoryCommission> findByCategoryId(UUID categoryId);
}
