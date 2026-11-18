package com.pharmacy.infra.exception;

public enum ErrorCode {
    DATABASE_CONNECTION_ERROR("Không thể kết nối đến cơ sở dữ liệu."),

    INVALID_CREDENTIALS("Tên đăng nhập hoặc mật khẩu không chính xác."),
    ACCOUNT_DISABLED("Tài khoản này đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."),
    PASSWORD_CHANGE_REQUIRED("Bạn cần đổi mật khẩu trong lần đăng nhập đầu tiên."),

    EMPTY_LOGIN_FIELDS("Vui lòng nhập tên đăng nhập và mật khẩu."),
    EMPTY_IDENTIFIER("Vui lòng nhập tên đăng nhập."),
    EMPTY_PASSWORD("Vui lòng nhập mật khẩu.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
