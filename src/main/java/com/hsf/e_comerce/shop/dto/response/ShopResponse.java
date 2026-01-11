package com.hsf.e_comerce.shop.dto.response;

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
public class ShopResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    private String phoneNumber;
    private String address;
    private String status;
    private Float averageRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
