package com.hsf.e_comerce.payment.service;

import com.hsf.e_comerce.payment.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

public interface VNPayService {

    String buildPaymentUrl(Payment payment);

    /** Build VNPay URL for batch payment (amount, txnRef, orderInfo). */
    String buildPaymentUrl(BigDecimal amount, String transactionId, String orderInfo);

    boolean verifyChecksum(Map<String, String> params);
}
