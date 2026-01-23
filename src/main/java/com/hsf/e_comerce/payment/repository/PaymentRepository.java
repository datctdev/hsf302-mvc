package com.hsf.e_comerce.payment.repository;

import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    long countByStatus(PaymentStatus status);

    Payment save(Payment payment);

    Optional<Payment> findByOrder(Order order);
}
