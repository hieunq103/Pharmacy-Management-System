package com.pharmacy.infra.exception;

public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Lỗi hệ thống không xác định.");
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank()
                ? customMessage
                : (errorCode != null ? errorCode.getDefaultMessage() : "Lỗi hệ thống không xác định."));
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Lỗi hệ thống không xác định.", cause);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage != null && !customMessage.isBlank()
                ? customMessage
                : (errorCode != null ? errorCode.getDefaultMessage() : "Lỗi hệ thống không xác định."), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode != null ? errorCode.getCode() : "UNKNOWN";
    }
}