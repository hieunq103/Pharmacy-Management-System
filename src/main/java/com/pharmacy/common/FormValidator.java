package com.pharmacy.common;

import com.pharmacy.infra.exception.ErrorCode;
import java.util.regex.Pattern;

public final class FormValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(03|05|07|08|09)\\d{8}$");

    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^\\d{12}$");

    private FormValidator() {
    }

    /**
     * Xác thực form đăng nhập với tên đăng nhập (Mã nhân viên PMSxxxx).
     */
    public static ValidationResult validateLoginForm(String username, String password) {
        String cleanUsername = (username != null) ? username.trim() : "";
        boolean idEmpty = cleanUsername.isEmpty();
        boolean pwdEmpty = (password == null || password.isEmpty());

        // Kiểm tra để trống
        if (idEmpty && pwdEmpty) {
            return ValidationResult.fail(TargetField.BOTH, ErrorCode.EMPTY_LOGIN_FIELDS);
        }
        if (idEmpty) {
            return ValidationResult.fail(TargetField.IDENTIFIER, ErrorCode.EMPTY_IDENTIFIER);
        }
        if (pwdEmpty) {
            return ValidationResult.fail(TargetField.PASSWORD, ErrorCode.EMPTY_PASSWORD);
        }

        return ValidationResult.ok();
    }

    /**
     * Kiểm tra số điện thoại Việt Nam hợp lệ (10 số, đầu 03, 05, 07, 08, 09)
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Kiểm tra căn cước công dân Việt Nam hợp lệ (đúng 12 số)
     */
    public static boolean isValidNationalId(String nationalId) {
        return nationalId != null && NATIONAL_ID_PATTERN.matcher(nationalId.trim()).matches();
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