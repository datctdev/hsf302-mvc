package com.hsf.e_comerce.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
public class CreateReviewRequest {
    // Phải có để xác thực mua hàng
    private UUID subOrderId;

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 2000, message = "Nội dung quá dài")
    private String comment;

    private List<MultipartFile> images;
}