package com.pharmacy.common;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Bộ chạy nền dùng chung: tối đa 4 luồng daemon, luồng rảnh tự kết thúc nên không tích tụ theo thời gian
public final class BackgroundTasks {

    private static final int MAX_THREADS = 4;
    private static final int IDLE_SECONDS = 30;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            MAX_THREADS, MAX_THREADS, IDLE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "pharma-bg-" + SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            });

    static {
        POOL.allowCoreThreadTimeOut(true);
    }

    private BackgroundTasks() {
    }

    public static void run(Runnable task) {
        POOL.execute(task);
    }
}
