package com.hsf.e_comerce.product.dto.response;

import java.util.UUID;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private UUID parentId;
}