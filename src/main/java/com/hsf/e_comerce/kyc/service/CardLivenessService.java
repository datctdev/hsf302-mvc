package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.valueobject.CardLivenessResult;

public interface CardLivenessService {
    CardLivenessResult verify(String fileHash, String clientSession);
}
