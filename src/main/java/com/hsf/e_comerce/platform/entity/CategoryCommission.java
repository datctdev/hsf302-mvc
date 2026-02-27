package com.hsf.e_comerce.platform.entity;

import com.hsf.e_comerce.product.entity.ProductCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "category_commissions",
        uniqueConstraints = @UniqueConstraint(columnNames = "category_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CategoryCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private ProductCategory category;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate; // ví dụ: 12.5 (%)

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}