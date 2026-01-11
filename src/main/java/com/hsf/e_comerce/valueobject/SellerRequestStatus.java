package com.hsf.e_comerce.valueobject;

public enum SellerRequestStatus {
    PENDING("PENDING", "Đang chờ duyệt"),
    APPROVED("APPROVED", "Đã được duyệt"),
    REJECTED("REJECTED", "Đã bị từ chối");

    private final String code;
    private final String description;

    SellerRequestStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SellerRequestStatus fromCode(String code) {
        for (SellerRequestStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SellerRequestStatus code: " + code);
    }
}
