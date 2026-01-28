package com.hsf.e_comerce.review.entity;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "review_reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"review_id", "reporter_user_id"}),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ReviewReport {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id")
    private User reporter; // nullable

    @Column(name = "reporter_ip", length = 45)
    private String reporterIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReportStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        status = ReviewReportStatus.PENDING;
    }
}
