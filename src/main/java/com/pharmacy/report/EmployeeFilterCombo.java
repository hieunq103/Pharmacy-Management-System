package com.pharmacy.report;

import com.pharmacy.employee.dto.EmployeeOption;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

/**
 * Ô chọn nhân viên có gõ để tìm: gõ một phần mã hoặc họ tên (không phân biệt hoa/thường, có hay không dấu,
 * nhiều từ thì mọi từ đều phải khớp) → danh sách thu hẹp lại; chọn một dòng để xem báo cáo của người đó.
 */
final class EmployeeFilterCombo {

    private final ComboBox<EmployeeOption> combo;
    private final ObservableList<EmployeeOption> master = FXCollections.observableArrayList(EmployeeOption.ALL);
    private final FilteredList<EmployeeOption> filtered = new FilteredList<>(master, o -> true);
    private final Consumer<Integer> onChanged;

    // Chặn vòng lặp: mọi thay đổi do chương trình (đổi giá trị, đổi bộ lọc danh sách) không được kích hoạt lại sự kiện
    private boolean updating;
    // Nhân viên đang được áp dụng cho báo cáo (null = tất cả)
    private Integer committedId;

    EmployeeFilterCombo(ComboBox<EmployeeOption> combo, Consumer<Integer> onChanged) {
        this.combo = combo;
        this.onChanged = onChanged;
        combo.setEditable(true);
        combo.setItems(filtered);
        combo.setVisibleRowCount(8);
        combo.setPromptText("Tìm mã hoặc tên nhân viên...");
        combo.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(EmployeeOption o) {
                    return o == null ? "" : o.label();
                }

                @Override
                public EmployeeOption fromString(String text) {
                    if (text == null) return null;
                    return master
                        .stream()
                        .filter(o -> o.label().equalsIgnoreCase(text.trim()))
                        .findFirst()
                        .orElse(null);
                }
            }
        );
        setSelected(EmployeeOption.ALL);

        // Người dùng chọn một dòng trong danh sách
        combo.valueProperty().addListener((obs, old, chosen) -> {
            if (updating || chosen == null) return;
            commit(chosen);
        });

        // Gõ → lọc danh sách và mở popup
        combo
            .getEditor()
            .textProperty()
            .addListener((obs, oldText, newText) -> {
                if (updating) return;
                EmployeeOption current = combo.getValue();
                if (current != null && current.label().equals(newText)) return;
                applyFilter(newText);
                if (!combo.isShowing() && combo.getEditor().isFocused()) combo.show();
            });

        combo.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                // Enter: đúng nhân viên vừa gõ, hoặc dòng đầu tiên đang hiển thị
                EmployeeOption pick = combo.getConverter().fromString(combo.getEditor().getText());
                if (pick == null && !filtered.isEmpty()) pick = filtered.get(0);
                if (pick != null) {
                    commit(pick);
                    combo.hide();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                restoreSelectedText();
                combo.hide();
            }
        });

        // Rời ô mà không chọn: trả lại chữ của người đang áp dụng; xóa trống = Tất cả nhân viên
        combo.focusedProperty().addListener((obs, was, focused) -> {
            if (focused) {
                Platform.runLater(() -> combo.getEditor().selectAll());
                return;
            }
            String text = combo.getEditor().getText();
            if (text == null || text.isBlank()) {
                commit(EmployeeOption.ALL);
            } else if (!combo.isShowing()) {
                restoreSelectedText();
            }
        });
    }

    void setEmployees(List<EmployeeOption> employees) {
        updating = true;
        try {
            master.setAll(EmployeeOption.ALL);
            master.addAll(employees);
        } finally {
            updating = false;
        }
        resetFilter();
        EmployeeOption applied = master
            .stream()
            .filter(o -> Objects.equals(o.id(), committedId))
            .findFirst()
            .orElse(EmployeeOption.ALL);
        setSelected(applied);
    }

    private void commit(EmployeeOption option) {
        boolean changed = !Objects.equals(committedId, option.id());
        committedId = option.id();
        resetFilter();
        setSelected(option);
        if (changed) onChanged.accept(option.id());
    }

    private void setSelected(EmployeeOption option) {
        boolean was = updating;
        updating = true;
        try {
            combo.setValue(option);
            combo.getEditor().setText(option.label());
        } finally {
            updating = was;
        }
    }

    private void restoreSelectedText() {
        EmployeeOption applied = master
            .stream()
            .filter(o -> Objects.equals(o.id(), committedId))
            .findFirst()
            .orElse(EmployeeOption.ALL);
        resetFilter();
        setSelected(applied);
    }

    private void resetFilter() {
        setPredicate(o -> true, null);
    }

    private void applyFilter(String text) {
        List<String> tokens = tokens(text);
        setPredicate(o -> {
            if (tokens.isEmpty()) return true;
            String haystack = normalize(o.id() == null ? o.fullName() : o.code() + " " + o.fullName());
            return tokens.stream().allMatch(haystack::contains);
        }, text);
    }

    // Đổi danh sách lọc làm ComboBox xóa lựa chọn và chữ trong ô → giữ lại chữ người dùng đang gõ
    private void setPredicate(Predicate<EmployeeOption> predicate, String keepText) {
        boolean was = updating;
        updating = true;
        try {
            filtered.setPredicate(predicate);
            if (keepText != null && !keepText.equals(combo.getEditor().getText())) {
                combo.getEditor().setText(keepText);
                combo.getEditor().positionCaret(keepText.length());
            }
        } finally {
            updating = was;
        }
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(normalize(text).split("\\s+"))
            .filter(t -> !t.isEmpty())
            .toList();
    }

    // Bỏ dấu tiếng Việt và hạ chữ thường để gõ "hieu" vẫn ra "Hiếu"
    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
