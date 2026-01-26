package com.hsf.e_comerce.auth.service;

/**
 * Gửi email (xác minh, reset password, thông báo...).
 */
public interface EmailService {

    /**
     * Gửi email chứa link xác minh tài khoản.
     * @param toEmail địa chỉ người nhận
     * @param fullName tên hiển thị
     * @param verificationLink link dạng /verify-email?token=xxx (base URL đã cộng ở impl)
     */
    void sendVerificationEmail(String toEmail, String fullName, String verificationLink);
}
