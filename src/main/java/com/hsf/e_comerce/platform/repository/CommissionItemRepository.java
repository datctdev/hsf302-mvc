package com.hsf.e_comerce.platform.repository;

import com.hsf.e_comerce.platform.entity.CommissionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommissionItemRepository
        extends JpaRepository<CommissionItem, UUID> {

    List<CommissionItem> findByCommissionId(UUID commissionId);
}