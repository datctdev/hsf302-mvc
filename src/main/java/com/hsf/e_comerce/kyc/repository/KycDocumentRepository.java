package com.hsf.e_comerce.kyc.repository;

import com.hsf.e_comerce.kyc.entity.EKycDocument;
import com.hsf.e_comerce.kyc.valueobject.KycDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<EKycDocument, UUID> {
    Optional<EKycDocument> findBySessionIdAndType(
            UUID sessionId,
            KycDocumentType type
    );
}
