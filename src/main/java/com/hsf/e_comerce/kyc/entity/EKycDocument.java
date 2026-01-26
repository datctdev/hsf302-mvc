package com.hsf.e_comerce.kyc.entity;

import com.hsf.e_comerce.common.BaseEntity;
import com.hsf.e_comerce.kyc.valueobject.KycDocumentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "ekyc_document")
public class EKycDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycDocumentType type;

//    @Column(nullable = false)
//    private UUID fileId;

    private String fileHash;

}
