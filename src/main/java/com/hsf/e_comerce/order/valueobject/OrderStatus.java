package com.hsf.e_comerce.order.valueobject;

public enum OrderStatus {
    PENDING,        // Chờ xác nhận
    CONFIRMED,      // Đã xác nhận
    PROCESSING,    // Đang xử lý (đóng gói)
    SHIPPED,        // Đã giao hàng
    DELIVERED,      // Đã nhận hàng
    CANCELLED,      // Đã hủy
    REFUNDED        // Đã hoàn tiền
}
