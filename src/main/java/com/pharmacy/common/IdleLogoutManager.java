package com.pharmacy.common;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tự đăng xuất khi người dùng không thao tác trong một khoảng thời gian (mặc định 30 phút theo cấu hình
 * auth.sessionTimeoutMinutes), để máy bán hàng bị bỏ quên không còn mở sẵn tài khoản.
 * Không đếm giờ khi đang có hộp thoại/cửa sổ khác mở (ví dụ đang chờ khách chuyển khoản).
 */
public final class IdleLogoutManager {

    private static final Logger log = LoggerFactory.getLogger(IdleLogoutManager.class);

    private final Scene scene;
    private final long timeoutNanos;
    private final Runnable onTimeout;
    private final Timeline checker;
    private final EventHandler<InputEvent> activityFilter = e -> touch();
    private volatile long lastActivityNanos = System.nanoTime();
    private boolean running;

    public IdleLogoutManager(Scene scene, long timeoutMillis, Runnable onTimeout) {
        this.scene = scene;
        this.timeoutNanos = timeoutMillis * 1_000_000L;
        this.onTimeout = onTimeout;
        long checkEveryMillis = Math.max(100, Math.min(10_000, timeoutMillis / 4));
        this.checker = new Timeline(new KeyFrame(Duration.millis(checkEveryMillis), e -> check()));
        this.checker.setCycleCount(Animation.INDEFINITE);
    }

    /** Khởi động giám sát; timeoutMillis <= 0 nghĩa là tắt tính năng. */
    public static IdleLogoutManager start(Scene scene, long timeoutMillis, Runnable onTimeout) {
        if (scene == null || timeoutMillis <= 0) return null;
        IdleLogoutManager manager = new IdleLogoutManager(scene, timeoutMillis, onTimeout);
        manager.begin();
        return manager;
    }

    private void begin() {
        running = true;
        lastActivityNanos = System.nanoTime();
        for (EventType<? extends InputEvent> type : new EventType[] {
            MouseEvent.MOUSE_MOVED,
            MouseEvent.MOUSE_PRESSED,
            KeyEvent.KEY_PRESSED,
            ScrollEvent.SCROLL,
        }) {
            scene.addEventFilter(type, activityFilter);
        }
        checker.play();
    }

    public void stop() {
        if (!running) return;
        running = false;
        checker.stop();
        for (EventType<? extends InputEvent> type : new EventType[] {
            MouseEvent.MOUSE_MOVED,
            MouseEvent.MOUSE_PRESSED,
            KeyEvent.KEY_PRESSED,
            ScrollEvent.SCROLL,
        }) {
            scene.removeEventFilter(type, activityFilter);
        }
    }

    private void touch() {
        lastActivityNanos = System.nanoTime();
    }

    private void check() {
        if (!running) return;
        // Đang có cửa sổ khác (hộp thoại thanh toán, cảnh báo...) → coi như đang thao tác
        boolean otherWindowOpen = Window.getWindows()
            .stream()
            .anyMatch(w -> w.isShowing() && w != scene.getWindow());
        if (otherWindowOpen) {
            touch();
            return;
        }
        if (System.nanoTime() - lastActivityNanos >= timeoutNanos) {
            log.info("Không thao tác quá {} phút, tự động đăng xuất.", timeoutNanos / 60_000_000_000L);
            stop();
            onTimeout.run();
        }
    }
}
