package com.hsf.e_comerce.platform.repository;

import com.hsf.e_comerce.platform.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CommissionRepository extends
        JpaRepository<Commission, UUID>,
        JpaSpecificationExecutor<Commission> {

    Optional<Commission> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
