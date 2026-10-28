package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.StringProperty;
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
 * Ô nhập liệu tìm kiếm gợi ý linh hoạt
 * - Có icon tìm kiếm, nút xoá nhanh ✕.
 * - Hỗ trợ tìm kiếm theo danh sách đã có (Nhà cung cấp, Nhóm thuốc...) cả tiếng Việt có dấu và không dấu.
 * - Cơ chế Debounce 300ms: người dùng nhập và dừng tay thì danh sách gợi ý xổ xuống ngay dưới ô nhập liệu.
 * - Khi không có kết quả gợi ý trùng khớp, danh sách ẩn đi, giữ nguyên giá trị người dùng gõ để tự động tạo mới.
 */
public class SearchableStringPicker extends VBox {

    private final TextField txtInput = new TextField();
    private final Button btnClear = new Button("✕");
    private final ListView<String> listView = new ListView<>();
    private final VBox resultsContainer = new VBox();

    private final ObservableList<String> masterData = FXCollections.observableArrayList();
    private final FilteredList<String> filteredData;
    private final HBox inputContainer = new HBox(8);
    private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));

    private boolean isUpdatingText = false;

    public SearchableStringPicker() {
        this("Nhập hoặc tìm kiếm...");
    }

    public SearchableStringPicker(String promptText) {
        setSpacing(4);
        setAlignment(Pos.TOP_LEFT);

        filteredData = new FilteredList<>(masterData, p -> true);
        listView.setItems(filteredData);
        listView.getStyleClass().add("string-picker-list");
        listView.setPrefHeight(150);
        listView.setMaxHeight(200);

        // Hiển thị từng dòng gợi ý
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 7 10; -fx-cursor: hand;");

                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-size: 13px; -fx-font-family: 'Inter 18pt'; -fx-text-fill: #1E293B;");
                    HBox.setHgrow(lbl, Priority.ALWAYS);

                    row.getChildren().add(lbl);

                    // Chọn ngay khi nhấn chuột
                    row.setOnMousePressed(e -> {
                        applySelection(item);
                        e.consume();
                    });

                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // Icon kính lúp tìm kiếm ở đầu ô nhập
        SVGPath svgSearch = new SVGPath();
        svgSearch.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        svgSearch.setFill(Color.web("#94A3B8"));

        // Ô nhập dữ liệu
        txtInput.setPromptText(promptText);
        txtInput.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 13.5px; -fx-font-family: 'Inter 18pt'; -fx-text-fill: #0F172A; -fx-padding: 4 0;");
        HBox.setHgrow(txtInput, Priority.ALWAYS);

        // Nút xóa nhanh
        btnClear.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-family: 'Inter 18pt SemiBold'; -fx-padding: 2 6;");
        btnClear.setVisible(false);
        btnClear.setManaged(false);
        btnClear.setOnAction(e -> clear());

        // Khung chứa ô nhập
        inputContainer.setAlignment(Pos.CENTER_LEFT);
        inputContainer.getStyleClass().add("string-picker-box");
        inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;");
        inputContainer.getChildren().addAll(svgSearch, txtInput, btnClear);

        // Hiệu ứng focus
        txtInput.focusedProperty().addListener((obs, oldV, isFocused) -> {
            if (isFocused) {
                if (!inputContainer.getStyleClass().contains("form-input-error")) {
                    inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #0066FF; -fx-border-width: 1.2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12; -fx-effect: dropshadow(three-pass-box, rgba(0, 102, 255, 0.15), 4, 0, 0, 0);");
                }
                if (!txtInput.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                    showResults();
                }
            } else {
                if (!inputContainer.getStyleClass().contains("form-input-error")) {
                    inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12; -fx-effect: null;");
                }
            }
        });

        // Tự động ẩn gợi ý khi người dùng click ra ngoài
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

        txtInput.setOnMouseClicked(e -> {
            if (!txtInput.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                showResults();
            }
        });

        // Khung danh sách gợi ý xổ xuống
        resultsContainer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4); -fx-padding: 2;");
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);

        getChildren().addAll(inputContainer, resultsContainer);

        // Lắng nghe khi gõ với Debounce 300ms
        txtInput.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingText) return;

            clearErrorStyle();
            String rawKeyword = newV != null ? newV.trim() : "";
            if (rawKeyword.isEmpty()) {
                debounceTimer.stop();
                filteredData.setPredicate(p -> true);
                btnClear.setVisible(false);
                btnClear.setManaged(false);
                hideResults();
            } else {
                btnClear.setVisible(true);
                btnClear.setManaged(true);

                debounceTimer.setOnFinished(ev -> {
                    String kw = rawKeyword.toLowerCase();
                    String kwNoAccent = removeAccents(rawKeyword);

                    filteredData.setPredicate(item -> {
                        if (item == null) return false;
                        String itemLower = item.toLowerCase();
                        String itemNoAccent = removeAccents(item);
                        return itemLower.contains(kw) || itemNoAccent.contains(kwNoAccent);
                    });

                    // Nếu có kết quả trùng khớp thì hiện gợi ý, không có thì ẩn để người dùng tự do nhập mới
                    if (!filteredData.isEmpty()) {
                        showResults();
                    } else {
                        hideResults();
                    }
                });
                debounceTimer.playFromStart();
            }
        });

        // Bấm chọn từ danh sách
        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                applySelection(selected);
            }
        });

        // Bàn phím điều hướng
        txtInput.setOnKeyPressed(e -> {
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
                    String selected = listView.getSelectionModel().getSelectedItem();
                    if (selected == null) selected = filteredData.get(0);
                    applySelection(selected);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    applySelection(selected);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
                txtInput.requestFocus();
            }
        });
    }

    private void showResults() {
        resultsContainer.getChildren().clear();
        int count = filteredData.size();
        if (count > 0) {
            double calculatedHeight = Math.min(Math.max(count * 34.0 + 6, 40), 180);
            listView.setPrefHeight(calculatedHeight);
            resultsContainer.getChildren().add(listView);
            resultsContainer.setVisible(true);
            resultsContainer.setManaged(true);
        } else {
            hideResults();
        }
    }

    private void hideResults() {
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);
    }

    public void setItems(List<String> list) {
        masterData.setAll(list != null ? list : List.of());
        filteredData.setPredicate(p -> true);
    }

    public void clear() {
        isUpdatingText = true;
        debounceTimer.stop();
        txtInput.clear();
        btnClear.setVisible(false);
        btnClear.setManaged(false);
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public void setText(String value) {
        isUpdatingText = true;
        debounceTimer.stop();
        txtInput.setText(value != null ? value : "");
        btnClear.setVisible(value != null && !value.isBlank());
        btnClear.setManaged(value != null && !value.isBlank());
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public String getText() {
        return txtInput.getText() != null ? txtInput.getText().trim() : "";
    }

    public String getValue() {
        return getText();
    }

    public StringProperty textProperty() {
        return txtInput.textProperty();
    }

    private void applySelection(String value) {
        isUpdatingText = true;
        debounceTimer.stop();
        txtInput.setText(value);
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

    private static String removeAccents(String s) {
        return FormatUtils.removeAccents(s);
    }
}
