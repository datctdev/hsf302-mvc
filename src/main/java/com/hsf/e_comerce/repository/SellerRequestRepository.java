package com.hsf.e_comerce.repository;

import com.hsf.e_comerce.entity.SellerRequest;
import com.hsf.e_comerce.valueobject.SellerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRequestRepository extends JpaRepository<SellerRequest, UUID> {
    
    Optional<SellerRequest> findByUserId(UUID userId);
    
    Optional<SellerRequest> findByUserIdAndStatus(UUID userId, SellerRequestStatus status);
    
    List<SellerRequest> findByStatusOrderByCreatedAtDesc(SellerRequestStatus status);
    
    List<SellerRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT sr FROM SellerRequest sr WHERE sr.user.id = :userId AND sr.status = :status")
    Optional<SellerRequest> findByUserIdAndStatusEnum(UUID userId, SellerRequestStatus status);
    
    default Optional<SellerRequest> findPendingRequestByUserId(UUID userId) {
        return findByUserIdAndStatusEnum(userId, SellerRequestStatus.PENDING);
    }
    
    boolean existsByUserIdAndStatus(UUID userId, SellerRequestStatus status);
}
