package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.valueobject.FaceLivenessResult;

public interface FaceLivenessService {
    FaceLivenessResult verify(String fileHash, String clientSession);
}
