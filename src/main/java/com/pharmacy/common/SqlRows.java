package com.pharmacy.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chuyển đổi an toàn giá trị cột trả về từ native query (Object[]) sang kiểu Java.
 * Driver MySQL có thể trả java.sql.Date/Timestamp hoặc java.time tùy cấu hình,
 * số có thể là Long/Integer/BigInteger/BigDecimal — gom về một chỗ để tránh lặp code.
 */
public final class SqlRows {

    private SqlRows() {
    }

    public static Integer toInteger(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    public static int toInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    public static Long toLongObj(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    public static long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(value.toString());
    }

    public static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return false;
    }

    public static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    public static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return null;
    }

    public static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
