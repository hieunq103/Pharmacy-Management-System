package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Ô chọn một chuỗi từ danh sách gợi ý (nhà cung cấp, nhóm thuốc); vẫn cho nhập giá trị mới. */
public class SearchableStringPicker extends SearchablePicker<String> {

    public SearchableStringPicker() {
        this("Nhập hoặc tìm kiếm...");
    }

    public SearchableStringPicker(String promptText) {
        super(promptText, "string-picker-list", "string-picker-box", 34.0, 180.0);
    }

    public void setItems(List<String> list) {
        setMasterItems(list);
    }

    @Override
    protected HBox renderRow(String item) {
        HBox row = new HBox(6);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lbl = new Label(item);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #1E293B;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        row.getChildren().add(lbl);
        return row;
    }

    @Override
    protected boolean matches(String item, String kwLower, String kwNoAccent) {
        return item.toLowerCase().contains(kwLower) || FormatUtils.removeAccents(item).contains(kwNoAccent);
    }

    @Override
    protected String displayText(String item) {
        return item;
    }
}
