package com.pharmacy.report;

import com.pharmacy.employee.dto.EmployeeOption;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
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
    private boolean updating;

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

        // Gõ → lọc danh sách và mở popup (bỏ qua khi chương trình tự điền chữ vào ô)
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

        combo.setOnAction(e -> {
            if (updating) return;
            EmployeeOption chosen = combo.getValue();
            if (chosen != null) commit(chosen);
        });

        combo.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                // Enter: chọn dòng đầu tiên đang hiển thị (hoặc đúng nhân viên vừa gõ)
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

        // Rời ô mà không chọn: trả lại chữ của người đang được chọn; xóa trống = Tất cả nhân viên
        combo.focusedProperty().addListener((obs, was, focused) -> {
            if (focused) {
                Platform.runLater(() -> combo.getEditor().selectAll());
                return;
            }
            String text = combo.getEditor().getText();
            if (text == null || text.isBlank()) {
                commit(EmployeeOption.ALL);
            } else {
                restoreSelectedText();
            }
        });
    }

    void setEmployees(List<EmployeeOption> employees) {
        EmployeeOption selected = combo.getValue();
        master.setAll(EmployeeOption.ALL);
        master.addAll(employees);
        filtered.setPredicate(o -> true);
        setSelected(selected != null ? selected : EmployeeOption.ALL);
    }

    private void commit(EmployeeOption option) {
        boolean changed = !sameId(combo.getValue(), option);
        setSelected(option);
        filtered.setPredicate(o -> true);
        if (changed) onChanged.accept(option.id());
    }

    private void setSelected(EmployeeOption option) {
        updating = true;
        try {
            combo.setValue(option);
            combo.getEditor().setText(option.label());
        } finally {
            updating = false;
        }
    }

    private void restoreSelectedText() {
        EmployeeOption current = combo.getValue() != null ? combo.getValue() : EmployeeOption.ALL;
        updating = true;
        try {
            combo.getEditor().setText(current.label());
        } finally {
            updating = false;
        }
        filtered.setPredicate(o -> true);
    }

    private void applyFilter(String text) {
        List<String> tokens = tokens(text);
        filtered.setPredicate(o -> {
            if (tokens.isEmpty()) return true;
            String haystack = normalize(o.id() == null ? o.fullName() : o.code() + " " + o.fullName());
            return tokens.stream().allMatch(haystack::contains);
        });
    }

    private static boolean sameId(EmployeeOption a, EmployeeOption b) {
        Integer x = a != null ? a.id() : null;
        Integer y = b != null ? b.id() : null;
        return java.util.Objects.equals(x, y);
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        return java.util.Arrays.stream(normalize(text).split("\\s+"))
            .filter(t -> !t.isEmpty())
            .toList();
    }

    // Bỏ dấu tiếng Việt và hạ chữ thường để gõ "hieu" vẫn ra "Hiếu"
    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
