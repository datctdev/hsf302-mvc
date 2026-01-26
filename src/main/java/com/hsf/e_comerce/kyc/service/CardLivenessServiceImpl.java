package com.hsf.e_comerce.kyc.service;

import com.hsf.e_comerce.kyc.dto.response.CardLivenessResponse;
import com.hsf.e_comerce.kyc.usecase.VNPTClient;
import com.hsf.e_comerce.kyc.valueobject.CardLivenessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardLivenessServiceImpl implements CardLivenessService {
    private final VNPTClient vnpt;

    @Override
    public CardLivenessResult verify(String fileHash, String clientSession) {
        CardLivenessResponse live = vnpt.cardLiveness(fileHash, clientSession);

        String lv = (live != null && live.getObject() != null) ? live.getObject().getLiveness() : null;
        String msg = (live != null && live.getObject() != null) ? live.getObject().getLivenessMsg() : null;

        boolean isReal = "success".equalsIgnoreCase(lv);
        return new CardLivenessResult(isReal, lv, msg);
    }
}
