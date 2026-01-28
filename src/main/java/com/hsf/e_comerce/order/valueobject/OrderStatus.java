package com.hsf.e_comerce.order.valueobject;

/**
 * Trạng thái đơn hàng. Luồng chuẩn:
 * PENDING_PAYMENT → (thanh toán) CONFIRMED → PROCESSING → SHIPPED → DELIVERED.
 * Hủy chỉ cho phép từ PENDING_PAYMENT hoặc CONFIRMED.
 * Đơn GHN được tạo khi CONFIRMED→PROCESSING/SHIPPED hoặc PENDING_PAYMENT→CONFIRMED.
 */
public enum OrderStatus {
    PENDING,         // Chờ xác nhận (ít dùng; createOrder dùng PENDING_PAYMENT)
    PENDING_PAYMENT, // Chờ thanh toán – buyer chưa COD/VNPay
    CONFIRMED,       // Đã thanh toán / đã xác nhận – seller chuẩn bị đóng gói
    PROCESSING,      // Đang xử lý (đóng gói, có thể đã tạo vận đơn GHN)
    SHIPPED,         // Đã giao cho GHN / đang vận chuyển
    DELIVERED,       // Đã nhận hàng – buyer có thể đánh giá
    CANCELLED,       // Đã hủy
    REFUNDED         // Đã hoàn tiền
}
