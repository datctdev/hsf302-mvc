package com.hsf.e_comerce.payment.entity;

import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.payment.enums.PaymentMethod;
import com.hsf.e_comerce.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * 1 Order = 1 Payment
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Mã giao dịch do hệ thống sinh ra (vnp_TxnRef)
     */
    @Column(name = "transaction_id")
    private String transactionId;

    /**
     * Mã giao dịch từ cổng thanh toán (vnp_TransactionNo)
     */
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    /**
     * Raw callback data (JSON / query string)
     */
    @Lob
    @Column(name = "gateway_response")
    private String gatewayResponse;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
