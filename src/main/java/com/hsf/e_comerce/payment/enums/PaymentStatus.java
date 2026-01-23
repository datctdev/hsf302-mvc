package com.hsf.e_comerce.payment.enums;

public enum PaymentStatus {
    INIT,        // Khởi tạo, chưa redirect
    PENDING,     // COD chờ thanh toán
    SUCCESS,     // Thanh toán thành công
    FAILED,      // Thanh toán thất bại
    CANCELLED    // Hủy
}
