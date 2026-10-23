package com.pharmacy.common;

import javafx.util.Duration;

public final class AppConstants {

    private AppConstants() {
    }

    // Cấu hình phân trang dữ liệu
    public static final int DEFAULT_PAGE_SIZE = 7;

    // Thời gian chờ gõ tìm kiếm (Debounce)
    public static final Duration SEARCH_DEBOUNCE = Duration.millis(300);

    // Kích thước và hiệu ứng Drawer trượt cạnh phải
    public static final double DRAWER_WIDTH = 520.0;
    public static final Duration DRAWER_ANIMATION_DURATION = Duration.millis(250);
    public static final Duration DRAWER_CLOSE_DURATION = Duration.millis(200);
}
