package com.hsf.e_comerce.recommendation.repository;

import com.hsf.e_comerce.recommendation.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    @Query("SELECT s FROM SearchHistory s WHERE (s.sessionId = :sessionId OR (s.userId = :userId AND :userId IS NOT NULL)) ORDER BY s.createdAt DESC")
    List<SearchHistory> findRecentBySessionOrUser(@Param("sessionId") String sessionId, @Param("userId") UUID userId, Pageable pageable);
}
