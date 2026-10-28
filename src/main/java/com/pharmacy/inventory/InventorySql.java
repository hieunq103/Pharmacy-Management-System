package com.pharmacy.inventory;

import com.pharmacy.common.AppConstants;

public final class InventorySql {
    private InventorySql() {
    }

    public static final String LOW_STOCK_THRESHOLD =
            "COALESCE(NULLIF(m.min_stock_threshold, 0), " + AppConstants.DEFAULT_LOW_STOCK_THRESHOLD + ")";

    private static final String NEAR_EXPIRY_END = "DATE_ADD(CURDATE(), INTERVAL " + AppConstants.NEAR_EXPIRY_DAYS + " DAY)";

    private static final String AGGREGATES = """
                    SUM(il.quantity_remaining) AS total_qty,
                    SUM(il.expiry_date <= CURDATE()) AS expired_count,
                    SUM(IF(il.expiry_date <= CURDATE(), il.quantity_remaining, 0)) AS expired_qty,
                    SUM(il.expiry_date > CURDATE() AND il.expiry_date <= %s) AS near_expired_count
            """.formatted(NEAR_EXPIRY_END);

    public static final String LOT_STATS_ALL = """
            LEFT JOIN (
                SELECT il.medicine_id,
            %s
                FROM inventory_lots il
                WHERE il.quantity_remaining > 0
                GROUP BY il.medicine_id
            ) ls ON ls.medicine_id = m.id
            """.formatted(AGGREGATES);

    public static final String LOT_STATS_LATERAL = """
            LEFT JOIN LATERAL (
                SELECT
            %s
                FROM inventory_lots il
                WHERE il.medicine_id = m.id AND il.quantity_remaining > 0
            ) ls ON TRUE
            """.formatted(AGGREGATES);

    public static final String INVENTORY_COLUMNS = """
            m.id, m.code, m.name, mc.name AS category_name, m.requires_prescription, m.unit, m.sell_price,
            m.active_ingredient, m.manufacturer,
            COALESCE(ls.total_qty, 0) AS total_qty,
            COALESCE(ls.expired_count, 0) AS expired_count,
            COALESCE(ls.near_expired_count, 0) AS near_expired_count,
            m.min_stock_threshold
            """;

    public static final String KEYWORD_FILTER =
            " AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ";

    public static String likeParam(String keyword) {
        return "%" + keyword.trim() + "%";
    }

    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
