package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.entity.EKycSession;
import com.hsf.e_comerce.kyc.valueobject.KycDocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

public interface KycOrchestratorService {
    
    // Session management
    UUID startSession(UUID userId);
    
    EKycSession getSession(UUID sessionId, UUID userId);
    
    EKycSession getLatestSessionByUser(UUID userId);
    
    // File upload and processing
    Map<String, Object> uploadFileAndAttach(
            UUID sessionId,
            UUID userId,
            MultipartFile file,
            String title,
            String description
    );
    
    String uploadDocument(
            UUID sessionId,
            UUID userId,
            KycDocumentType type,
            MultipartFile file,
            String title,
            String description
    );
    
    // Information extraction
    Map<String, Object> extractFrontInfo(UUID sessionId, UUID userId);
    
    Map<String, Object> extractBackInfo(UUID sessionId, UUID userId);
    
    // Verification
    Map<String, Object> compareAndVerify(UUID sessionId, UUID userId);
}
