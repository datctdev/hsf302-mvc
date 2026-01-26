package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.entity.EKycSession;

import java.util.Optional;
import java.util.UUID;

public interface KycVerificationService {
    
    /**
     * Check if user has completed KYC verification
     * @param userId User ID to check
     * @return true if user has a VERIFIED KYC session
     */
    boolean isKycVerified(UUID userId);
    
    /**
     * Get the latest verified KYC session for a user
     * @param userId User ID
     * @return Optional containing the latest verified KYC session
     */
    Optional<EKycSession> getVerifiedKycSession(UUID userId);
    
    /**
     * Get the latest KYC session for a user (any status)
     * @param userId User ID
     * @return Optional containing the latest KYC session
     */
    Optional<EKycSession> getLatestKycSession(UUID userId);
}
