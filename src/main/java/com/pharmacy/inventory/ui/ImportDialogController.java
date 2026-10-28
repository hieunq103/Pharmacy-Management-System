package com.pharmacy.inventory.ui;

import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.InventoryServiceImpl;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.BatchImportItemDTO;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ImportDialogController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ImportDialogController.class);

    // Mã của thuốc mới chỉ được cấp khi lưu phiếu (bộ đếm trong CSDL) nên lúc lập phiếu chưa có mã thật
    private static final String PENDING_CODE = "Tự cấp khi lưu";

    @FXML private StackPane supplierPickerContainer;
    @FXML private Label lblErrorSupplier;
    @FXML private TextField txtNote;

    @FXML private RadioButton rbExistingMedicine;
    @FXML private RadioButton rbNewMedicine;
    @FXML private ToggleGroup itemModeGroup;

    @FXML private VBox boxExistingMedicine;
    @FXML private StackPane medicinePickerContainer;
    @FXML private HBox boxSelectedMedInfo;
    @FXML private Label lblSelectedMedName;
    @FXML private Label lblSelectedCategory;
    @FXML private Label lblSelectedStock;

    @FXML private VBox boxNewMedicine;
    @FXML private TextField txtNewName;
    @FXML private StackPane categoryPickerContainer;
    @FXML private TextField txtNewActiveIngredient;
    @FXML private TextField txtNewManufacturer;
    @FXML private TextField txtNewSellPrice;
    @FXML private CheckBox chkRequiresPrescription;

    @FXML private TextField txtLotNumber;
    @FXML private DatePicker dpExpiryDate;
    @FXML private ComboBox<String> cbUnit;
    @FXML private TextField txtQuantityIn;
    @FXML private TextField txtImportPrice;
    @FXML private Label lblItemError;

    @FXML private Label lblItemCountBadge;
    @FXML private TableView<BatchImportItemDTO> tableItems;
    @FXML private TableColumn<BatchImportItemDTO, Integer> colIndex;
    @FXML private TableColumn<BatchImportItemDTO, String> colCode;
    @FXML private TableColumn<BatchImportItemDTO, String> colName;
    @FXML private TableColumn<BatchImportItemDTO, String> colCategory;
    @FXML private TableColumn<BatchImportItemDTO, String> colManufacturer;
    @FXML private TableColumn<BatchImportItemDTO, String> colLot;
    @FXML private TableColumn<BatchImportItemDTO, String> colExpiry;
    @FXML private TableColumn<BatchImportItemDTO, String> colUnit;
    @FXML private TableColumn<BatchImportItemDTO, Integer> colQuantity;
    @FXML private TableColumn<BatchImportItemDTO, String> colPrice;
    @FXML private TableColumn<BatchImportItemDTO, String> colTotal;
    @FXML private TableColumn<BatchImportItemDTO, BatchImportItemDTO> colAction;

    @FXML private Label lblGeneralError;
    @FXML private Label lblSummaryTotalQuantity;
    @FXML private Label lblGrandTotal;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirmImport;

    private SearchableMedicinePicker medicinePicker;
    private SearchableStringPicker supplierPicker;
    private SearchableStringPicker categoryPicker;

    private InventoryRepository repository;
    private InventoryService inventoryService;
    private Runnable onSuccessCallback;
    private Runnable onCloseCallback;

    private final ObservableList<BatchImportItemDTO> itemList = FXCollections.observableArrayList();
    private MedicineInventoryDTO currentlySelectedMedicine = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        supplierPicker = new SearchableStringPicker("Nhập hoặc tìm kiếm nhà cung cấp...");
        supplierPickerContainer.getChildren().add(supplierPicker);

        medicinePicker = new SearchableMedicinePicker("Nhập mã thuốc hoặc tên thuốc...");
        medicinePickerContainer.getChildren().add(medicinePicker);

        categoryPicker = new SearchableStringPicker("Nhập hoặc tìm kiếm nhóm thuốc...");
        categoryPickerContainer.getChildren().add(categoryPicker);

        dpExpiryDate.setPromptText("dd/MM/yyyy");
        com.pharmacy.common.FormatUtils.setupDatePicker(dpExpiryDate);

        cbUnit.setItems(FXCollections.observableArrayList(
                "Hộp", "Vỉ", "Viên", "Chai", "Lọ", "Ống", "Gói", "Tuýp", "Hũ", "Bình", "Túi", "Miếng"
        ));
        cbUnit.setValue("Hộp");

        setupTableColumns();
        tableItems.setItems(itemList);

        itemModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isNew = (newVal == rbNewMedicine);
            boxExistingMedicine.setVisible(!isNew);
            boxExistingMedicine.setManaged(!isNew);
            boxNewMedicine.setVisible(isNew);
            boxNewMedicine.setManaged(isNew);
            clearItemError();
        });

        medicinePicker.selectedMedicineProperty().addListener((obs, oldVal, newVal) -> {
            currentlySelectedMedicine = newVal;
            if (newVal != null) {
                lblSelectedMedName.setText(newVal.getMedicineName() + " (" + newVal.getMedicineCode() + ")");
                lblSelectedCategory.setText(newVal.getCategoryName() != null ? newVal.getCategoryName() : "—");
                lblSelectedStock.setText(newVal.getTotalQuantity() + " " + (newVal.getUnit() != null ? newVal.getUnit() : ""));
                boxSelectedMedInfo.setVisible(true);
                boxSelectedMedInfo.setManaged(true);

                if (newVal.getUnit() != null && !newVal.getUnit().isBlank()) {
                    cbUnit.setValue(newVal.getUnit());
                }
                clearItemError();
            } else {
                boxSelectedMedInfo.setVisible(false);
                boxSelectedMedInfo.setManaged(false);
            }
        });

        txtQuantityIn.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("\\d*")) {
                txtQuantityIn.setText(newV.replaceAll("[^\\d]", ""));
            }
            clearItemError();
        });

        txtImportPrice.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("[\\d.,]*")) {
                txtImportPrice.setText(newV.replaceAll("[^\\d.,]", ""));
            }
            clearItemError();
        });

        txtNewSellPrice.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("[\\d.,]*")) {
                txtNewSellPrice.setText(newV.replaceAll("[^\\d.,]", ""));
            }
            clearItemError();
        });

        supplierPicker.textProperty().addListener((obs, o, n) -> {
            lblErrorSupplier.setVisible(false);
            lblErrorSupplier.setManaged(false);
        });
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(tableItems.getItems().indexOf(cellData.getValue()) + 1));
        colIndex.setCellFactory(col -> new TableCell<>() {
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
        colCode.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setMinWidth(Label.USE_PREF_SIZE);
                    lbl.setStyle(PENDING_CODE.equals(item)
                            ? "-fx-font-size: 11px; -fx-font-family: 'Inter 18pt Regular'; -fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-background-radius: 4px; -fx-padding: 2 6;"
                            : "-fx-font-size: 11px; -fx-font-family: 'Inter 18pt SemiBold'; -fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-background-radius: 4px; -fx-padding: 2 6;");
                    setGraphic(lbl);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        colLot.setCellValueFactory(new PropertyValueFactory<>("lotNumber"));
        colExpiry.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedExpiryDate()));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantityIn"));
        colPrice.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedImportPrice()));
        colTotal.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedLineTotal()));

        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("✕ Xóa");
            {
                btnDelete.setStyle("-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #FECACA; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                btnDelete.setOnMouseEntered(ev -> btnDelete.setStyle("-fx-background-color: #FCA5A5; -fx-text-fill: #991B1B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #F87171; -fx-border-radius: 4px; -fx-background-radius: 4px;"));
                btnDelete.setOnMouseExited(ev -> btnDelete.setStyle("-fx-background-color: #FEECEB; -fx-text-fill: #D9483B; -fx-font-family: 'Inter 18pt SemiBold'; -fx-cursor: hand; -fx-font-size: 11.5px; -fx-padding: 3 10; -fx-border-color: #FECACA; -fx-border-radius: 4px; -fx-background-radius: 4px;"));
                btnDelete.setTooltip(new Tooltip("Xóa mặt hàng này khỏi phiếu nhập"));
                btnDelete.setOnAction(e -> {
                    BatchImportItemDTO item = getItem();
                    if (item != null) {
                        itemList.remove(item);
                        updateSummary();
                        tableItems.refresh();
                    }
                });
            }

            @Override
            protected void updateItem(BatchImportItemDTO item, boolean empty) {
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
        this.repository = repo != null ? repo : new InventoryRepository();
        this.inventoryService = new InventoryServiceImpl(this.repository);
        this.onSuccessCallback = onSuccess;
        medicinePicker.setMedicines(medicines);

        Task<Void> loadAuxTask = new Task<>() {
            private List<String> suppliers;
            private List<String> categories;

            @Override
            protected Void call() {
                suppliers = inventoryService.getAllSupplierNames();
                categories = inventoryService.getAllCategoryNames();
                return null;
            }

            @Override
            protected void succeeded() {
                if (suppliers != null && !suppliers.isEmpty()) {
                    supplierPicker.setItems(suppliers);
                }
                if (categories != null && !categories.isEmpty()) {
                    categoryPicker.setItems(categories);
                }
            }
        };

        BackgroundTasks.run(loadAuxTask);
    }

    @FXML
    private void handleAddItemToList() {
        clearItemError();

        boolean isNewMedicine = rbNewMedicine.isSelected();

        String medicineName;
        String categoryName;
        String manufacturer = "";
        String activeIngredient = "";
        BigDecimal sellPrice = BigDecimal.ZERO;
        boolean rx = false;
        Integer existingId = null;
        String code = PENDING_CODE;

        if (isNewMedicine) {
            medicineName = txtNewName.getText() != null ? txtNewName.getText().trim() : "";
            categoryName = categoryPicker.getText() != null ? categoryPicker.getText().trim() : "";
            manufacturer = txtNewManufacturer.getText() != null ? txtNewManufacturer.getText().trim() : "";
            activeIngredient = txtNewActiveIngredient.getText() != null ? txtNewActiveIngredient.getText().trim() : "";
            String sellPriceStr = txtNewSellPrice.getText() != null ? txtNewSellPrice.getText().trim().replace(".", "").replace(",", "") : "0";

            if (medicineName.isEmpty()) {
                showItemError("Vui lòng nhập Tên thuốc / Biệt dược mới!");
                txtNewName.requestFocus();
                return;
            }
            if (categoryName.isEmpty()) {
                showItemError("Vui lòng chọn hoặc nhập Nhóm thuốc!");
                return;
            }
            try {
                if (!sellPriceStr.isEmpty()) {
                    sellPrice = new BigDecimal(sellPriceStr);
                }
            } catch (Exception e) {
                showItemError("Giá bán lẻ đề xuất không hợp lệ!");
                return;
            }
            rx = chkRequiresPrescription.isSelected();
        } else {
            if (currentlySelectedMedicine == null) {
                showItemError("Vui lòng tìm và chọn một loại thuốc trong kho (hoặc chọn chế độ '+ Thêm loại thuốc mới')!");
                return;
            }
            existingId = currentlySelectedMedicine.getMedicineId();
            code = currentlySelectedMedicine.getMedicineCode();
            medicineName = currentlySelectedMedicine.getMedicineName();
            categoryName = currentlySelectedMedicine.getCategoryName();
            manufacturer = currentlySelectedMedicine.getManufacturer();
        }

        String lotNumber = txtLotNumber.getText() != null ? txtLotNumber.getText().trim() : "";
        if (lotNumber.isEmpty()) {
            showItemError("Vui lòng nhập Số lô (Lot Number)!");
            txtLotNumber.requestFocus();
            return;
        }

        LocalDate expiryDate = dpExpiryDate.getValue();
        if (expiryDate == null) {
            showItemError("Vui lòng chọn Hạn sử dụng (dd/MM/yyyy)!");
            dpExpiryDate.requestFocus();
            return;
        }

        if (expiryDate.isBefore(LocalDate.now())) {
            showItemError("Cảnh báo: Hạn sử dụng (" + com.pharmacy.common.FormatUtils.formatDate(expiryDate) + ") đã hết hạn! Vui lòng kiểm tra lại vỏ hộp.");
            return;
        }

        String unit = cbUnit.getValue() != null ? cbUnit.getValue().trim() : "Hộp";
        if (unit.isEmpty()) {
            showItemError("Vui lòng chọn hoặc nhập Đơn vị tính!");
            return;
        }

        String qtyStr = txtQuantityIn.getText() != null ? txtQuantityIn.getText().trim() : "";
        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                showItemError("Số lượng nhập phải lớn hơn 0!");
                txtQuantityIn.requestFocus();
                return;
            }
        } catch (Exception e) {
            showItemError("Số lượng nhập không hợp lệ!");
            txtQuantityIn.requestFocus();
            return;
        }

        String priceStr = txtImportPrice.getText() != null ? txtImportPrice.getText().trim().replace(".", "").replace(",", "") : "";
        BigDecimal importPrice;
        try {
            if (priceStr.isEmpty()) priceStr = "0";
            importPrice = new BigDecimal(priceStr);
            if (importPrice.compareTo(BigDecimal.ZERO) < 0) {
                showItemError("Đơn giá nhập không được âm!");
                txtImportPrice.requestFocus();
                return;
            }
        } catch (Exception e) {
            showItemError("Đơn giá nhập không hợp lệ (ví dụ: 150000)!");
            txtImportPrice.requestFocus();
            return;
        }

        BatchImportItemDTO item;
        if (isNewMedicine) {
            item = BatchImportItemDTO.fromNew(
                    code, medicineName, categoryName, activeIngredient,
                    manufacturer, sellPrice, rx, lotNumber, expiryDate, unit, quantity, importPrice
            );
        } else {
            item = BatchImportItemDTO.fromExisting(
                    existingId, code, medicineName, categoryName,
                    manufacturer, lotNumber, expiryDate, unit, quantity, importPrice
            );
        }

        itemList.add(item);
        updateSummary();
        tableItems.refresh();
        tableItems.scrollTo(item);

        resetItemInputForm();
    }

    // Kết quả nhập kho: bảng mã thuốc + tên thuốc của cả phiếu (cuộn được khi nhiều dòng), thuốc mới có nhãn "Mới"
    private static void showImportResult(javafx.stage.Window owner, List<BatchImportItemDTO> items) {
        long newCount = items.stream().filter(BatchImportItemDTO::isNewMedicine).count();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogUtils.configure(alert, owner);
        alert.setTitle("Nhập kho thành công");
        alert.setHeaderText("Đã lập phiếu nhập kho thành công cho " + items.size() + " mặt hàng thuốc"
                + (newCount > 0 ? "\nTrong đó " + newCount + " thuốc mới đã được cấp mã" : ""));

        TableView<BatchImportItemDTO> table = new TableView<>(FXCollections.observableArrayList(items));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(""));

        TableColumn<BatchImportItemDTO, BatchImportItemDTO> codeCol = new TableColumn<>("Mã thuốc");
        codeCol.setSortable(false);
        codeCol.setMinWidth(130);
        codeCol.setPrefWidth(130);
        codeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        codeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BatchImportItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label code = new Label(item.getMedicineCode());
                code.setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #0066FF;");
                HBox box = new HBox(6, code);
                box.setAlignment(Pos.CENTER_LEFT);
                if (item.isNewMedicine()) {
                    Label badge = new Label("Mới");
                    badge.setMinWidth(Label.USE_PREF_SIZE);
                    badge.setStyle("-fx-font-size: 10.5px; -fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-background-radius: 4px; -fx-padding: 1 5;");
                    box.getChildren().add(badge);
                }
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<BatchImportItemDTO, String> nameCol = new TableColumn<>("Tên thuốc");
        nameCol.setSortable(false);
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineName()));

        TableColumn<BatchImportItemDTO, String> lotCol = new TableColumn<>("Số lô");
        lotCol.setSortable(false);
        lotCol.setPrefWidth(120);
        lotCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLotNumber()));

        TableColumn<BatchImportItemDTO, String> qtyCol = new TableColumn<>("Số lượng");
        qtyCol.setSortable(false);
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getQuantityIn() + " " + (c.getValue().getUnit() != null ? c.getValue().getUnit() : "")));

        for (TableColumn<BatchImportItemDTO, String> col : List.of(nameCol, lotCol, qtyCol)) {
            col.setStyle("-fx-alignment: CENTER-LEFT;");
        }
        table.getColumns().setAll(List.of(codeCol, nameCol, lotCol, qtyCol));
        table.setFixedCellSize(34);
        // Tối đa 8 dòng, nhiều hơn thì cuộn trong bảng
        table.setPrefHeight(36 + 34 * Math.min(Math.max(items.size(), 1), 8) + 4);
        table.setPrefWidth(620);

        alert.getDialogPane().setContent(table);
        alert.getDialogPane().setPrefWidth(660);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void resetItemInputForm() {
        medicinePicker.clear();
        currentlySelectedMedicine = null;
        boxSelectedMedInfo.setVisible(false);
        boxSelectedMedInfo.setManaged(false);

        txtNewName.clear();
        categoryPicker.clear();
        txtNewActiveIngredient.clear();
        txtNewManufacturer.clear();
        txtNewSellPrice.clear();
        chkRequiresPrescription.setSelected(false);

        txtLotNumber.clear();
        dpExpiryDate.setValue(null);
        txtQuantityIn.clear();
        txtImportPrice.clear();

        clearItemError();
    }

    @FXML
    private void handleClearAllItems() {
        if (itemList.isEmpty()) return;
        itemList.clear();
        updateSummary();
        tableItems.refresh();
    }

    private void updateSummary() {
        int totalItems = itemList.size();
        int totalQty = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (BatchImportItemDTO it : itemList) {
            totalQty += it.getQuantityIn();
            grandTotal = grandTotal.add(it.getLineTotal());
        }

        lblItemCountBadge.setText(totalItems + " mặt hàng");
        lblSummaryTotalQuantity.setText(totalQty + " sản phẩm");
        lblGrandTotal.setText(FormatUtils.formatCurrency(grandTotal) + " đ");

        lblGeneralError.setVisible(false);
        lblGeneralError.setManaged(false);
    }

    @FXML
    private void handleConfirmImport() {
        lblGeneralError.setVisible(false);
        lblGeneralError.setManaged(false);

        if (itemList.isEmpty()) {
            showGeneralError("Danh sách mặt hàng trống! Vui lòng thêm ít nhất 1 loại thuốc vào phiếu nhập.");
            return;
        }

        String supplier = supplierPicker.getText() != null ? supplierPicker.getText().trim() : "";
        if (supplier.isEmpty()) {
            lblErrorSupplier.setText("Vui lòng chọn hoặc nhập tên Nhà cung cấp!");
            lblErrorSupplier.setVisible(true);
            lblErrorSupplier.setManaged(true);
            supplierPicker.requestFocus();
            return;
        }

        Integer employeeId = UserSession.currentEmployeeId();

        String note = txtNote.getText() != null ? txtNote.getText().trim() : "";

        btnConfirmImport.setDisable(true);
        btnConfirmImport.setText("Đang ghi vào kho...");

        final Integer validEmpId = employeeId;
        Task<Boolean> importTask = new Task<>() {
            @Override
            protected Boolean call() {
                return inventoryService.importBatchStock(supplier, validEmpId, note, itemList);
            }
        };

        importTask.setOnSucceeded(e -> {
            btnConfirmImport.setDisable(false);
            btnConfirmImport.setText("Xác nhận Nhập Kho");

            boolean success = Boolean.TRUE.equals(importTask.getValue());
            if (success) {
                showImportResult(btnCancel != null && btnCancel.getScene() != null ? btnCancel.getScene().getWindow() : null,
                        List.copyOf(itemList));

                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }
                closeDialog();
            } else {
                showGeneralError("Không thể hoàn tất nhập kho. Vui lòng thử lại!");
            }
        });

        importTask.setOnFailed(e -> {
            btnConfirmImport.setDisable(false);
            btnConfirmImport.setText("Xác nhận Nhập Kho");
            Throwable ex = importTask.getException();
            log.error("Lỗi khi nhập kho hàng loạt: ", ex);
            showGeneralError("Lỗi hệ thống: " + (ex != null ? ex.getMessage() : "Không xác định"));
        });

        BackgroundTasks.run(importTask);
    }

    private void showItemError(String msg) {
        lblItemError.setText(msg);
        lblItemError.setVisible(true);
        lblItemError.setManaged(true);
    }

    private void clearItemError() {
        lblItemError.setText("");
        lblItemError.setVisible(false);
        lblItemError.setManaged(false);
    }

    private void showGeneralError(String msg) {
        lblGeneralError.setText(msg);
        lblGeneralError.setVisible(true);
        lblGeneralError.setManaged(true);
    }

    public void setMasterMedicines(List<MedicineInventoryDTO> medicines) {
        if (medicinePicker != null) {
            medicinePicker.setMedicines(medicines);
        }
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void clearForm() {
        itemList.clear();
        supplierPicker.clear();
        txtNote.clear();
        rbExistingMedicine.setSelected(true);
        resetItemInputForm();
        updateSummary();
    }
}
