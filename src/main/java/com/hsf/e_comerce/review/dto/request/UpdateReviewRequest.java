package com.hsf.e_comerce.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UpdateReviewRequest {
    @NotNull(message = "Vui lòng chọn số sao")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 2000, message = "Nội dung quá dài")
    private String comment;

    // Ảnh mới muốn thêm vào
    private List<MultipartFile> newImages;
}