package com.pharmacy.common;

import javafx.util.Duration;

public final class AppConstants {
    private AppConstants() {
    }

    public static final int NEAR_EXPIRY_DAYS = 90;
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    public static final int DEFAULT_PAGE_SIZE = 7;

    public static final Duration SEARCH_DEBOUNCE = Duration.millis(300);

    public static final double DRAWER_WIDTH = 520.0;
    public static final Duration DRAWER_ANIMATION_DURATION = Duration.millis(250);
    public static final Duration DRAWER_CLOSE_DURATION = Duration.millis(200);
}
