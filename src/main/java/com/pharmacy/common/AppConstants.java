package com.pharmacy.common;

import javafx.util.Duration;

public final class AppConstants {

    private AppConstants() {
    }

    // Quy tắc nghiệp vụ kho dùng chung cho Dashboard, Kho, Bán hàng (tránh mỗi màn hình một ngưỡng)
    /** Lô còn hạn nhưng hết hạn trong vòng N ngày tới được coi là "cận hạn". */
    public static final int NEAR_EXPIRY_DAYS = 90;
    /** Ngưỡng "sắp hết hàng" mặc định khi thuốc chưa cấu hình min_stock_threshold (= 0). */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    // Cấu hình phân trang dữ liệu
    public static final int DEFAULT_PAGE_SIZE = 7;

    // Thời gian chờ gõ tìm kiếm (Debounce)
    public static final Duration SEARCH_DEBOUNCE = Duration.millis(300);

    // Kích thước và hiệu ứng Drawer trượt cạnh phải
    public static final double DRAWER_WIDTH = 520.0;
    public static final Duration DRAWER_ANIMATION_DURATION = Duration.millis(250);
    public static final Duration DRAWER_CLOSE_DURATION = Duration.millis(200);
}
