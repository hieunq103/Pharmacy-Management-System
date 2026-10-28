package com.pharmacy.infra.security;

import java.security.SecureRandom;

// Mật khẩu tạm cấp cho tài khoản mới: 8 chữ số ngẫu nhiên, nhân viên bắt buộc đổi ở lần đăng nhập đầu
public final class TemporaryPassword {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 8;

    private TemporaryPassword() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
