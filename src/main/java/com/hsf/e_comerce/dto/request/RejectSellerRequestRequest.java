package com.hsf.e_comerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectSellerRequestRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionReason;
}
