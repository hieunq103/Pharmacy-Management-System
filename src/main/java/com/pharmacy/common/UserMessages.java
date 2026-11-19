package com.pharmacy.common;

/**
 * Thông báo lỗi an toàn để hiện cho người dùng: chỉ giữ nguyên lời nhắn nghiệp vụ (do code chủ động ném bằng
 * IllegalArgumentException/IllegalStateException); mọi lỗi kỹ thuật khác (SQL, kết nối, tên máy chủ...) được thay bằng câu chung,
 * chi tiết đã nằm trong log.
 */
public final class UserMessages {

    public static final String GENERIC_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại hoặc liên hệ quản trị viên.";

    private UserMessages() {}

    public static String safeMessage(Throwable error, String fallback) {
        Throwable root = error;
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (
            root != null &&
            (root instanceof IllegalArgumentException || root instanceof IllegalStateException) &&
            root.getMessage() != null &&
            !root.getMessage().isBlank()
        ) {
            return root.getMessage();
        }
        return fallback != null ? fallback : GENERIC_ERROR;
    }
}
