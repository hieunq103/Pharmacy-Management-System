package com.pharmacy.infra;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ghi vào log mức bộ nhớ đang dùng mỗi {@value #INTERVAL_MINUTES} phút để theo dõi khi app chạy lâu:
 * nếu "đã dùng" tăng đều qua nhiều giờ mà không giảm thì có dấu hiệu rò rỉ.
 */
public final class MemoryMonitor {

    private static final Logger log = LoggerFactory.getLogger(MemoryMonitor.class);
    private static final int INTERVAL_MINUTES = 10;
    private static final double WARN_RATIO = 0.85;
    private static boolean started;

    private MemoryMonitor() {}

    public static synchronized void start() {
        if (started) return;
        started = true;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Memory-Monitor-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(MemoryMonitor::logUsage, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
        log.info("Theo dõi bộ nhớ: ghi log mỗi {} phút (tối đa {} MB).", INTERVAL_MINUTES, maxMb());
    }

    private static void logUsage() {
        try {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMb = maxMb();
            int threads = Thread.activeCount();
            if (maxMb > 0 && usedMb > maxMb * WARN_RATIO) {
                log.warn("Bộ nhớ gần đầy: đã dùng {} MB / tối đa {} MB, {} luồng.", usedMb, maxMb, threads);
            } else {
                log.info("Bộ nhớ: đã dùng {} MB / tối đa {} MB, {} luồng.", usedMb, maxMb, threads);
            }
        } catch (Exception e) {
            log.debug("Không ghi được thông tin bộ nhớ: {}", e.getMessage());
        }
    }

    private static long maxMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }
}
