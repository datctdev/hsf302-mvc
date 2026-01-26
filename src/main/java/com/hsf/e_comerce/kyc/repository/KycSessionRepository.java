package com.hsf.e_comerce.kyc.repository;

import com.hsf.e_comerce.kyc.entity.EKycSession;
import com.hsf.e_comerce.kyc.valueobject.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycSessionRepository extends JpaRepository<EKycSession, UUID> {

    Optional<EKycSession> findByIdAndAccountId(UUID id, UUID accountId);
    
    List<EKycSession> findByAccountId(UUID accountId);
    
    List<EKycSession> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    
    Optional<EKycSession> findFirstByAccountIdOrderByCreatedAtDesc(UUID accountId);
    
    boolean existsByAccountIdAndStatus(UUID accountId, KycStatus status);
    
    @Query("SELECT s FROM EKycSession s WHERE s.accountId = :accountId AND s.status = :status ORDER BY s.createdAt DESC")
    Optional<EKycSession> findLatestByAccountIdAndStatus(@Param("accountId") UUID accountId, @Param("status") KycStatus status);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM EKycSession s WHERE s.accountId = :accountId AND s.status = 'VERIFIED'")
    boolean hasVerifiedKyc(@Param("accountId") UUID accountId);
}
