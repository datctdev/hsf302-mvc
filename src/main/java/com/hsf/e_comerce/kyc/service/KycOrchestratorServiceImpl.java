package com.hsf.e_comerce.kyc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.common.exception.KycNotFoundException;
import com.hsf.e_comerce.kyc.dto.response.*;
import com.hsf.e_comerce.kyc.entity.EKycDocument;
import com.hsf.e_comerce.kyc.entity.EKycSession;
import com.hsf.e_comerce.kyc.repository.KycDocumentRepository;
import com.hsf.e_comerce.kyc.repository.KycSessionRepository;
import com.hsf.e_comerce.kyc.usecase.VNPTClient;
import com.hsf.e_comerce.kyc.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycOrchestratorServiceImpl implements KycOrchestratorService {

    private final KycSessionRepository sessionRepository;
    private final KycDocumentRepository documentRepository;
    private final VNPTClient vnptClient;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    private final UploadFileService uploadFileService;
    private final CardClassifyService cardClassifyService;
    private final CardLivenessService cardLivenessService;
    // private final FaceLivenessService faceLivenessService; // KHÔNG CẦN - BỎ QUA FACE LIVENESS CHECK
    private final KycSessionAttachService sessionAttachService;

    @Override
    @Transactional
    public UUID startSession(UUID userId) {
        EKycSession session = new EKycSession();
        session.setAccountId(userId);
        session.setStatus(KycStatus.DRAFT);
        session = sessionRepository.save(session);
        return session.getId();
    }

    @Override
    public EKycSession getSession(UUID sessionId, UUID userId) {
        return sessionRepository.findByIdAndAccountId(sessionId, userId)
                .orElseThrow(() -> new KycNotFoundException(sessionId.toString(), userId.toString()));
    }

    @Override
    public EKycSession getLatestSessionByUser(UUID userId) {
        // Find latest session - implement in repository if needed
        return sessionRepository.findByIdAndAccountId(userId, userId).orElse(null);
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFileAndAttach(
            UUID sessionId,
            UUID userId,
            MultipartFile file,
            String title,
            String description) {
        
        try {
            // 1. Upload to VNPT
            String hash = uploadFileService.upload(file, title, description);
            if (hash == null || hash.isBlank()) {
                return createErrorResult("UPLOAD", hash, "Empty hash from upload");
            }

            // 2. Classify document
            ClassifyResult classifyResult = cardClassifyService.classify(hash, sessionId.toString());
            if (classifyResult == null || classifyResult.name() == null || classifyResult.name().isBlank()) {
                return createErrorResult("CLASSIFY", hash, "Classify returned empty name");
            }

            Integer type = classifyResult.type();
            String name = classifyResult.name();

            // 3. Liveness check based on type
            if (type != null && (type >= 0 && type <= 3)) {
                // Card liveness - CHỈ CHECK CHO CCCD
                CardLivenessResult livenessResult = cardLivenessService.verify(hash, sessionId.toString());
                if (livenessResult == null || !livenessResult.isReal()) {
                    return createLivenessErrorResult("CARD_LIVENESS", hash, name, type, 
                            livenessResult != null ? livenessResult.liveness() : "",
                            livenessResult != null ? livenessResult.livenessMsg() : "");
                }
            } else if (type != null && type == 4) {
                // Face/Selfie - Skip liveness check, only upload
            } else {
                return createErrorResult("ROUTE_UNSUPPORTED", hash, 
                        "Unsupported classify type: " + type);
            }

            // 4. Attach to session
            AttachDecision decision = sessionAttachService.attachFile(sessionId, userId, hash, name);
            if (decision == null || !decision.attached()) {
                return createErrorResult("ATTACH", hash, 
                        decision != null ? decision.reason() : "ATTACH_DECISION_NULL");
            }

            // 5. Success
            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            result.put("fileHash", hash);
            result.put("classifiedName", name);
            result.put("classifiedType", type);
            result.put("classifiedConfidence", classifyResult.confidence());
            result.put("savedTo", decision.savedTo());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error in uploadFileAndAttach", e);
            return createErrorResult("EXCEPTION", "", e.getMessage());
        }
    }

    @Override
    @Transactional
    public String uploadDocument(
            UUID sessionId,
            UUID userId,
            KycDocumentType type,
            MultipartFile file,
            String title,
            String description) {
        
        // Upload to VNPT
        UploadResponse uploadResponse = vnptClient.addFile(file, title, description);
        String hash = uploadResponse.getObject().getHash();
        
        // Attach to session
        EKycSession session = getSession(sessionId, userId);
        
        switch (type) {
            case FRONT:
                session.setFrontHash(hash);
                break;
            case BACK:
                session.setBackHash(hash);
                break;
            case SELFIE:
                session.setSelfieHash(hash);
                break;
            case OTHER:
                // OTHER type not handled in this flow
                break;
        }
        
        session.setStatus(KycStatus.IN_PROGRESS);
        sessionRepository.save(session);
        
        // Save document record
        EKycDocument document = new EKycDocument();
        document.setSessionId(sessionId);
        document.setType(type);
        document.setFileHash(hash);
        documentRepository.save(document);
        return hash;
    }

    @Override
    public Map<String, Object> extractFrontInfo(UUID sessionId, UUID userId) {
        EKycSession session = getSession(sessionId, userId);
        
        if (session.getFrontHash() == null || session.getFrontHash().isBlank()) {
            throw new IllegalStateException("No front document uploaded");
        }
        
        OcrFrontResponse response = vnptClient.ocrFront(session.getFrontHash(), -1, sessionId.toString());
        OcrFrontResponse.Obj obj = response.getObj();
        
        Map<String, Object> result = new HashMap<>();
        if (obj != null) {
            result.put("idNumber", obj.getId());
            result.put("fullName", obj.getName());
            result.put("birthDay", obj.getBirthDay());
            result.put("gender", obj.getGender());
            result.put("nationality", obj.getNationality());
            result.put("originLocation", obj.getOriginLocation());
            result.put("recentLocation", obj.getRecentLocation());
            result.put("issueDate", obj.getIssueDate());
            result.put("issuePlace", obj.getIssuePlace());
            result.put("validDate", obj.getValidDate());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> extractBackInfo(UUID sessionId, UUID userId) {
        EKycSession session = getSession(sessionId, userId);
        
        if (session.getBackHash() == null || session.getBackHash().isBlank()) {
            throw new IllegalStateException("No back document uploaded");
        }
        
        OcrBackResponse response = vnptClient.ocrBack(session.getBackHash(), -1, sessionId.toString());
        OcrBackResponse.Obj obj = response.getObj();
        
        Map<String, Object> result = new HashMap<>();
        if (obj != null) {
            result.put("issueDate", obj.getIssueDate());
            result.put("issuePlace", obj.getIssuePlace());
        }
        
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> compareAndVerify(UUID sessionId, UUID userId) {
        EKycSession session = getSession(sessionId, userId);
        
        if (session.getFrontHash() == null || session.getFrontHash().isBlank()) {
            throw new IllegalStateException("Front document not uploaded");
        }
        if (session.getSelfieHash() == null || session.getSelfieHash().isBlank()) {
            throw new IllegalStateException("Selfie not uploaded");
        }
        
        // Compare faces
        CompareResponse response = vnptClient.compare(
                session.getFrontHash(), 
                session.getSelfieHash(), 
                sessionId.toString()
        );
        
        CompareResponse.Obj obj = response.getObj();
        String msg = obj != null ? obj.getMsg() : null;
        Double prob = obj != null && obj.getProb() != null ? obj.getProb() : 0.0;
        
        boolean isMatch = "MATCH".equalsIgnoreCase(msg);
        boolean verified = isMatch && prob >= 95.0;
        
        // Update session status
        session.setStatus(verified ? KycStatus.VERIFIED : KycStatus.REJECTED);
        session.setProviderTrace(toJsonSafe(response));
        sessionRepository.save(session);
        
        // Update user verification status if verified
        if (verified) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found"));
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("isMatch", isMatch);
        result.put("matchScore", prob);
        result.put("status", session.getStatus().name());
        result.put("verified", verified);
        
        return result;
    }

    // Helper methods
    private Map<String, Object> createErrorResult(String step, String fileHash, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", false);
        result.put("step", step);
        result.put("fileHash", fileHash);
        result.put("reason", reason);
        return result;
    }

    private Map<String, Object> createLivenessErrorResult(
            String step, String fileHash, String name, Integer type, 
            String liveness, String livenessMsg) {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", false);
        result.put("step", step);
        result.put("fileHash", fileHash);
        result.put("classifiedName", name);
        result.put("classifiedType", type);
        result.put("liveness", liveness);
        result.put("livenessMsg", livenessMsg);
        return result;
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
