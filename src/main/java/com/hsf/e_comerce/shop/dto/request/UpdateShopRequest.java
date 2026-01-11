package com.hsf.e_comerce.shop.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShopRequest {

    @Size(max = 255, message = "Tên shop không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả shop không được vượt quá 2000 ký tự")
    private String description;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phoneNumber;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    private String logoUrl;

    private String coverImageUrl;
}
