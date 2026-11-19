package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Ô nhập có gợi ý dạng danh sách (gõ để lọc, chọn bằng chuột hoặc bàn phím). Lớp nền dùng chung cho ô chọn chuỗi
 * (nhà cung cấp, nhóm thuốc) và ô chọn thuốc; lớp con chỉ khai báo cách hiển thị một dòng, cách so khớp và việc cần làm khi chọn.
 */
abstract class SearchablePicker<T> extends VBox {

    private static final String BOX_STYLE_IDLE =
        "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;";
    private static final String BOX_STYLE_FOCUSED =
        "-fx-background-color: #FFFFFF; -fx-border-color: #0066FF; -fx-border-width: 1.2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12; -fx-effect: dropshadow(three-pass-box, rgba(0, 102, 255, 0.15), 4, 0, 0, 0);";
    private static final String BOX_STYLE_ERROR =
        "-fx-background-color: #FFFFFF; -fx-border-color: #EF4444; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3 10 3 12;";

    protected final TextField txtInput = new TextField();
    private final Button btnClear = new Button("✕");
    private final ListView<T> listView = new ListView<>();
    private final VBox resultsContainer = new VBox();
    private final ObservableList<T> masterData = FXCollections.observableArrayList();
    private final FilteredList<T> filteredData;
    private final HBox inputContainer = new HBox(8);
    private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));
    private final double rowHeight;
    private final double maxListHeight;
    private boolean isUpdatingText = false;

    /**
     * @param listStyleClass  class CSS của danh sách
     * @param boxStyleClass   class CSS của khung nhập
     * @param rowHeight       chiều cao ước tính mỗi dòng (để tính chiều cao danh sách)
     * @param maxListHeight   chiều cao tối đa của danh sách
     */
    protected SearchablePicker(
        String promptText,
        String listStyleClass,
        String boxStyleClass,
        double rowHeight,
        double maxListHeight
    ) {
        this.rowHeight = rowHeight;
        this.maxListHeight = maxListHeight;
        setSpacing(4);
        setAlignment(Pos.TOP_LEFT);

        filteredData = new FilteredList<>(masterData, p -> true);
        listView.setItems(filteredData);
        listView.getStyleClass().add(listStyleClass);
        listView.setPrefHeight(maxListHeight * 0.8);
        listView.setMaxHeight(maxListHeight + 20);
        listView.setCellFactory(lv ->
            new ListCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        HBox row = renderRow(item);
                        row.setStyle("-fx-padding: 7 10; -fx-cursor: hand;");
                        row.setOnMousePressed(e -> {
                            applySelection(item);
                            e.consume();
                        });
                        setGraphic(row);
                        setText(null);
                    }
                }
            }
        );

        SVGPath svgSearch = new SVGPath();
        svgSearch.setContent(
            "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
        );
        svgSearch.setFill(Color.web("#94A3B8"));

        txtInput.setPromptText(promptText);
        txtInput.setStyle(
            "-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 13.5px; -fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #0F172A; -fx-padding: 4 0;"
        );
        HBox.setHgrow(txtInput, Priority.ALWAYS);

        btnClear.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-family: 'Inter 18pt Bold'; -fx-padding: 2 6;"
        );
        setClearVisible(false);
        btnClear.setOnAction(e -> clear());

        inputContainer.setAlignment(Pos.CENTER_LEFT);
        inputContainer.getStyleClass().add(boxStyleClass);
        inputContainer.setStyle(BOX_STYLE_IDLE);
        inputContainer.getChildren().addAll(svgSearch, txtInput, btnClear);

        txtInput.focusedProperty().addListener((obs, oldV, isFocused) -> {
            if (!inputContainer.getStyleClass().contains("form-input-error")) {
                inputContainer.setStyle(isFocused ? BOX_STYLE_FOCUSED : BOX_STYLE_IDLE + " -fx-effect: null;");
            }
            if (isFocused && !txtInput.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                showResults();
            }
        });

        // Bấm ra ngoài thì đóng danh sách (bộ lọc gắn lên Scene; bỏ khi ô rời khỏi Scene để không giữ tham chiếu)
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
            if (newScene != null) newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
        });

        txtInput.setOnMouseClicked(e -> {
            if (!txtInput.getText().trim().isEmpty() && !resultsContainer.isVisible()) {
                showResults();
            }
        });

        resultsContainer.setStyle(
            "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4); -fx-padding: 2;"
        );
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);

        getChildren().addAll(inputContainer, resultsContainer);

        txtInput.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingText) return;
            clearErrorStyle();
            String rawKeyword = newV != null ? newV.trim() : "";
            if (rawKeyword.isEmpty()) {
                debounceTimer.stop();
                filteredData.setPredicate(p -> true);
                onTextEmptied();
                setClearVisible(false);
                hideResults();
            } else {
                setClearVisible(true);
                debounceTimer.setOnFinished(ev -> {
                    String kw = rawKeyword.toLowerCase();
                    String kwNoAccent = FormatUtils.removeAccents(rawKeyword);
                    filteredData.setPredicate(item -> item != null && matches(item, kw, kwNoAccent));
                    showResults();
                });
                debounceTimer.playFromStart();
            }
        });

        listView.setOnMouseClicked(e -> {
            T selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) applySelection(selected);
        });

        txtInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!resultsContainer.isVisible()) showResults();
                listView.requestFocus();
                if (!filteredData.isEmpty()) listView.getSelectionModel().select(0);
            } else if (e.getCode() == KeyCode.ENTER) {
                if (resultsContainer.isVisible() && !filteredData.isEmpty()) {
                    T selected = listView.getSelectionModel().getSelectedItem();
                    applySelection(selected != null ? selected : filteredData.get(0));
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                T selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) applySelection(selected);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideResults();
                txtInput.requestFocus();
            }
        });
    }

    private final javafx.event.EventHandler<MouseEvent> outsideClickFilter = evt -> {
        if (!resultsContainer.isVisible()) return;
        Point2D local = sceneToLocal(new Point2D(evt.getSceneX(), evt.getSceneY()));
        if (local == null || !contains(local)) hideResults();
    };

    // ---- Phần lớp con phải khai báo ----

    /** Dựng nội dung một dòng gợi ý (lớp nền gắn kiểu dáng chung và sự kiện chọn). */
    protected abstract HBox renderRow(T item);

    /** Dòng có khớp từ khóa không ({@code kwLower}: chữ thường; {@code kwNoAccent}: không dấu). */
    protected abstract boolean matches(T item, String kwLower, String kwNoAccent);

    /** Chữ điền vào ô nhập khi chọn một dòng. */
    protected abstract String displayText(T item);

    /** Được gọi sau khi chọn một dòng. */
    protected void onSelected(T item) {}

    /** Được gọi khi ô nhập bị xóa trống hoặc gọi {@link #clear()}. */
    protected void onTextEmptied() {}

    /** Nút hiển thị khi lọc không ra dòng nào; null nghĩa là ẩn danh sách. */
    protected Node emptyResultNode() {
        return null;
    }

    // ---- Phần dùng chung ----

    private void showResults() {
        resultsContainer.getChildren().clear();
        int count = filteredData.size();
        if (count > 0) {
            listView.setPrefHeight(Math.min(Math.max(count * rowHeight + 6, 40), maxListHeight));
            resultsContainer.getChildren().add(listView);
        } else if (emptyResultNode() != null) {
            resultsContainer.getChildren().add(emptyResultNode());
        } else {
            hideResults();
            return;
        }
        resultsContainer.setVisible(true);
        resultsContainer.setManaged(true);
    }

    private void hideResults() {
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);
    }

    private void setClearVisible(boolean visible) {
        btnClear.setVisible(visible);
        btnClear.setManaged(visible);
    }

    protected final void setMasterItems(List<T> list) {
        masterData.setAll(list != null ? list : List.of());
        filteredData.setPredicate(p -> true);
    }

    protected final void applySelection(T item) {
        isUpdatingText = true;
        debounceTimer.stop();
        txtInput.setText(displayText(item));
        onSelected(item);
        setClearVisible(true);
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public void clear() {
        isUpdatingText = true;
        debounceTimer.stop();
        txtInput.clear();
        onTextEmptied();
        setClearVisible(false);
        clearErrorStyle();
        hideResults();
        isUpdatingText = false;
    }

    public String getText() {
        return txtInput.getText() != null ? txtInput.getText().trim() : "";
    }

    public StringProperty textProperty() {
        return txtInput.textProperty();
    }

    public void showErrorStyle() {
        inputContainer.setStyle(BOX_STYLE_ERROR);
        if (!inputContainer.getStyleClass().contains("form-input-error")) {
            inputContainer.getStyleClass().add("form-input-error");
        }
    }

    public void clearErrorStyle() {
        inputContainer.getStyleClass().remove("form-input-error");
        inputContainer.setStyle(BOX_STYLE_IDLE);
    }
}
