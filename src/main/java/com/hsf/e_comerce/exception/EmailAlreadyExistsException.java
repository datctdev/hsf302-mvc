package com.hsf.e_comerce.exception;

public class EmailAlreadyExistsException extends CustomException {
    public EmailAlreadyExistsException(String email) {
        super("Email đã tồn tại: " + email);
    }
}
