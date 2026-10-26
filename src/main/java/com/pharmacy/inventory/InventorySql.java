package com.pharmacy.inventory;

import com.pharmacy.common.AppConstants;

/**
 * Các mảnh SQL tổng hợp tồn kho theo lô dùng chung cho màn hình Kho, Bán hàng và Dashboard.
 * Trước đây mỗi truy vấn tự viết 5 subquery tương quan (nearest_lot, nearest_expiry, total_qty,
 * expired_count, near_expired_count) cho TỪNG thuốc; nay gom vào một chỗ và tính bằng
 * conditional aggregation trong một lần quét chỉ mục idx_inventory_lots_stock.
 */
public final class InventorySql {

    private InventorySql() {
    }

    /** Biểu thức ngưỡng sắp hết hàng của một thuốc (alias bảng medicines là m). */
    public static final String LOW_STOCK_THRESHOLD =
            "COALESCE(NULLIF(m.min_stock_threshold, 0), " + AppConstants.DEFAULT_LOW_STOCK_THRESHOLD + ")";

    private static final String NEAR_EXPIRY_END = "DATE_ADD(CURDATE(), INTERVAL " + AppConstants.NEAR_EXPIRY_DAYS + " DAY)";

    private static final String AGGREGATES = """
                    SUM(il.quantity_remaining) AS total_qty,
                    SUM(il.expiry_date <= CURDATE()) AS expired_count,
                    SUM(IF(il.expiry_date <= CURDATE(), il.quantity_remaining, 0)) AS expired_qty,
                    SUM(il.expiry_date > CURDATE() AND il.expiry_date <= %s) AS near_expired_count,
                    MIN(il.expiry_date) AS nearest_expiry
            """.formatted(NEAR_EXPIRY_END);

    /**
     * Bảng dẫn xuất ls(medicine_id, total_qty, expired_count, expired_qty, near_expired_count, nearest_expiry)
     * cho TẤT CẢ thuốc còn tồn — dùng khi cần lọc/đếm trên toàn bộ danh mục (1 lần GROUP BY thay vì N subquery).
     * Thuốc không còn lô tồn sẽ có các cột NULL sau LEFT JOIN.
     */
    public static final String LOT_STATS_ALL = """
            LEFT JOIN (
                SELECT il.medicine_id,
            %s
                FROM inventory_lots il
                WHERE il.quantity_remaining > 0
                GROUP BY il.medicine_id
            ) ls ON ls.medicine_id = m.id
            """.formatted(AGGREGATES);

    /**
     * Bảng dẫn xuất LATERAL chỉ tính cho từng dòng m.* đã được phân trang (vài dòng) —
     * dùng chỉ mục (medicine_id, quantity_remaining, expiry_date). Yêu cầu MySQL >= 8.0.14.
     */
    public static final String LOT_STATS_LATERAL = """
            LEFT JOIN LATERAL (
                SELECT
            %s
                FROM inventory_lots il
                WHERE il.medicine_id = m.id AND il.quantity_remaining > 0
            ) ls ON TRUE
            """.formatted(AGGREGATES);

    /** Số lô của lô gần hết hạn nhất còn tồn (chỉ dùng cho các dòng đã phân trang). */
    public static final String NEAREST_LOT_NUMBER = """
            (SELECT il2.lot_number FROM inventory_lots il2
             WHERE il2.medicine_id = m.id AND il2.quantity_remaining > 0
             ORDER BY il2.expiry_date ASC, il2.id ASC LIMIT 1)
            """;

    /**
     * Danh sách cột chuẩn cho MedicineInventoryDTO, theo đúng thứ tự mà
     * {@link InventoryRepository#mapInventoryRow(Object[])} đọc:
     * 0 id, 1 code, 2 name, 3 category, 4 requires_prescription, 5 unit, 6 sell_price,
     * 7 active_ingredient, 8 manufacturer, 9 nearest_lot, 10 nearest_expiry, 11 total_qty,
     * 12 expired_count, 13 near_expired_count, 14 min_stock_threshold.
     */
    public static final String INVENTORY_COLUMNS = """
            m.id, m.code, m.name, mc.name AS category_name, m.requires_prescription, m.unit, m.sell_price,
            m.active_ingredient, m.manufacturer,
            %s AS nearest_lot,
            ls.nearest_expiry,
            COALESCE(ls.total_qty, 0) AS total_qty,
            COALESCE(ls.expired_count, 0) AS expired_count,
            COALESCE(ls.near_expired_count, 0) AS near_expired_count,
            m.min_stock_threshold
            """.formatted(NEAREST_LOT_NUMBER);

    /** Điều kiện tìm kiếm thuốc theo mã / tên / hoạt chất (tham số :keyword). */
    public static final String KEYWORD_FILTER =
            " AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ";

    public static String likeParam(String keyword) {
        return "%" + keyword.trim() + "%";
    }

    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
