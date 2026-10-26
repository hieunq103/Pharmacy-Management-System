package com.pharmacy.infra.exception;

public enum ErrorCode {
    // 1. Nhóm lỗi hệ thống & hạ tầng (SYS / DB)
    UNCATEGORIZED_EXCEPTION("SYS_001", "Đã xảy ra lỗi hệ thống không xác định."),
    DATABASE_CONNECTION_ERROR("DB_001", "Không thể kết nối đến cơ sở dữ liệu."),
    DATABASE_QUERY_ERROR("DB_002", "Lỗi thực thi truy vấn cơ sở dữ liệu."),

    // 2. Nhóm lỗi xác thực & tài khoản (AUTH)
    INVALID_CREDENTIALS("AUTH_001", "Số điện thoại/CCCD hoặc mật khẩu không chính xác."),
    ACCOUNT_DISABLED("AUTH_003", "Tài khoản này đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."),
    UNAUTHENTICATED("AUTH_004", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."),
    UNAUTHORIZED("AUTH_005", "Bạn không có quyền thực hiện chức năng này."),
    PASSWORD_CHANGE_REQUIRED("AUTH_006", "Bạn cần đổi mật khẩu trong lần đăng nhập đầu tiên."),

    // 3. Nhóm lỗi thẩm định dữ liệu đầu vào (VAL)
    EMPTY_LOGIN_FIELDS("VAL_001", "Vui lòng nhập Số điện thoại/CCCD và mật khẩu."),
    EMPTY_IDENTIFIER("VAL_002", "Vui lòng nhập Số điện thoại hoặc số CCCD."),
    EMPTY_PASSWORD("VAL_003", "Vui lòng nhập mật khẩu."),
    INVALID_IDENTIFIER_FORMAT("VAL_004", "Số điện thoại (10 số) hoặc CCCD (12 số) không đúng định dạng.");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}