package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.entity.EKycSession;
import com.hsf.e_comerce.kyc.repository.KycSessionRepository;
import com.hsf.e_comerce.kyc.valueobject.KycStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycVerificationServiceImpl implements KycVerificationService {
    
    private final KycSessionRepository sessionRepository;
    
    @Override
    @Transactional(readOnly = true)
    public boolean isKycVerified(UUID userId) {
        return sessionRepository.hasVerifiedKyc(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<EKycSession> getVerifiedKycSession(UUID userId) {
        return sessionRepository.findLatestByAccountIdAndStatus(userId, KycStatus.VERIFIED);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<EKycSession> getLatestKycSession(UUID userId) {
        return sessionRepository.findFirstByAccountIdOrderByCreatedAtDesc(userId);
    }
}
