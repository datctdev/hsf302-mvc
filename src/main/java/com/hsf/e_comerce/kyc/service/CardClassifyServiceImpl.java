package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.dto.response.ClassifyResponse;
import com.hsf.e_comerce.kyc.usecase.VNPTClient;
import com.hsf.e_comerce.kyc.valueobject.ClassifyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardClassifyServiceImpl implements CardClassifyService {
    private final VNPTClient vnpt;

    @Override
    public ClassifyResult classify(String fileHash, String clientSession) {
        ClassifyResponse cls = vnpt.classify(fileHash, clientSession);

        String name = (cls != null && cls.getObj() != null) ? cls.getObj().getName() : null;
        Integer type = (cls != null && cls.getObj() != null) ? cls.getObj().getType() : null;
        Double conf = (cls != null && cls.getObj() != null) ? cls.getObj().getConfidence() : null;

        return new ClassifyResult(name, type, conf);
    }
}
