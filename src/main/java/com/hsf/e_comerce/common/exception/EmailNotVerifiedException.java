package com.hsf.e_comerce.common.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Ném khi user đăng nhập nhưng chưa xác minh email.
 */
public class EmailNotVerifiedException extends AuthenticationException {

    public EmailNotVerifiedException(String msg) {
        super(msg);
    }

    public EmailNotVerifiedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
