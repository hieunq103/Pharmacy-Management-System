package com.pharmacy.infra.exception;

public abstract class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank()
                ? customMessage
                : (errorCode != null ? errorCode.getDefaultMessage() : "Lỗi hệ thống không xác định."));
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
