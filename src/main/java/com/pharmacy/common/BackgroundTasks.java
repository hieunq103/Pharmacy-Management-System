package com.pharmacy.common;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundTasks {

    private static final int CORE_THREADS = 8;
    private static final int MAX_THREADS = 16;
    private static final int IDLE_SECONDS = 60;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
        CORE_THREADS,
        MAX_THREADS,
        IDLE_SECONDS,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = new Thread(r, "pharma-bg-" + SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    );

    static {
        POOL.prestartAllCoreThreads();
    }

    private BackgroundTasks() {}

    public static java.util.concurrent.Executor getExecutor() {
        return POOL;
    }

    public static void run(Runnable task) {
        POOL.execute(task);
    }
}
