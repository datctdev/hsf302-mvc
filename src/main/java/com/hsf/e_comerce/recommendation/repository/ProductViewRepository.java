package com.hsf.e_comerce.recommendation.repository;

import com.hsf.e_comerce.recommendation.entity.ProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductViewRepository extends JpaRepository<ProductView, UUID> {

    @Query("SELECT v FROM ProductView v WHERE (v.sessionId = :sessionId OR (v.userId = :userId AND :userId IS NOT NULL)) ORDER BY v.viewedAt DESC")
    List<ProductView> findRecentBySessionOrUser(@Param("sessionId") String sessionId, @Param("userId") UUID userId, Pageable pageable);

}
