package com.pharmacy.common;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.query.SelectionQuery;

/**
 * Dựng điều kiện tìm kiếm HQL từ từ khóa người dùng nhập:
 * <ul>
 *   <li>Tách từ khóa thành nhiều token, mỗi token phải khớp (AND) ở ít nhất một cột (OR) →
 *       "paracetamol 500" tìm ra "Paracetamol 500mg" dù hai từ nằm xa nhau, hoặc ở hai cột khác nhau.</li>
 *   <li>Cột mã ({@link #prefix}) khớp theo tiền tố ({@code token%}) nên dùng được index UNIQUE/INDEX;
 *       riêng token toàn chữ số (phần đuôi của mã) khớp chứa.</li>
 *   <li>Cột văn bản ({@link #contains}) khớp chứa ({@code %token%}).</li>
 *   <li>Ký tự đặc biệt {@code % _} do người dùng gõ được escape, không còn khớp nhầm mọi dòng.</li>
 * </ul>
 * Collation utf8mb4_unicode_ci của schema đã không phân biệt hoa/thường và dấu.
 */
public final class SearchFilter {

    private static final int MAX_TOKENS = 6;
    private static final char ESC = '!';

    private final List<String> tokens;
    private final String param;
    private final List<String> prefixFields = new ArrayList<>();
    private final List<String> containsFields = new ArrayList<>();
    private final List<String> customTemplates = new ArrayList<>();

    private SearchFilter(String keyword, String param) {
        this.param = param;
        this.tokens = tokenize(keyword);
    }

    public static SearchFilter of(String keyword, String paramPrefix) {
        return new SearchFilter(keyword, paramPrefix);
    }

    public SearchFilter prefix(String... fields) {
        prefixFields.addAll(List.of(fields));
        return this;
    }

    public SearchFilter contains(String... fields) {
        containsFields.addAll(List.of(fields));
        return this;
    }

    /** Điều kiện tùy biến cho mỗi token, dùng {@code {p}} làm chỗ đặt tham số dạng chứa (vd. subquery). */
    public SearchFilter custom(String template) {
        customTemplates.add(template);
        return this;
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public String hql() {
        if (tokens.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            List<String> ors = new ArrayList<>();
            for (String f : prefixFields) ors.add(f + " LIKE :" + param + i + "p ESCAPE '" + ESC + "'");
            for (String f : containsFields) ors.add(f + " LIKE :" + param + i + "c ESCAPE '" + ESC + "'");
            for (String t : customTemplates) ors.add(t.replace("{p}", ":" + param + i + "c"));
            sb.append(" AND (").append(String.join(" OR ", ors)).append(")");
        }
        return sb.append(' ').toString();
    }

    public void bind(SelectionQuery<?> query) {
        boolean usesPrefix = !prefixFields.isEmpty();
        boolean usesContains = !containsFields.isEmpty() || !customTemplates.isEmpty();
        for (int i = 0; i < tokens.size(); i++) {
            String escaped = escape(tokens.get(i));
            // Chỉ gán tham số thực sự có trong câu HQL (gán thừa sẽ ném IllegalArgumentException)
            if (usesPrefix) {
                // Token toàn chữ số (vd. "003" trong MED003) là phần đuôi của mã → phải khớp chứa, không chỉ tiền tố
                boolean digitsOnly = tokens.get(i).chars().allMatch(Character::isDigit);
                query.setParameter(param + i + "p", digitsOnly ? "%" + escaped + "%" : escaped + "%");
            }
            if (usesContains) {
                query.setParameter(param + i + "c", "%" + escaped + "%");
            }
        }
    }

    private static List<String> tokenize(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String t : keyword.trim().split("\\s+")) {
            if (!t.isEmpty() && out.size() < MAX_TOKENS && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private static String escape(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
