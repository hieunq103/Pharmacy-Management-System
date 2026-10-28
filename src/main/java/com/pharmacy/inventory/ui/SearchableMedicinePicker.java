package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.List;

/**
 * Component ô tìm kiếm chọn thuốc:
 * - Giao diện thanh tìm kiếm chuyên nghiệp: có icon kính lúp, prompt text, nút xóa nhanh.
 * - Cơ chế Debounce 300ms: người dùng nhập và dừng tay thì danh sách kết quả gợi ý tự động hiện ra ngay phía dưới.
 * - Hỗ trợ tìm kiếm theo Mã thuốc, Tên thuốc và Hoạt chất (hỗ trợ cả gõ tiếng Việt có dấu và không dấu).
 * - Danh sách gợi ý hiển thị trực quan thông tin mã, tên, tồn kho, đơn vị, giá bán.
 */
public class SearchableMedicinePicker extends VBox {

    private final TextField txtSearch = new TextField();
    private final Button btnClear = new Button("✕");
    private final ListView<MedicineInventoryDTO> listView = new ListView<>();
    private final VBox resultsContainer = new VBox();
    private final Label lblNoResult = new Label("Không tìm thấy thuốc phù hợp");

    private final ObservableList<MedicineInventoryDTO> masterData = FXCollections.observableArrayList();
    private final FilteredList<MedicineInventoryDTO> filteredData;
    private final ObjectProperty<MedicineInventoryDTO> selectedMedicine = new SimpleObjectProperty<>(null);
    private final HBox inputContainer = new HBox(8);
    private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));

    private boolean isUpdatingText = false;

    public SearchableMedicinePicker() {
        this("Nhập mã thuốc hoặc tên thuốc...");
    }

    public SearchableMedicinePicker(String promptText) {
        setSpacing(4);
        setAlignment(Pos.TOP_LEFT);

        filteredData = new FilteredList<>(masterData, p -> true);
        listView.setItems(filteredData);
        listView.getStyleClass().add("medicine-picker-list");
        listView.setPrefHeight(180);
        listView.setMaxHeight(240);

        // Thiết kế giao diện từng dòng kết quả trong danh sách: MÃ THUỐC + TÊN THUỐC + TRẠNG THÁI HẠN DÙNG BÊN PHẢI
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 7 10; -fx-cursor: hand;");

                    Label lblCode = new Label(item.getMedicineCode() != null ? item.getMedicineCode() : "");
                    lblCode.setStyle("-fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt SemiBold'; -fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-background-radius: 4px; -fx-padding: 2 8;");

                    Label lblName = new Label(item.getMedicineName() != null ? item.getMedicineName() : "");
                    lblName.setStyle("-fx-font-size: 13.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #0F172A;");
                    HBox.setHgrow(lblName, Priority.ALWAYS);

                    // Trạng thái hạn dùng ở bên phải tên thuốc (css giống bảng kho thuốc)
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
                            lblExpiry.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt'; -fx-alignment: CENTER;");
                        }
                        case NEAR_EXPIRY -> {
                            lblExpiry.getStyleClass().add("status-badge-near-expiry");
                            lblExpiry.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt'; -fx-alignment: CENTER;");
                        }
                        case EXPIRED -> {
                            lblExpiry.getStyleClass().add("status-badge-expired");
                            lblExpiry.setStyle("-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-background-radius: 14px; -fx-padding: 2 10; -fx-font-size: 11.5px; -fx-font-family: 'Inter 18pt'; -fx-alignment: CENTER;");
                        }
                    }

                    row.getChildren().addAll(lblCode, lblName, lblExpiry);

                    // Xử lý chọn thuốc ngay khi chuột nhấn xuống để không bị trễ hoặc mất sự kiện
                    row.setOnMousePressed(e -> {
                        applySelection(item);
                        e.consume();
                    });

                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // 1. Icon kính lúp tìm kiếm ở đầu ô nhập
        SVGPath svgSearch = new SVGPath();
        svgSearch.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        svgSearch.setFill(Color.web("#94A3B8"));

        // 2. Ô nhập tìm kiếm
        txtSearch.setPromptText(promptText);
        txtSearch.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 13.5px; -fx-font-family: 'Inter 18pt'; -fx-text-fill: #0F172A; -fx-padding: 4 0;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        // 3. Nút xóa nhanh nội dung tìm kiếm
        btnClear.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-family: 'Inter 18pt SemiBold'; -fx-padding: 2 6;");
        btnClear.setVisible(false);
        btnClear.setManaged(false);
        btnClear.setOnAction(e -> clear());

        // 4. Khung chứa thanh tìm kiếm (giống như nút tìm kiếm chuyên nghiệp)
        inputContainer.setAlignment(Pos.CENTER_LEFT);
        inputContainer.getStyleClass().add("medicine-picker-box");
        inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;");
        inputContainer.getChildren().addAll(svgSearch, txtSearch, btnClear);

        // Hiệu ứng focus đường viền
        txtSearch.focusedProperty().addListener((obs, oldV, isFocused) -> {
            if (isFocused) {
                if (!inputContainer.getStyleClass().contains("form-input-error")) {
                    inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #0066FF; -fx-border-width: 1.2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12; -fx-effect: dropshadow(three-pass-box, rgba(0, 102, 255, 0.15), 4, 0, 0, 0);");
                }
                if (!txtSearch.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                    showResults();
                }
            } else {
                if (!inputContainer.getStyleClass().contains("form-input-error")) {
                    inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12; -fx-effect: null;");
                }
            }
        });

        // Tự động ẩn danh sách khi người dùng click chuột ra ngoài picker
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> {
                    if (!resultsContainer.isVisible()) return;
                    javafx.geometry.Point2D clickPoint = new javafx.geometry.Point2D(evt.getSceneX(), evt.getSceneY());
                    javafx.geometry.Point2D localPoint = sceneToLocal(clickPoint);
                    if (localPoint == null || !contains(localPoint)) {
                        hideResults();
                    }
                });
            }
        });

        txtSearch.setOnMouseClicked(e -> {
            if (!txtSearch.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                showResults();
            }
        });

        // 5. Khung danh sách kết quả xổ xuống ở ngay dưới ô tìm kiếm
        lblNoResult.setStyle("-fx-font-size: 12.5px; -fx-font-family: 'Inter 18pt'; -fx-text-fill: #94A3B8; -fx-padding: 12 16;");
        lblNoResult.setAlignment(Pos.CENTER);
        lblNoResult.setMaxWidth(Double.MAX_VALUE);

        resultsContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4); -fx-padding: 2;");
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);

        getChildren().addAll(inputContainer, resultsContainer);

        // 6. Lắng nghe khi gõ vào ô tìm kiếm với cơ chế Debounce 300ms (dừng tay thì hiện kết quả ở dưới)
        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingText) return;

            clearErrorStyle();
            String rawKeyword = newV != null ? newV.trim() : "";
            if (rawKeyword.isEmpty()) {
                debounceTimer.stop();
                filteredData.setPredicate(p -> true);
                selectedMedicine.set(null);
                btnClear.setVisible(false);
                btnClear.setManaged(false);
                hideResults();
            } else {
                btnClear.setVisible(true);
                btnClear.setManaged(true);

                // Khởi động bộ đếm 300ms sau khi người dùng dừng tay
                debounceTimer.setOnFinished(ev -> {
                    String kw = rawKeyword.toLowerCase();
                    String kwNoAccent = removeAccents(rawKeyword);

                    filteredData.setPredicate(med -> {
                        if (med == null) return false;
                        String code = med.getMedicineCode() != null ? med.getMedicineCode().toLowerCase() : "";
                        String name = med.getMedicineName() != null ? med.getMedicineName().toLowerCase() : "";
                        String nameNoAccent = removeAccents(med.getMedicineName());
                        String expLabel = (med.getExpiryStatus() != null) ? med.getExpiryStatus().getLabel().toLowerCase() : "";
                        String expLabelNoAccent = removeAccents(expLabel);

                        return code.contains(kw)
                                || name.contains(kw) || nameNoAccent.contains(kwNoAccent)
                                || expLabel.contains(kw) || expLabelNoAccent.contains(kwNoAccent);
                    });

                    showResults();
                });
                debounceTimer.playFromStart();
            }
        });

        // 7. Thao tác chọn thuốc trong danh sách
        listView.setOnMouseClicked(e -> {
            MedicineInventoryDTO selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                applySelection(selected);
            }
        });

        // Phím điều hướng: Mũi tên xuống để vào danh sách, Enter để chọn, Escape để đóng
        txtSearch.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!resultsContainer.isVisible()) {
                    showResults();
                }
                listView.requestFocus();
                if (!filteredData.isEmpty()) {
                    listView.getSelectionModel().select(0);
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                if (resultsContainer.isVisible() && !filteredData.isEmpty()) {
                    MedicineInventoryDTO selected = listView.getSelectionModel().getSelectedItem();
                    if (selected == null) selected = filteredData.get(0);
                    applySelection(selected);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                MedicineInventoryDTO selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    applySelection(selected);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
                txtSearch.requestFocus();
            }
        });
    }

    private void showResults() {
        resultsContainer.getChildren().clear();
        int count = filteredData.size();
        if (count == 0) {
            resultsContainer.getChildren().add(lblNoResult);
            resultsContainer.setVisible(true);
            resultsContainer.setManaged(true);
        } else {
            double itemHeight = 38.0;
            double calculatedHeight = Math.min(Math.max(count * itemHeight + 6, 42), 220);
            listView.setPrefHeight(calculatedHeight);
            resultsContainer.getChildren().add(listView);
            resultsContainer.setVisible(true);
            resultsContainer.setManaged(true);
        }
    }

    private void hideResults() {
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);
    }

    public void setMedicines(List<MedicineInventoryDTO> list) {
        masterData.setAll(list != null ? list : List.of());
        filteredData.setPredicate(p -> true);
    }

    public void clear() {
        isUpdatingText = true;
        debounceTimer.stop();
        txtSearch.clear();
        selectedMedicine.set(null);
        btnClear.setVisible(false);
        btnClear.setManaged(false);
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public void setSelectedMedicine(MedicineInventoryDTO dto) {
        if (dto != null) {
            applySelection(dto);
        } else {
            clear();
        }
    }

    private void applySelection(MedicineInventoryDTO dto) {
        isUpdatingText = true;
        debounceTimer.stop();
        txtSearch.setText(dto.getMedicineCode() + " - " + dto.getMedicineName());
        selectedMedicine.set(dto);
        btnClear.setVisible(true);
        btnClear.setManaged(true);
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public void showErrorStyle() {
        inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #EF4444; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;");
        if (!inputContainer.getStyleClass().contains("form-input-error")) {
            inputContainer.getStyleClass().add("form-input-error");
        }
    }

    public void clearErrorStyle() {
        inputContainer.getStyleClass().remove("form-input-error");
        inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;");
    }

    public ObjectProperty<MedicineInventoryDTO> selectedMedicineProperty() {
        return selectedMedicine;
    }

    public ObjectProperty<MedicineInventoryDTO> selectedItemProperty() {
        return selectedMedicine;
    }

    public MedicineInventoryDTO getSelectedMedicine() {
        return selectedMedicine.get();
    }

    /**
     * Chuyển đổi chuỗi tiếng Việt có dấu thành không dấu để hỗ trợ tìm kiếm linh hoạt
     */
    private static String removeAccents(String s) {
        return FormatUtils.removeAccents(s);
    }
}
