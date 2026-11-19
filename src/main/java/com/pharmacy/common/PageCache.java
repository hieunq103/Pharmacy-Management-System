package com.pharmacy.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bộ đệm theo số trang có giới hạn: giữ tối đa {@value #MAX_PAGES} trang gần nhất (bỏ trang ít dùng nhất)
 * và mỗi trang hết hạn sau {@value #TTL_MINUTES} phút để không giữ dữ liệu cũ quá lâu. An toàn khi dùng từ nhiều luồng
 * (nạp trước trang kế tiếp chạy ở luồng nền).
 */
public final class PageCache<V> {

    private static final int MAX_PAGES = 10;
    private static final long TTL_MINUTES = 5;

    private record CachedPage<V>(V value, long storedAtNanos) {}

    private final Map<Integer, CachedPage<V>> map = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, CachedPage<V>> eldest) {
            return size() > MAX_PAGES;
        }
    };

    public synchronized V get(int page) {
        CachedPage<V> e = map.get(page);
        if (e == null) return null;
        if (System.nanoTime() - e.storedAtNanos() > TTL_MINUTES * 60_000_000_000L) {
            map.remove(page);
            return null;
        }
        return e.value();
    }

    public synchronized boolean containsKey(int page) {
        return get(page) != null;
    }

    public synchronized void put(int page, V value) {
        map.put(page, new CachedPage<>(value, System.nanoTime()));
    }

    public synchronized void clear() {
        map.clear();
    }
}
