package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.valueobject.ClassifyResult;

public interface CardClassifyService {

    ClassifyResult classify(String fileHash, String clientSession);
}
