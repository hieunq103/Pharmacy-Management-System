package com.pharmacy.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Tiện ích định dạng ngày tháng và tiền tệ tập trung cho toàn bộ ứng dụng.
 */
public final class FormatUtils {

    private FormatUtils() {}

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DecimalFormat CURRENCY_FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMATTER = new DecimalFormat("#,##0", symbols);
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(amount);
        }
    }

    public static String formatCurrency(double amount) {
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(amount);
        }
    }

    public static String formatCurrency(long amount) {
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(amount);
        }
    }

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "NV";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    /**
     * Chuyển đổi chuỗi tiếng Việt có dấu thành không dấu để hỗ trợ tìm kiếm linh hoạt.
     */
    public static String removeAccents(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }

    /**
     * Cấu hình định dạng dd/MM/yyyy chuẩn cho DatePicker toàn hệ thống.
     */
    public static void setupDatePicker(javafx.scene.control.DatePicker datePicker) {
        if (datePicker == null) return;
        datePicker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return formatDate(date);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(string.trim(), DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }
}
