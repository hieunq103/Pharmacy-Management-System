package com.pharmacy.common;

import java.util.Collections;
import java.util.List;

/**
 * Lớp đóng gói kết quả phân trang chuẩn Server-side.
 */
public class PageResult<T> {

    private final List<T> items;
    private final long totalElements;
    private final int totalPages;
    private final int currentPage;

    public PageResult(List<T> items, long totalElements, int currentPage, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
