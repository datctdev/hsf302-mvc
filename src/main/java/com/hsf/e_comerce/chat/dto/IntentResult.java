package com.hsf.e_comerce.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    /** "chao" | "tim_san_pham" | "khac" */
    private String intent;
    /** Từ khóa tìm kiếm (rút ra từ tin nhắn), rỗng nếu không phải tìm sản phẩm. */
    private String keyword;
}
