package com.pharmacy.inventory;

import static com.pharmacy.common.SqlRows.toInt;
import static com.pharmacy.common.SqlRows.toInteger;

import com.pharmacy.common.AppConstants;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

/**
 * Thống kê các lô còn tồn của một thuốc: tổng tồn, tồn còn hạn (bán được), số lô hết hạn, số lô cận hạn.
 * Dùng chung cho kho và bán hàng; chỉ nạp cho đúng danh sách ID thuốc của trang hiện tại.
 */
public record LotStats(int totalQty, int sellableQty, int expiredCount, int nearExpiredCount) {
    public static final LotStats EMPTY = new LotStats(0, 0, 0, 0);

    public static Map<Integer, LotStats> load(Session session, Collection<Integer> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDate today = LocalDate.now();
        List<Object[]> rows = session
            .createSelectionQuery(
                """
                SELECT il.medicine.id,
                       COALESCE(SUM(il.quantityRemaining), 0),
                       COALESCE(SUM(CASE WHEN il.expiryDate > :today THEN il.quantityRemaining ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN il.expiryDate <= :today THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN il.expiryDate > :today AND il.expiryDate <= :nearExpiryDate THEN 1 ELSE 0 END), 0)
                FROM InventoryLot il
                WHERE il.medicine.id IN (:ids) AND il.quantityRemaining > 0
                GROUP BY il.medicine.id
                """,
                Object[].class
            )
            .setParameter("ids", medicineIds)
            .setParameter("today", today)
            .setParameter("nearExpiryDate", today.plusDays(AppConstants.NEAR_EXPIRY_DAYS))
            .list();
        Map<Integer, LotStats> map = new HashMap<>(rows.size() * 2);
        for (Object[] r : rows) {
            map.put(toInteger(r[0]), new LotStats(toInt(r[1]), toInt(r[2]), toInt(r[3]), toInt(r[4])));
        }
        return map;
    }
}
