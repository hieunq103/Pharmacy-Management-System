package com.pharmacy.common;

import com.pharmacy.infra.exception.ErrorCode;
import java.util.regex.Pattern;

public final class FormValidator {

    // Regex SĐT Việt Nam: 10 chữ số, bắt đầu bằng đầu số 03, 05, 07, 08, 09
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(03|05|07|08|09)\\d{8}$");

    // Regex CCCD Việt Nam: đúng 12 chữ số
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^\\d{12}$");

    private FormValidator() {}

    /**
     * Xác thực form đăng nhập với định danh là SĐT hoặc CCCD
     */
    public static ValidationResult validateLoginForm(String identifier, String password) {
        String cleanIdentifier = (identifier != null) ? identifier.trim() : "";
        boolean idEmpty = cleanIdentifier.isEmpty();
        boolean pwdEmpty = (password == null || password.isEmpty());

        // 1. Kiểm tra để trống
        if (idEmpty && pwdEmpty) {
            return ValidationResult.fail(TargetField.BOTH, ErrorCode.EMPTY_LOGIN_FIELDS);
        }
        if (idEmpty) {
            return ValidationResult.fail(TargetField.IDENTIFIER, ErrorCode.EMPTY_IDENTIFIER);
        }
        if (pwdEmpty) {
            return ValidationResult.fail(TargetField.PASSWORD, ErrorCode.EMPTY_PASSWORD);
        }

        // 2. Kiểm tra định dạng SĐT (10 số) hoặc CCCD (12 số)
        if (!isValidIdentifier(cleanIdentifier)) {
            return ValidationResult.fail(TargetField.IDENTIFIER, ErrorCode.INVALID_CREDENTIALS);
        }

        return ValidationResult.ok();
    }

    /**
     * Kiểm tra chuỗi định danh có đúng cấu trúc SĐT hoặc CCCD hợp lệ hay không
     */
    public static boolean isValidIdentifier(String identifier) {
        if (identifier == null) {
            return false;
        }
        String trimmed = identifier.trim();
        return PHONE_PATTERN.matcher(trimmed).matches() || NATIONAL_ID_PATTERN.matcher(trimmed).matches();
    }

    public enum TargetField {
        NONE, IDENTIFIER, PASSWORD, BOTH
    }

    public record ValidationResult(boolean isValid, TargetField targetField, ErrorCode errorCode) {
        public static ValidationResult ok() {
            return new ValidationResult(true, TargetField.NONE, null);
        }

        public static ValidationResult fail(TargetField field, ErrorCode code) {
            return new ValidationResult(false, field, code);
        }
    }
}