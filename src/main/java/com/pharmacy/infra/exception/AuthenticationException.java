package com.pharmacy.infra.exception;

public class AuthenticationException extends AppException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}