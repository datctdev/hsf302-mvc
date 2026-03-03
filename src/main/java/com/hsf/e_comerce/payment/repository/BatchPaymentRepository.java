package com.hsf.e_comerce.payment.repository;

import com.hsf.e_comerce.payment.entity.BatchPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchPaymentRepository extends JpaRepository<BatchPayment, UUID> {

    Optional<BatchPayment> findByTransactionId(String transactionId);

    @Query("SELECT b FROM BatchPayment b LEFT JOIN FETCH b.orders o LEFT JOIN FETCH o.order WHERE b.id = :id")
    Optional<BatchPayment> findByIdWithOrders(@Param("id") UUID id);

    @Query("SELECT b FROM BatchPayment b LEFT JOIN FETCH b.orders o LEFT JOIN FETCH o.order WHERE b.transactionId = :txnRef")
    Optional<BatchPayment> findByTransactionIdWithOrders(@Param("txnRef") String txnRef);
}
