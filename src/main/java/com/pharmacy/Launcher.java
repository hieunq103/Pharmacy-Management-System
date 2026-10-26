package com.pharmacy;

/**
 * Điểm khởi động khi chạy trực tiếp từ IDE / classpath (không qua module path).
 * JVM từ chối chạy main của lớp kế thừa javafx.application.Application nếu JavaFX
 * không nằm trên module path ("JavaFX runtime components are missing"); lớp trung gian
 * này không kế thừa Application nên tránh được kiểm tra đó.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        PharmacyApplication.main(args);
    }
}
