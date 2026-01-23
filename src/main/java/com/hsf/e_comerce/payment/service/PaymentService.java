package com.hsf.e_comerce.payment.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    Payment createPayment(Order order, PaymentMethod method);

    void confirmCOD(UUID orderId, User user);

    void handleVNPayCallback(Map<String, String> params);
}
