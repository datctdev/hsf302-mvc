package com.hsf.e_comerce.kyc.dto.request;

import com.hsf.e_comerce.kyc.valueobject.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class KycThumbnailAttachRequest {
    @NotNull
    private KycDocumentType type;

    @NotBlank
    private String fileHash;
}
