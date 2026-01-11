package com.hsf.e_comerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRequestResponse {

    private UUID id;
    private UUID userId;
    private String shopName;
    private String shopDescription;
    private String shopPhone;
    private String shopAddress;
    private String logoUrl;
    private String coverImageUrl;
    private String status;
    private String rejectionReason;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
