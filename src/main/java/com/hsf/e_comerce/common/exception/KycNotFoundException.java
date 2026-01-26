package com.hsf.e_comerce.common.exception;

public class KycNotFoundException extends RuntimeException {
    public KycNotFoundException(String sessionId, String accountId) {
        super("KYC session not found: " + sessionId + " for account: " + accountId);
    }
}
