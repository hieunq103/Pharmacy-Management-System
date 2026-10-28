package com.pharmacy.inventory.ui;

import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.InventoryServiceImpl;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
import com.pharmacy.inventory.dto.BatchExportItemDTO;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ExportDialogController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ExportDialogController.class);

    private static final List<String> EXPORT_REASONS = List.of(
            "Thuốc hết hạn sử dụng (Tiêu hủy)",
            "Thuốc hư hỏng, vỡ bao bì, biến chất",
            "Thu hồi theo quyết định của Cục Quản lý Dược",
            "Chuyển trả Nhà cung cấp",
            "Xuất mẫu kiểm nghiệm / kiểm định chất lượng",
            "Điều chuyển nội bộ / Lý do khác"
    );

    @FXML private TextField txtNote;
    @FXML private Label lblEmployeeName;

    @FXML private StackPane medicinePickerContainer;
    @FXML private Label lblErrorMedicine;
    @FXML private HBox boxMedicineInfo;
    @FXML private Label lblMedCode;
    @FXML private Label lblMedName;
    @FXML private Label lblMedCategory;
    @FXML private Label lblMedTotalStock;

    @FXML private VBox lotRowsContainer;
    @FXML private Button btnSelectAnotherLot;

    @FXML private Label lblItemError;

    @FXML private Label lblItemCountBadge;
    @FXML private Button btnClearAllItems;
    @FXML private TableView<BatchExportItemDTO> tableItems;
    @FXML private TableColumn<BatchExportItemDTO, Integer> colIndex;
    @FXML private TableColumn<BatchExportItemDTO, String> colCode;
    @FXML private TableColumn<BatchExportItemDTO, String> colName;
    @FXML private TableColumn<BatchExportItemDTO, String> colCategory;
    @FXML private TableColumn<BatchExportItemDTO, String> colLot;
    @FXML private TableColumn<BatchExportItemDTO, String> colExpiry;
    @FXML private TableColumn<BatchExportItemDTO, String> colUnit;
    @FXML private TableColumn<BatchExportItemDTO, Integer> colStock;
    @FXML private TableColumn<BatchExportItemDTO, Integer> colQuantity;
    @FXML private TableColumn<BatchExportItemDTO, String> colReason;
    @FXML private TableColumn<BatchExportItemDTO, BatchExportItemDTO> colAction;

    @FXML private Label lblGeneralError;
    @FXML private Button btnConfirmExport;

    private SearchableMedicinePicker medicinePicker;
    private InventoryRepository repository;
    private InventoryService inventoryService;
    private Runnable onSuccessCallback;
    private Runnable onCloseCallback;

    private final ObservableList<MedicineLotDTO> currentMedicineLots = FXCollections.observableArrayList();

    private final List<LotRowItem> activeLotRows = new ArrayList<>();

    private final ObservableList<BatchExportItemDTO> itemList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        medicinePicker = new SearchableMedicinePicker("Nhập tên hoặc mã thuốc cần xuất kho...");
        medicinePickerContainer.getChildren().add(medicinePicker);

        medicinePicker.selectedMedicineProperty().addListener((obs, oldV, newV) -> {
            clearPickerError(medicinePicker, lblErrorMedicine);
            onMedicineSelected(newV);
        });

        if (btnSelectAnotherLot != null) {
            btnSelectAnotherLot.setOnMouseEntered(ev -> btnSelectAnotherLot.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 12px; -fx-padding: 7 14; -fx-border-color: #93C5FD; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;"));
            btnSelectAnotherLot.setOnMouseExited(ev -> btnSelectAnotherLot.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 12px; -fx-padding: 7 14; -fx-border-color: #BFDBFE; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;"));
        }

        setupTableColumns();
        tableItems.setItems(itemList);
        tableItems.setPlaceholder(new Label("Chưa có mặt hàng nào trong phiếu xuất. Vui lòng thêm thuốc từ phần trên."));

        if (UserSession.isLoggedIn()) {
            lblEmployeeName.setText(UserSession.getInstance().getEmployeeName());
        }

        addNewLotRow(false);

        updateSummary();
    }

    public class LotRowItem {
        private final GridPane rowNode;
        private final ComboBox<MedicineLotDTO> cbLot;
        private final Label lblLotStockInfo;
        private final Label lblErrorLot;

        private final TextField txtQuantity;
        private final Label lblMaxAvailable;
        private final Label lblErrorQuantity;

        private final TextField txtReason;
        private final ContextMenu popupReason;
        private final Label lblErrorReason;

        private final Button btnRemoveRow;

        public LotRowItem() {
            rowNode = new GridPane();
            rowNode.setHgap(12);
            rowNode.setVgap(4);
            rowNode.setAlignment(Pos.CENTER_LEFT);

            ColumnConstraints col0 = new ColumnConstraints();
            col0.setPercentWidth(38.0);
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(22.0);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(35.0);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPercentWidth(5.0);
            rowNode.getColumnConstraints().addAll(col0, col1, col2, col3);

            cbLot = new ComboBox<>(currentMedicineLots);
            cbLot.setMaxWidth(Double.MAX_VALUE);
            cbLot.setPromptText("-- Chọn lô hàng xuất --");
            cbLot.getStyleClass().add("form-combobox");

            lblLotStockInfo = new Label();
            lblLotStockInfo.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
            lblErrorLot = new Label();
            lblErrorLot.getStyleClass().add("form-error-msg");
            lblErrorLot.setVisible(false);
            lblErrorLot.setManaged(false);

            txtQuantity = new TextField();
            txtQuantity.setPromptText("SL...");
            txtQuantity.getStyleClass().add("form-input");

            lblMaxAvailable = new Label("(Tối đa: 0)");
            lblMaxAvailable.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11.5px;");
            lblErrorQuantity = new Label();
            lblErrorQuantity.getStyleClass().add("form-error-msg");
            lblErrorQuantity.setVisible(false);
            lblErrorQuantity.setManaged(false);

            txtReason = new TextField();
            txtReason.setPromptText("-- Nhập hoặc chọn gợi ý lý do --");
            txtReason.getStyleClass().add("form-input");
            txtReason.setStyle("-fx-padding: 7 26 7 10; -fx-font-size: 13px; -fx-font-family: 'Inter 18pt'; -fx-text-fill: #0F172A;");
            txtReason.setMaxWidth(Double.MAX_VALUE);

            popupReason = new ContextMenu();
            popupReason.setAutoHide(true);

            Button btnDropdownReason = new Button("▾");
            btnDropdownReason.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-padding: 4 6; -fx-font-size: 11px;");
            btnDropdownReason.setFocusTraversable(false);
            btnDropdownReason.setOnAction(e -> {
                txtReason.requestFocus();
                showReasonSuggestions(txtReason, popupReason, "");
            });

            StackPane reasonPane = new StackPane(txtReason, btnDropdownReason);
            StackPane.setAlignment(btnDropdownReason, Pos.CENTER_RIGHT);
            StackPane.setMargin(btnDropdownReason, new Insets(0, 4, 0, 0));

            lblErrorReason = new Label();
            lblErrorReason.getStyleClass().add("form-error-msg");
            lblErrorReason.setVisible(false);
            lblErrorReason.setManaged(false);

            txtReason.textProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.length() > 255) {
                    txtReason.setText(newV.substring(0, 255));
                    return;
                }
                clearControlError(txtReason, lblErrorReason);
                if (txtReason.isFocused()) {
                    showReasonSuggestions(txtReason, popupReason, newV != null ? newV : "");
                }
            });

            txtReason.setOnMouseClicked(e -> {
                if (!popupReason.isShowing()) {
                    showReasonSuggestions(txtReason, popupReason, txtReason.getText() != null ? txtReason.getText() : "");
                }
            });

            txtReason.focusedProperty().addListener((obs, oldV, isFocused) -> {
                if (isFocused) {
                    showReasonSuggestions(txtReason, popupReason, txtReason.getText() != null ? txtReason.getText() : "");
                } else {
                    popupReason.hide();
                }
            });

            txtReason.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.DOWN) {
                    if (!popupReason.isShowing()) {
                        showReasonSuggestions(txtReason, popupReason, txtReason.getText() != null ? txtReason.getText() : "");
                    }
                } else if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                    popupReason.hide();
                }
            });

            btnRemoveRow = new Button("✕");
            btnRemoveRow.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 13px; -fx-padding: 6 10; -fx-background-radius: 6px; -fx-cursor: hand;");
            btnRemoveRow.setTooltip(new Tooltip("Xóa dòng lô này"));
            btnRemoveRow.setOnAction(e -> removeLotRow(this));

            setupLotComboBox(cbLot, this);

            cbLot.valueProperty().addListener((obs, oldV, newV) -> {
                clearControlError(cbLot, lblErrorLot);
                updateRowLotInfo(this);
                updateAllRowsAvailableDisplay();
            });

            txtQuantity.textProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && !newV.matches("\\d*")) {
                    txtQuantity.setText(newV.replaceAll("[^\\d]", ""));
                    return;
                }
                clearControlError(txtQuantity, lblErrorQuantity);
                updateAllRowsAvailableDisplay();
            });

            VBox boxLot = new VBox(3, cbLot, lblLotStockInfo, lblErrorLot);
            VBox boxQty = new VBox(3, txtQuantity, lblMaxAvailable, lblErrorQuantity);
            VBox boxReason = new VBox(3, reasonPane, lblErrorReason);
            VBox boxAction = new VBox(btnRemoveRow);
            boxAction.setAlignment(Pos.TOP_CENTER);
            boxAction.setPadding(new Insets(1, 0, 0, 0));

            rowNode.add(boxLot, 0, 0);
            rowNode.add(boxQty, 1, 0);
            rowNode.add(boxReason, 2, 0);
            rowNode.add(boxAction, 3, 0);
        }

        public String getReasonText() {
            return txtReason.getText() != null ? txtReason.getText().trim() : "";
        }

        public void clearReason() {
            txtReason.clear();
            popupReason.hide();
        }
    }

    private void showReasonSuggestions(TextField field, ContextMenu menu, String keyword) {
        String kw = removeAccents(keyword).trim();
        List<String> matched = EXPORT_REASONS.stream()
                .filter(r -> kw.isEmpty() || removeAccents(r).contains(kw))
                .toList();

        menu.getItems().clear();
        if (matched.isEmpty()) {
            menu.hide();
            return;
        }

        for (String r : matched) {
            MenuItem item = new MenuItem(r);
            item.setStyle("-fx-font-size: 12.5px; -fx-padding: 6 12;");
            item.setOnAction(e -> {
                field.setText(r);
                field.positionCaret(r.length());
                menu.hide();
            });
            menu.getItems().add(item);
        }

        if (field.getScene() != null && field.getScene().getWindow() != null && field.getScene().getWindow().isShowing()) {
            menu.show(field, Side.BOTTOM, 0, 2);
        }
    }

    private static String removeAccents(String s) {
        return FormatUtils.removeAccents(s);
    }

    private void setupLotComboBox(ComboBox<MedicineLotDTO> combo, LotRowItem row) {
        combo.setCellFactory(lv -> new ListCell<MedicineLotDTO>() {
            @Override
            protected void updateItem(MedicineLotDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    int total = item.getQuantityRemaining() != null ? item.getQuantityRemaining() : 0;
                    int avail = getRemainingAvailableForLot(item.getId(), total, row);
                    String unit = getSelectedMedicineUnit();
                    if (avail <= 0) {
                        setText("Lô " + item.getLotNumber() + " (Đã chọn hết, HSD: " + item.getFormattedExpiryDate() + ")");
                        setStyle("-fx-text-fill: #94A3B8;");
                    } else {
                        setText("Lô " + item.getLotNumber() + " (Còn lại: " + avail + " " + unit + ", HSD: " + item.getFormattedExpiryDate() + ")");
                        setStyle("-fx-text-fill: #0F172A;");
                    }
                }
            }
        });

        combo.setButtonCell(new ListCell<MedicineLotDTO>() {
            @Override
            protected void updateItem(MedicineLotDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int total = item.getQuantityRemaining() != null ? item.getQuantityRemaining() : 0;
                    int avail = getRemainingAvailableForLot(item.getId(), total, row);
                    String unit = getSelectedMedicineUnit();
                    if (avail <= 0) {
                        setText("Lô " + item.getLotNumber() + " (Đã chọn hết, HSD: " + item.getFormattedExpiryDate() + ")");
                    } else {
                        setText("Lô " + item.getLotNumber() + " (Còn lại: " + avail + " " + unit + ", HSD: " + item.getFormattedExpiryDate() + ")");
                    }
                }
            }
        });
    }

    private int getRemainingAvailableForLot(Long lotId, int totalRemainingInLot, LotRowItem currentRow) {
        if (lotId == null) return totalRemainingInLot;

        int inTableItems = itemList.stream()
                .filter(it -> it.getLotId() == lotId)
                .mapToInt(BatchExportItemDTO::getExportQuantity)
                .sum();

        int inOtherRows = 0;
        for (LotRowItem r : activeLotRows) {
            if (r != currentRow && r.cbLot.getValue() != null && lotId.equals(r.cbLot.getValue().getId())) {
                String qStr = r.txtQuantity.getText() != null ? r.txtQuantity.getText().trim() : "";
                if (!qStr.isEmpty()) {
                    try {
                        inOtherRows += Integer.parseInt(qStr);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return Math.max(0, totalRemainingInLot - inTableItems - inOtherRows);
    }

    private void updateAllRowsAvailableDisplay() {
        for (LotRowItem r : activeLotRows) {
            updateRowLotInfo(r);
        }
    }

    private void updateRowLotInfo(LotRowItem r) {
        MedicineLotDTO lot = r.cbLot.getValue();
        String unit = getSelectedMedicineUnit();
        if (lot != null) {
            int total = lot.getQuantityRemaining() != null ? lot.getQuantityRemaining() : 0;
            int avail = getRemainingAvailableForLot(lot.getId(), total, r);
            r.lblMaxAvailable.setText("(Tối đa: " + avail + " " + unit + ")");
            r.lblLotStockInfo.setText("HSD: " + lot.getFormattedExpiryDate() + " | Còn lại: " + avail + " " + unit);
        } else {
            r.lblMaxAvailable.setText("(Tối đa: 0)");
            MedicineInventoryDTO med = (medicinePicker != null) ? medicinePicker.getSelectedMedicine() : null;
            if (med == null) {
                r.lblLotStockInfo.setText("Chọn thuốc để xem các lô hàng có sẵn");
            } else {
                r.lblLotStockInfo.setText("Vui lòng chọn lô xuất");
            }
        }
    }

    private String getSelectedMedicineUnit() {
        MedicineInventoryDTO med = medicinePicker != null ? medicinePicker.getSelectedMedicine() : null;
        return (med != null && med.getUnit() != null) ? med.getUnit() : "đơn vị";
    }

    private void resetToSingleEmptyRow() {
        lotRowsContainer.getChildren().clear();
        activeLotRows.clear();
        addNewLotRow(false);
    }

    private LotRowItem addNewLotRow(boolean autoSelectNextLot) {
        LotRowItem row = new LotRowItem();
        activeLotRows.add(row);
        lotRowsContainer.getChildren().add(row.rowNode);

        if (autoSelectNextLot && !currentMedicineLots.isEmpty()) {
            MedicineLotDTO nextLot = findNextAvailableLotForNewRow();
            if (nextLot != null) {
                row.cbLot.setValue(nextLot);
            } else {
                row.cbLot.setValue(currentMedicineLots.get(0));
            }
        } else if (!currentMedicineLots.isEmpty()) {
            row.cbLot.setValue(currentMedicineLots.get(0));
        }

        updateRowLotInfo(row);
        updateRemoveButtonsVisibility();

        if (autoSelectNextLot) {
            Platform.runLater(() -> row.txtQuantity.requestFocus());
        }

        return row;
    }

    private MedicineLotDTO findNextAvailableLotForNewRow() {
        for (MedicineLotDTO lot : currentMedicineLots) {
            int total = lot.getQuantityRemaining() != null ? lot.getQuantityRemaining() : 0;
            int avail = getRemainingAvailableForLot(lot.getId(), total, null);
            boolean alreadySelected = activeLotRows.stream()
                    .anyMatch(r -> r.cbLot.getValue() != null && lot.getId().equals(r.cbLot.getValue().getId()));
            if (avail > 0 && !alreadySelected) {
                return lot;
            }
        }

        for (MedicineLotDTO lot : currentMedicineLots) {
            int total = lot.getQuantityRemaining() != null ? lot.getQuantityRemaining() : 0;
            int avail = getRemainingAvailableForLot(lot.getId(), total, null);
            if (avail > 0) {
                return lot;
            }
        }
        return null;
    }

    private void removeLotRow(LotRowItem row) {
        if (activeLotRows.size() <= 1) {
            row.txtQuantity.clear();
            row.clearReason();
            return;
        }
        activeLotRows.remove(row);
        lotRowsContainer.getChildren().remove(row.rowNode);
        updateRemoveButtonsVisibility();
        updateAllRowsAvailableDisplay();
    }

    private void updateRemoveButtonsVisibility() {
        boolean canRemove = activeLotRows.size() > 1;
        for (LotRowItem r : activeLotRows) {
            r.btnRemoveRow.setVisible(canRemove);
            r.btnRemoveRow.setManaged(canRemove);
        }
    }

    @FXML
    private void handleSelectAnotherLot() {
        hideItemError();
        MedicineInventoryDTO selectedMed = medicinePicker.getSelectedMedicine();
        if (selectedMed == null) {
            showPickerError(medicinePicker, lblErrorMedicine, "Vui lòng chọn thuốc trước khi xuất thêm lô!");
            return;
        }

        if (currentMedicineLots.isEmpty()) {
            showItemError("Thuốc " + selectedMed.getMedicineName() + " không có lô hàng nào còn tồn!");
            return;
        }

        addNewLotRow(true);
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(tableItems.getItems().indexOf(cellData.getValue()) + 1));
        colIndex.setCellFactory(col -> new TableCell<BatchExportItemDTO, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView() == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicineCode()));
        colCode.setCellFactory(col -> new TableCell<BatchExportItemDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-size: 11px; -fx-font-family: 'Inter 18pt SemiBold'; -fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-background-radius: 4px; -fx-padding: 2 6;");
                    setGraphic(lbl);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));

        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        colLot.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLotNumber()));
        colLot.setCellFactory(col -> new TableCell<BatchExportItemDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #1E293B;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colExpiry.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedExpiryDate()));

        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        colStock.setCellValueFactory(new PropertyValueFactory<>("stockRemaining"));

        colQuantity.setCellValueFactory(new PropertyValueFactory<>("exportQuantity"));
        colQuantity.setCellFactory(col -> new TableCell<BatchExportItemDTO, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #DC2626; -fx-alignment: CENTER_LEFT;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colReason.setCellFactory(com.pharmacy.common.WrappingTextCell.forColumn());

        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAction.setCellFactory(col -> new TableCell<BatchExportItemDTO, BatchExportItemDTO>() {
            private final Button btnDelete = new Button("✕ Xóa");
            {
                btnDelete.setStyle("-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #FECACA; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                btnDelete.setOnMouseEntered(ev -> btnDelete.setStyle("-fx-background-color: #FCA5A5; -fx-text-fill: #991B1B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #F87171; -fx-border-radius: 4px; -fx-background-radius: 4px;"));
                btnDelete.setOnMouseExited(ev -> btnDelete.setStyle("-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #FECACA; -fx-border-radius: 4px; -fx-background-radius: 4px;"));
                btnDelete.setTooltip(new Tooltip("Xóa mặt hàng này khỏi phiếu xuất"));
                btnDelete.setOnAction(e -> {
                    BatchExportItemDTO item = getItem();
                    if (item != null) {
                        itemList.remove(item);
                        updateSummary();
                        tableItems.refresh();
                        updateAllRowsAvailableDisplay();
                    }
                });
            }

            @Override
            protected void updateItem(BatchExportItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    public void initData(List<MedicineInventoryDTO> medicines, InventoryRepository repo, Runnable onSuccess) {
        initData(medicines, repo, null, onSuccess);
    }

    public void initData(List<MedicineInventoryDTO> medicines, InventoryRepository repo, MedicineInventoryDTO preselectedMedicine, Runnable onSuccess) {
        this.repository = repo != null ? repo : new InventoryRepository();
        this.inventoryService = new InventoryServiceImpl(this.repository);
        this.onSuccessCallback = onSuccess;
        medicinePicker.setMedicines(medicines);
        if (preselectedMedicine != null) {
            selectMedicinePredefined(preselectedMedicine);
        }
    }

    public void selectMedicinePredefined(MedicineInventoryDTO dto) {
        if (dto != null) {
            medicinePicker.setSelectedMedicine(dto);
        }
    }

    private void onMedicineSelected(MedicineInventoryDTO med) {
        currentMedicineLots.clear();
        hideItemError();
        clearPickerError(medicinePicker, lblErrorMedicine);

        if (med == null) {
            boxMedicineInfo.setVisible(false);
            boxMedicineInfo.setManaged(false);
            resetToSingleEmptyRow();
            return;
        }

        lblMedCode.setText(med.getMedicineCode() != null ? med.getMedicineCode() : "");
        lblMedName.setText(med.getMedicineName() != null ? med.getMedicineName() : "");
        lblMedCategory.setText(med.getCategoryName() != null ? "Nhóm: " + med.getCategoryName() : "Nhóm: —");
        String unit = med.getUnit() != null ? med.getUnit() : "đơn vị";
        lblMedTotalStock.setText("Tổng tồn: " + med.getTotalQuantity() + " " + unit);

        boxMedicineInfo.setVisible(true);
        boxMedicineInfo.setManaged(true);

        resetToSingleEmptyRow();

        Task<List<MedicineLotDTO>> task = new Task<List<MedicineLotDTO>>() {
            @Override
            protected List<MedicineLotDTO> call() {
                return repository.getLotsByMedicineId(med.getMedicineId());
            }
        };

        task.setOnSucceeded(e -> {
            List<MedicineLotDTO> allLots = task.getValue();
            List<MedicineLotDTO> availableLots = allLots != null ? allLots.stream()
                    .filter(lot -> lot.getQuantityRemaining() != null && lot.getQuantityRemaining() > 0)
                    .toList() : List.of();

            currentMedicineLots.setAll(availableLots);
            if (activeLotRows.isEmpty()) {
                addNewLotRow(false);
            }

            if (!availableLots.isEmpty()) {
                activeLotRows.get(0).cbLot.setValue(availableLots.get(0));
            } else {
                activeLotRows.get(0).cbLot.setValue(null);
                showItemError("⚠️ Thuốc này hiện không có lô hàng nào còn tồn trong kho!");
            }
            updateAllRowsAvailableDisplay();
        });

        task.setOnFailed(e -> {
            showItemError("❌ Không thể tải danh sách lô của thuốc này.");
            if (activeLotRows.isEmpty()) {
                addNewLotRow(false);
            }
            updateAllRowsAvailableDisplay();
        });

        BackgroundTasks.run(task);
    }

    @FXML
    private void handleAddItemToList() {
        hideItemError();
        MedicineInventoryDTO selectedMed = medicinePicker.getSelectedMedicine();
        if (selectedMed == null) {
            showPickerError(medicinePicker, lblErrorMedicine, "Vui lòng nhập tìm kiếm và chọn thuốc cần xuất");
            return;
        }

        if (activeLotRows.isEmpty()) {
            showItemError("Chưa có dòng lô nào để xuất. Vui lòng bấm '+ Xuất thêm lô khác'!");
            return;
        }

        boolean hasError = false;
        for (LotRowItem r : activeLotRows) {
            MedicineLotDTO lot = r.cbLot.getValue();
            if (lot == null) {
                showControlError(r.cbLot, r.lblErrorLot, "Vui lòng chọn lô xuất");
                hasError = true;
                continue;
            }

            String qStr = r.txtQuantity.getText() != null ? r.txtQuantity.getText().trim() : "";
            if (qStr.isEmpty()) {
                showControlError(r.txtQuantity, r.lblErrorQuantity, "Vui lòng nhập SL xuất");
                hasError = true;
                continue;
            }

            int qty;
            try {
                qty = Integer.parseInt(qStr);
                if (qty <= 0) {
                    showControlError(r.txtQuantity, r.lblErrorQuantity, "Số lượng phải lớn hơn 0");
                    hasError = true;
                    continue;
                }
            } catch (NumberFormatException e) {
                showControlError(r.txtQuantity, r.lblErrorQuantity, "Số lượng không hợp lệ");
                hasError = true;
                continue;
            }

            int total = lot.getQuantityRemaining() != null ? lot.getQuantityRemaining() : 0;
            int avail = getRemainingAvailableForLot(lot.getId(), total, r);
            if (qty > avail) {
                showControlError(r.txtQuantity, r.lblErrorQuantity, "Vượt quá tồn khả dụng (Tối đa: " + avail + ")");
                hasError = true;
                continue;
            }

            String reason = r.getReasonText();
            if (reason.isBlank()) {
                showControlError(r.txtReason, r.lblErrorReason, "Vui lòng chọn hoặc nhập lý do xuất");
                hasError = true;
                continue;
            }
            if (reason.length() > 255) {
                showControlError(r.txtReason, r.lblErrorReason, "Lý do xuất không vượt quá 255 ký tự");
                hasError = true;
                continue;
            }
        }

        if (hasError) {
            return;
        }

        for (LotRowItem r : activeLotRows) {
            MedicineLotDTO lot = r.cbLot.getValue();
            int qty = Integer.parseInt(r.txtQuantity.getText().trim());
            String reason = r.getReasonText();
            int total = lot.getQuantityRemaining() != null ? lot.getQuantityRemaining() : 0;

            BatchExportItemDTO sameLotAndReason = null;
            for (BatchExportItemDTO it : itemList) {
                if (it.getLotId() == lot.getId() && reason.equals(it.getReason())) {
                    sameLotAndReason = it;
                    break;
                }
            }

            if (sameLotAndReason != null) {
                sameLotAndReason.setExportQuantity(sameLotAndReason.getExportQuantity() + qty);
            } else {
                BatchExportItemDTO newItem = BatchExportItemDTO.of(
                        selectedMed.getMedicineId(),
                        selectedMed.getMedicineCode(),
                        selectedMed.getMedicineName(),
                        selectedMed.getCategoryName(),
                        selectedMed.getManufacturer(),
                        lot.getId(),
                        lot.getLotNumber(),
                        lot.getExpiryDate(),
                        selectedMed.getUnit(),
                        total,
                        qty,
                        lot.getImportPrice(),
                        reason
                );
                itemList.add(newItem);
            }
        }

        updateSummary();
        tableItems.refresh();

        clearMedicineSelection();
    }

    private void clearMedicineSelection() {
        medicinePicker.clear();
        currentMedicineLots.clear();
        boxMedicineInfo.setVisible(false);
        boxMedicineInfo.setManaged(false);
        resetToSingleEmptyRow();
        hideItemError();
        clearPickerError(medicinePicker, lblErrorMedicine);
    }

    @FXML
    private void handleClearAllItems() {
        if (itemList.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtils.configure(confirm, btnClearAllItems.getScene().getWindow());
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa toàn bộ mặt hàng trong phiếu xuất?");
        confirm.setContentText("Bạn có chắc chắn muốn xóa tất cả " + itemList.size() + " mặt hàng đang có trong phiếu?");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                itemList.clear();
                updateSummary();
                tableItems.refresh();
                updateAllRowsAvailableDisplay();
            }
        });
    }

    private void updateSummary() {
        int totalItems = itemList.size();
        lblItemCountBadge.setText(totalItems + " mặt hàng");
    }

    @FXML
    private void handleConfirmExport() {
        hideGeneralError();

        if (itemList.isEmpty()) {
            showGeneralError("Phiếu xuất kho chưa có mặt hàng nào. Vui lòng thêm ít nhất 1 loại thuốc vào phiếu!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtils.configure(confirm, btnConfirmExport.getScene().getWindow());
        confirm.setTitle("Xác nhận xuất kho");
        confirm.setHeaderText("Bạn có đồng ý xuất kho không?");
        confirm.setContentText(String.format("Phiếu xuất gồm %d mặt hàng.", itemList.size()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String note = txtNote.getText() != null ? txtNote.getText().trim() : "";
            doExecuteExport(note);
        }
    }

    private void doExecuteExport(String note) {
        Integer employeeId = UserSession.isLoggedIn() ? UserSession.getInstance().getEmployeeId() : null;
        String generalReason = "Xuất kho (" + itemList.size() + " mặt hàng)";

        btnConfirmExport.setDisable(true);
        btnConfirmExport.setText("Đang xử lý xuất kho...");

        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() {
                return inventoryService.exportBatchStock(
                        generalReason,
                        employeeId,
                        note,
                        List.copyOf(itemList)
                );
            }
        };

        exportTask.setOnSucceeded(e -> {
            btnConfirmExport.setDisable(false);
            btnConfirmExport.setText("Xác nhận Xuất Kho");

            int totalLots = itemList.size();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            DialogUtils.configure(alert, btnConfirmExport.getScene().getWindow());
            alert.setTitle("Thành công");
            alert.setHeaderText("Xuất kho thành công!");
            alert.setContentText(String.format("Đã lập phiếu xuất kho thành công cho %d mặt hàng.", totalLots));
            alert.showAndWait();

            closeDialog();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        });

        exportTask.setOnFailed(e -> {
            btnConfirmExport.setDisable(false);
            btnConfirmExport.setText("Xác nhận Xuất Kho");
            Throwable ex = exportTask.getException();
            log.error("Lỗi xuất kho hàng loạt: ", ex);
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra khi xuất kho hàng loạt!";
            showGeneralError(msg);
        });

        BackgroundTasks.run(exportTask);
    }

    private void showControlError(Control input, Label errLabel, String msg) {
        if (input != null && !input.getStyleClass().contains("form-input-error")) {
            input.getStyleClass().add("form-input-error");
        }
        if (errLabel != null) {
            errLabel.setText(msg);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        }
    }

    private void showPickerError(SearchableMedicinePicker picker, Label errLabel, String msg) {
        if (picker != null) picker.showErrorStyle();
        if (errLabel != null) {
            errLabel.setText(msg);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        }
    }

    private void clearControlError(Control input, Label errLabel) {
        if (input != null) input.getStyleClass().remove("form-input-error");
        if (errLabel != null) {
            errLabel.setText("");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
    }

    private void clearPickerError(SearchableMedicinePicker picker, Label errLabel) {
        if (picker != null) picker.clearErrorStyle();
        if (errLabel != null) {
            errLabel.setText("");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
    }

    private void showItemError(String msg) {
        lblItemError.setText(msg);
        lblItemError.setVisible(true);
        lblItemError.setManaged(true);
    }

    private void hideItemError() {
        lblItemError.setVisible(false);
        lblItemError.setManaged(false);
        lblItemError.setText("");
    }

    private void showGeneralError(String msg) {
        lblGeneralError.setText(msg);
        lblGeneralError.setVisible(true);
        lblGeneralError.setManaged(true);
    }

    private void hideGeneralError() {
        lblGeneralError.setVisible(false);
        lblGeneralError.setManaged(false);
        lblGeneralError.setText("");
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void setMasterMedicines(List<MedicineInventoryDTO> medicines) {
        medicinePicker.setMedicines(medicines);
    }

    public void resetFormWithItem(MedicineInventoryDTO item) {
        clearForm();
        if (item != null) {
            medicinePicker.setSelectedMedicine(item);
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        hideGeneralError();
        hideItemError();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void clearForm() {
        medicinePicker.clear();
        currentMedicineLots.clear();
        txtNote.clear();
        boxMedicineInfo.setVisible(false);
        boxMedicineInfo.setManaged(false);
        itemList.clear();
        updateSummary();
        resetToSingleEmptyRow();
        hideGeneralError();
        hideItemError();
    }
}
