package com.hsf.e_comerce.payment.service;

import com.hsf.e_comerce.payment.entity.Payment;

import java.util.Map;

public interface VNPayService {

    String buildPaymentUrl(Payment payment);

    boolean verifyChecksum(Map<String, String> params);
}
