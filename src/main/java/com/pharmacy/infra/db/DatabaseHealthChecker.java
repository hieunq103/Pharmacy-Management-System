package com.pharmacy.infra.db;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class DatabaseHealthChecker {
    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DB-HealthChecker-Thread");
        thread.setDaemon(true); // Thread tự giải phóng khi tắt app
        return thread;
    });

    // Thuộc tính theo dõi trạng thái (true = Online, false = Mất kết nối)
    private static final BooleanProperty connected = new SimpleBooleanProperty(true);

    private DatabaseHealthChecker() {}

    public static BooleanProperty connectedProperty() {
        return connected;
    }

    public static boolean isConnected() {
        return connected.get();
    }

    /**
     * Bắt đầu giám sát định kỳ trạng thái CSDL
     * @param intervalSeconds Chu kỳ kiểm tra (giây)
     * @param onStatusChange Callback gọi khi trạng thái thay đổi (chạy trên JavaFX Application Thread)
     */
    public static void startMonitoring(int intervalSeconds, Consumer<Boolean> onStatusChange) {
        scheduler.scheduleWithFixedDelay(() -> {
            boolean alive = HibernateUtil.isDatabaseAlive();
            if (alive != connected.get()) {
                log.warn("Trạng thái kết nối CSDL thay đổi: {}", alive ? "ONLINE" : "DISCONNECTED");
                Platform.runLater(() -> {
                    connected.set(alive);
                    if (onStatusChange != null) {
                        onStatusChange.accept(alive);
                    }
                });
            }
        }, 2, intervalSeconds, TimeUnit.SECONDS);
    }
}