package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Ô chọn thuốc theo mã hoặc tên (kèm trạng thái hạn dùng); chọn xong có thể lấy thuốc qua {@link #selectedMedicineProperty()}. */
public class SearchableMedicinePicker extends SearchablePicker<MedicineInventoryDTO> {

    private final ObjectProperty<MedicineInventoryDTO> selectedMedicine = new SimpleObjectProperty<>(null);
    private final Label lblNoResult = new Label("Không tìm thấy thuốc phù hợp");

    public SearchableMedicinePicker() {
        this("Nhập mã thuốc hoặc tên thuốc...");
    }

    public SearchableMedicinePicker(String promptText) {
        super(promptText, "medicine-picker-list", "medicine-picker-box", 38.0, 220.0);
        lblNoResult.setStyle(
            "-fx-font-size: 12.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #94A3B8; -fx-padding: 12 16;"
        );
        lblNoResult.setAlignment(Pos.CENTER);
        lblNoResult.setMaxWidth(Double.MAX_VALUE);
    }

    public void setMedicines(List<MedicineInventoryDTO> list) {
        setMasterItems(list);
    }

    public void setSelectedMedicine(MedicineInventoryDTO dto) {
        if (dto != null) {
            applySelection(dto);
        } else {
            clear();
        }
    }

    public ObjectProperty<MedicineInventoryDTO> selectedMedicineProperty() {
        return selectedMedicine;
    }

    public MedicineInventoryDTO getSelectedMedicine() {
        return selectedMedicine.get();
    }

    @Override
    protected HBox renderRow(MedicineInventoryDTO item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblCode = new Label(item.getMedicineCode() != null ? item.getMedicineCode() : "");
        lblCode.setStyle(
            "-fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt Bold'; -fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-background-radius: 4px; -fx-padding: 2 8;"
        );

        Label lblName = new Label(item.getMedicineName() != null ? item.getMedicineName() : "");
        lblName.setStyle("-fx-font-size: 13.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #0F172A;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblExpiry = new Label();
        MedicineInventoryDTO.ExpiryStatus expStatus = item.getExpiryStatus();
        if (expStatus == null) {
            expStatus = MedicineInventoryDTO.ExpiryStatus.NORMAL;
        }
        lblExpiry.setText(expStatus.getLabel());
        lblExpiry.getStyleClass().add("status-badge");
        switch (expStatus) {
            case NORMAL -> {
                lblExpiry.getStyleClass().add("status-badge-normal");
                lblExpiry.setStyle(
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-alignment: CENTER;"
                );
            }
            case NEAR_EXPIRY -> {
                lblExpiry.getStyleClass().add("status-badge-near-expiry");
                lblExpiry.setStyle(
                    "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-alignment: CENTER;"
                );
            }
            case EXPIRED -> {
                lblExpiry.getStyleClass().add("status-badge-expired");
                lblExpiry.setStyle(
                    "-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-alignment: CENTER;"
                );
            }
        }

        row.getChildren().addAll(lblCode, lblName, lblExpiry);
        return row;
    }

    @Override
    protected boolean matches(MedicineInventoryDTO med, String kw, String kwNoAccent) {
        String code = med.getMedicineCode() != null ? med.getMedicineCode().toLowerCase() : "";
        String name = med.getMedicineName() != null ? med.getMedicineName().toLowerCase() : "";
        String nameNoAccent = FormatUtils.removeAccents(med.getMedicineName());
        String expLabel = med.getExpiryStatus() != null ? med.getExpiryStatus().getLabel().toLowerCase() : "";
        String expLabelNoAccent = FormatUtils.removeAccents(expLabel);
        return (
            code.contains(kw) ||
            name.contains(kw) ||
            nameNoAccent.contains(kwNoAccent) ||
            expLabel.contains(kw) ||
            expLabelNoAccent.contains(kwNoAccent)
        );
    }

    @Override
    protected String displayText(MedicineInventoryDTO dto) {
        return dto.getMedicineCode() + " - " + dto.getMedicineName();
    }

    @Override
    protected void onSelected(MedicineInventoryDTO dto) {
        selectedMedicine.set(dto);
    }

    @Override
    protected void onTextEmptied() {
        selectedMedicine.set(null);
    }

    @Override
    protected Node emptyResultNode() {
        return lblNoResult;
    }
}
