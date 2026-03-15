package com.hsf.e_comerce.payment.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.payment.entity.BatchPayment;
import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.enums.PaymentMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PaymentService {
    Payment createPayment(Order order, PaymentMethod method);

    void confirmCOD(UUID orderId, User user);

    void handleVNPayCallback(Map<String, String> params);

    /** Get or create VNPay payment for order; throws if order invalid or not owned by user. */
    Payment getOrCreatePaymentForVNPay(UUID orderId, User user);

    /** Create batch VNPay payment for multiple orders (one payment, one redirect to VNPay). */
    BatchPayment createBatchPaymentForVNPay(List<UUID> orderIds, User user);

    Optional<UUID> getOrderIdByTransactionId(String transactionId);

    /** True if transaction is batch (redirect to /orders after callback). */
    boolean isBatchTransaction(String transactionId);
}
