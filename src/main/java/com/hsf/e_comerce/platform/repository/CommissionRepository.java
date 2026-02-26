package com.hsf.e_comerce.platform.repository;

import com.hsf.e_comerce.platform.entity.Commission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommissionRepository extends
        JpaRepository<Commission, UUID>,
        JpaSpecificationExecutor<Commission> {

    Optional<Commission> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    @Query("""
    SELECT
        COALESCE(SUM(c.totalCommission), 0),
        COUNT(c),
        COALESCE(
           CASE
               WHEN SUM(c.orderAmount) = 0 THEN 0
               ELSE SUM(c.totalCommission) / SUM(c.orderAmount) * 100
           END
        , 0)
    FROM Commission c
    """)
    List<Object[]> getOverviewStatistics();

    @Query("""
    SELECT
        YEAR(c.createdAt),
        MONTH(c.createdAt),
        SUM(c.totalCommission)
    FROM Commission c
    GROUP BY YEAR(c.createdAt), MONTH(c.createdAt)
    ORDER BY YEAR(c.createdAt), MONTH(c.createdAt)
""")
    List<Object[]> getCommissionByMonth();

    @Query("""
    SELECT
        p.category.id,
        p.category.name,
        SUM(ci.commissionAmount)
    FROM CommissionItem ci
    JOIN OrderItem oi ON oi.id = ci.orderItemId
    JOIN oi.product p
    GROUP BY p.category.id, p.category.name
""")
    List<Object[]> getCommissionByCategory();

    @Query("""
    SELECT
        c.sellerId,
        SUM(c.totalCommission)
    FROM Commission c
    GROUP BY c.sellerId
    ORDER BY SUM(c.totalCommission) DESC
""")
    List<Object[]> getTopSellerCommission(Pageable pageable);
}
