package com.hsf.e_comerce.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRequestRequest {

    @NotBlank(message = "Tên shop không được để trống")
    @Size(min = 3, max = 100, message = "Tên shop phải có từ 3 đến 100 ký tự")
    private String shopName;

    @Size(max = 2000, message = "Mô tả shop không được vượt quá 2000 ký tự")
    private String shopDescription;

    @NotBlank(message = "Số điện thoại shop không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String shopPhone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String shopAddress; // Optional - có thể cập nhật sau

    private String logoUrl;

    private String coverImageUrl;
}
