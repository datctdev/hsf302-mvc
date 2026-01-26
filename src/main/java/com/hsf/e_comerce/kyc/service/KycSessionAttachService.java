package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.valueobject.AttachDecision;

import java.util.UUID;

public interface KycSessionAttachService {

    AttachDecision attachFile(UUID sessionId, UUID accountId, String fileHash, String classifyName);
}
