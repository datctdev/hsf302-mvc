package com.hsf.e_comerce.kyc.valueobject;

public record FaceLivenessResult(boolean isLive,
                                 String liveness,
                                 String livenessMsg) {
}
