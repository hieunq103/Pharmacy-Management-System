package com.pharmacy.inventory;

import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.ui.ExportDialogController;
import com.pharmacy.inventory.ui.ImportDialogController;
import com.pharmacy.inventory.ui.InventoryHistoryController;
import com.pharmacy.inventory.ui.MedicineLotDialogController;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.PageResult;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.DrawerHelper;
import com.pharmacy.common.TableFilterHelper;
import com.pharmacy.common.TableSyncHelper;

public class InventoryController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @FXML private VBox viewMedicineInventory;
    @FXML private StackPane viewInventoryHistory;

    @FXML private Label lblTotalMedicines;
    @FXML private Label lblInStockMedicines;
    @FXML private TextField txtSearch;
    @FXML private Button btnImport;
    @FXML private Button btnExport;
    @FXML private Button btnHistory;

    @FXML private TableView<MedicineInventoryDTO> inventoryTable;
    @FXML private TableColumn<MedicineInventoryDTO, String> colCode;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colName;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colActiveIngredient;
    @FXML private TableColumn<MedicineInventoryDTO, String> colCategory;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colManufacturer;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colQuantity;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colPrice;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO.StockStatus> colStockStatus;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO.ExpiryStatus> colExpiryStatus;

    @FXML private TableView<MedicineInventoryDTO> tableActionSticky;
    @FXML private TableColumn<MedicineInventoryDTO, MedicineInventoryDTO> colAction;

    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;

    private final InventoryService inventoryService = new InventoryServiceImpl();
    private final ObservableList<MedicineInventoryDTO> pageData = FXCollections.observableArrayList();

    private final Map<String, String> activeFilters = new HashMap<>();

    private int pageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int currentPage = 1;
    private int totalPages = 1;
    private long totalRecords = 0;
    private Task<PageResult<MedicineInventoryDTO>> currentLoadTask;

    private final DrawerHelper drawerHelper = new DrawerHelper();
    private Parent importDrawerRoot;
    private ImportDialogController importController;
    private Parent exportDrawerRoot;
    private ExportDialogController exportController;

    private InventoryHistoryController historyController;
    private Parent historyRootNode;

    public static class EditDraft {
        private String name;
        private String activeIngredient;
        private String manufacturer;
        private String sellPriceStr;

        public EditDraft(MedicineInventoryDTO dto) {
            this.name = dto.getMedicineName() != null ? dto.getMedicineName() : "";
            this.activeIngredient = dto.getActiveIngredient() != null ? dto.getActiveIngredient() : "";
            this.manufacturer = dto.getManufacturer() != null ? dto.getManufacturer() : "";
            this.sellPriceStr = dto.getSellPrice() != null ? String.format("%.0f", dto.getSellPrice()) : "0";
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getActiveIngredient() { return activeIngredient; }
        public void setActiveIngredient(String activeIngredient) { this.activeIngredient = activeIngredient; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public String getSellPriceStr() { return sellPriceStr; }
        public void setSellPriceStr(String sellPriceStr) { this.sellPriceStr = sellPriceStr; }
    }

    private Integer editingMedicineId = null;
    private final Map<Integer, EditDraft> editDrafts = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupStickyTable();

        TableSyncHelper.enableAutoRowHeight(inventoryTable, tableActionSticky, AppConstants.DEFAULT_PAGE_SIZE);

        inventoryTable.setItems(pageData);

        inventoryTable.setRowFactory(tv -> {
            TableRow<MedicineInventoryDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    if (editingMedicineId == null) {
                        showLotDetailDialog(row.getItem());
                    }
                }
            });
            return row;
        });

        TableFilterHelper.setupMultiSelectColumnFilter(colStockStatus, "Tồn kho", new String[]{"Sắp hết hàng", "Hết hàng", "Bình thường"}, "stockStatus", activeFilters, () -> {
            currentPage = 1;
            loadInventoryData();
        });

        TableFilterHelper.setupMultiSelectColumnFilter(colExpiryStatus, "Hạn dùng", new String[]{"Có lô cận hạn", "Có lô hết hạn", "Còn hạn"}, "expiryStatus", activeFilters, () -> {
            currentPage = 1;
            loadInventoryData();
        });

        setupEvents();
        loadInventoryData();
        loadSummaryCounts();
    }

    public void applyFilters(String[] stockFilters, String[] expiryFilters) {
        if (stockFilters != null && stockFilters.length > 0) {
            TableFilterHelper.setMultiFilter(colStockStatus, stockFilters);
        }
        if (expiryFilters != null && expiryFilters.length > 0) {
            TableFilterHelper.setMultiFilter(colExpiryStatus, expiryFilters);
        }
    }

    private void setupTableColumns() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("medicineCode"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        colName.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colName.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            private final Label lblName = new Label();
            private final Label lblPrescription = new Label("Thuốc kê toa");
            private final VBox vbox = new VBox(2, lblName, lblPrescription);
            private final TextField txtField = new TextField();

            {
                setAlignment(Pos.CENTER_LEFT);
                vbox.setAlignment(Pos.CENTER_LEFT);
                vbox.setMaxHeight(Double.MAX_VALUE);
                lblName.setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #1E293B;");
                lblPrescription.getStyleClass().add("lbl-prescription-tag");
                txtField.getStyleClass().add("table-cell-editor");
                txtField.setOnAction(e -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) saveRowEdit(dto);
                });
                txtField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) cancelEditing();
                });
                txtField.textProperty().addListener((obs, oldV, newV) -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null && editingMedicineId != null && editingMedicineId.equals(dto.getMedicineId())) {
                        EditDraft draft = editDrafts.get(dto.getMedicineId());
                        if (draft != null) draft.setName(newV);
                    }
                });
            }

            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (editingMedicineId != null && editingMedicineId.equals(item.getMedicineId())) {
                    EditDraft draft = editDrafts.get(item.getMedicineId());
                    if (draft != null && !Objects.equals(txtField.getText(), draft.getName())) {
                        txtField.setText(draft.getName());
                    }
                    setText(null);
                    setGraphic(txtField);
                    Platform.runLater(txtField::requestFocus);
                } else {
                    lblName.setText(item.getMedicineName() != null ? item.getMedicineName() : "");
                    boolean needPrescription = Boolean.TRUE.equals(item.getRequiresPrescription());
                    lblPrescription.setVisible(needPrescription);
                    lblPrescription.setManaged(needPrescription);
                    vbox.setAlignment(Pos.CENTER_LEFT);
                    setText(null);
                    setGraphic(vbox);
                }
            }
        });

        colActiveIngredient.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colActiveIngredient.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            private final Label lblText = new Label();
            private final TextField txtField = new TextField();

            {
                lblText.setStyle("-fx-text-fill: #1E293B;");
                txtField.getStyleClass().add("table-cell-editor");
                txtField.setOnAction(e -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) saveRowEdit(dto);
                });
                txtField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) cancelEditing();
                });
                txtField.textProperty().addListener((obs, oldV, newV) -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null && editingMedicineId != null && editingMedicineId.equals(dto.getMedicineId())) {
                        EditDraft draft = editDrafts.get(dto.getMedicineId());
                        if (draft != null) draft.setActiveIngredient(newV);
                    }
                });
            }

            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (editingMedicineId != null && editingMedicineId.equals(item.getMedicineId())) {
                    EditDraft draft = editDrafts.get(item.getMedicineId());
                    if (draft != null && !Objects.equals(txtField.getText(), draft.getActiveIngredient())) {
                        txtField.setText(draft.getActiveIngredient());
                    }
                    setText(null);
                    setGraphic(txtField);
                } else {
                    lblText.setText(item.getActiveIngredient() != null ? item.getActiveIngredient() : "");
                    setText(null);
                    setGraphic(lblText);
                }
            }
        });

        colManufacturer.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colManufacturer.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            private final Label lblText = new Label();
            private final TextField txtField = new TextField();

            {
                lblText.setStyle("-fx-text-fill: #1E293B;");
                txtField.getStyleClass().add("table-cell-editor");
                txtField.setOnAction(e -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) saveRowEdit(dto);
                });
                txtField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) cancelEditing();
                });
                txtField.textProperty().addListener((obs, oldV, newV) -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null && editingMedicineId != null && editingMedicineId.equals(dto.getMedicineId())) {
                        EditDraft draft = editDrafts.get(dto.getMedicineId());
                        if (draft != null) draft.setManufacturer(newV);
                    }
                });
            }

            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (editingMedicineId != null && editingMedicineId.equals(item.getMedicineId())) {
                    EditDraft draft = editDrafts.get(item.getMedicineId());
                    if (draft != null && !Objects.equals(txtField.getText(), draft.getManufacturer())) {
                        txtField.setText(draft.getManufacturer());
                    }
                    setText(null);
                    setGraphic(txtField);
                } else {
                    lblText.setText(item.getManufacturer() != null ? item.getManufacturer() : "");
                    setText(null);
                    setGraphic(lblText);
                }
            }
        });

        colQuantity.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colQuantity.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String unit = item.getUnit() != null ? item.getUnit() : "";
                    setText(item.getTotalQuantity() + " " + unit);
                    setGraphic(null);
                }
            }
        });

        colPrice.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colPrice.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            private final Label lblText = new Label();
            private final TextField txtField = new TextField();

            {
                lblText.setStyle("-fx-text-fill: #1E293B;");
                txtField.getStyleClass().add("table-cell-editor");
                txtField.setOnAction(e -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) saveRowEdit(dto);
                });
                txtField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) cancelEditing();
                });
                txtField.textProperty().addListener((obs, oldV, newV) -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null && editingMedicineId != null && editingMedicineId.equals(dto.getMedicineId())) {
                        EditDraft draft = editDrafts.get(dto.getMedicineId());
                        if (draft != null) draft.setSellPriceStr(newV);
                    }
                });
            }

            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (editingMedicineId != null && editingMedicineId.equals(item.getMedicineId())) {
                    EditDraft draft = editDrafts.get(item.getMedicineId());
                    if (draft != null && !Objects.equals(txtField.getText(), draft.getSellPriceStr())) {
                        txtField.setText(draft.getSellPriceStr());
                    }
                    setText(null);
                    setGraphic(txtField);
                } else {
                    BigDecimal price = item.getSellPrice();
                    lblText.setText(price != null ? String.format("%,.0f đ", price.doubleValue()) : "");
                    setText(null);
                    setGraphic(lblText);
                }
            }
        });

        colStockStatus.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));
        colStockStatus.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO.StockStatus>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("status-badge");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MedicineInventoryDTO.StockStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    badge.setText(status.getLabel());
                    badge.getStyleClass().setAll("status-badge");
                    switch (status) {
                        case NORMAL -> {
                            badge.getStyleClass().add("status-badge-normal");
                            badge.setStyle("-fx-text-fill: #16A34A;");
                        }
                        case LOW_STOCK -> {
                            badge.getStyleClass().add("status-badge-low-stock");
                            badge.setStyle("-fx-text-fill: #EA580C;");
                        }
                        case OUT_OF_STOCK -> {
                            badge.getStyleClass().add("status-badge-out-of-stock");
                            badge.setStyle("-fx-text-fill: #64748B;");
                        }
                    }
                    setText(null);
                    setGraphic(badge);
                }
            }
        });

        colExpiryStatus.setCellValueFactory(new PropertyValueFactory<>("expiryStatus"));
        colExpiryStatus.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO.ExpiryStatus>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("status-badge");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MedicineInventoryDTO.ExpiryStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    badge.setText(status.getLabel());
                    badge.getStyleClass().setAll("status-badge");
                    switch (status) {
                        case NORMAL -> {
                            badge.getStyleClass().add("status-badge-normal");
                            badge.setStyle("-fx-text-fill: #16A34A;");
                        }
                        case NEAR_EXPIRY -> {
                            badge.getStyleClass().add("status-badge-near-expiry");
                            badge.setStyle("-fx-text-fill: #D97706;");
                        }
                        case EXPIRED -> {
                            badge.getStyleClass().add("status-badge-expired");
                            badge.setStyle("-fx-text-fill: #D9483B;");
                        }
                    }
                    setText(null);
                    setGraphic(badge);
                }
            }
        });

        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAction.setCellFactory(param -> new TableCell<MedicineInventoryDTO, MedicineInventoryDTO>() {
            private final Button btnMore = new Button("⋮");
            private final Button btnSave = new Button("✓");
            private final Button btnCancel = new Button("✕");
            private final HBox editBox = new HBox(4, btnSave, btnCancel);

            {
                btnMore.getStyleClass().add("btn-action-more");
                btnSave.getStyleClass().add("btn-action-save");
                btnCancel.getStyleClass().add("btn-action-cancel");
                btnSave.setTooltip(new Tooltip("Lưu (Enter)"));
                btnCancel.setTooltip(new Tooltip("Hủy (Esc)"));
                editBox.setAlignment(Pos.CENTER);

                btnMore.setOnAction(event -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) {
                        handleActionClick(dto, btnMore);
                    }
                });

                btnSave.setOnAction(event -> {
                    MedicineInventoryDTO dto = getItem();
                    if (dto != null) saveRowEdit(dto);
                });

                btnCancel.setOnAction(event -> cancelEditing());
            }

            @Override
            protected void updateItem(MedicineInventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (editingMedicineId != null && editingMedicineId.equals(item.getMedicineId())) {
                    setAlignment(Pos.CENTER);
                    setText(null);
                    setGraphic(editBox);
                } else {
                    setAlignment(Pos.CENTER);
                    setText(null);
                    setGraphic(btnMore);
                }
            }
        });
    }

    private void setupStickyTable() {
        TableSyncHelper.setupDualTableSync(inventoryTable, tableActionSticky);

        Platform.runLater(() -> {
            Node nestedHeader = inventoryTable.lookup(".nested-column-header");
            if (nestedHeader != null) {
                inventoryTable.getItems().addListener((javafx.collections.ListChangeListener<MedicineInventoryDTO>) c -> {
                    nestedHeader.setTranslateX(0.0);
                });
            }
        });
    }

    private void setupEvents() {
        btnImport.setOnAction(e -> showImportDrawer());
        btnExport.setOnAction(e -> showExportDrawer(null));
        if (btnHistory != null) {
            btnHistory.setOnAction(e -> showHistoryView());
        }

        if (txtSearch != null) {
            com.pharmacy.common.DebounceUtil.debounce(txtSearch, AppConstants.SEARCH_DEBOUNCE, text -> {
                currentPage = 1;
                loadInventoryData();
            });
        }
    }

    private void handleActionClick(MedicineInventoryDTO dto, Button btnTarget) {
        if (dto == null) return;

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("action-context-menu");

        MenuItem itemEdit = new MenuItem("Sửa");
        itemEdit.setOnAction(e -> startEditing(dto));

        MenuItem itemLotDetail = new MenuItem("Xem chi tiết Lô hàng");
        itemLotDetail.setOnAction(e -> showLotDetailDialog(dto));

        MenuItem itemExport = new MenuItem("Xuất kho / Xuất hủy");
        itemExport.setOnAction(e -> showExportDrawer(dto));

        contextMenu.getItems().addAll(itemEdit, itemLotDetail, itemExport);
        contextMenu.show(btnTarget, Side.BOTTOM, 0, 0);
    }

    private void showLotDetailDialog(MedicineInventoryDTO dto) {
        if (dto == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/medicine_lot_dialog.fxml"));
            Parent root = loader.load();

            MedicineLotDialogController controller = loader.getController();
            controller.setMedicineData(dto, inventoryService.getRepository());

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (inventoryTable != null && inventoryTable.getScene() != null) {
                dialogStage.initOwner(inventoryTable.getScene().getWindow());
            }
            dialogStage.setTitle("Chi tiết các lô hàng - " + dto.getMedicineName());

            Scene scene = new Scene(root);

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setWidth(1150);
            dialogStage.setHeight(640);
            dialogStage.setMinWidth(1050);
            dialogStage.setMinHeight(580);
            dialogStage.showAndWait();
        } catch (Exception ex) {
            log.error("Lỗi khi mở cửa sổ chi tiết lô hàng: ", ex);
            DialogUtils.showError("Lỗi", "Không thể mở chi tiết lô hàng: " + ex.getMessage());
        }
    }

    private void initDrawerInfrastructure() {
        if (inventoryTable == null || inventoryTable.getScene() == null) return;
        drawerHelper.init(inventoryTable.getScene(), () -> {
            onStockUpdated();
            if (historyController != null) {
                historyController.loadAllData();
            }
        });
    }

    private void onStockUpdated() {
        loadInventoryData();
        loadSummaryCounts();
    }

    private void showHistoryView() {
        ensureHistoryViewLoaded();
        if (viewMedicineInventory != null) {
            viewMedicineInventory.setVisible(false);
            viewMedicineInventory.setManaged(false);
        }
        if (viewInventoryHistory != null) {
            viewInventoryHistory.setVisible(true);
            viewInventoryHistory.setManaged(true);
        }
        if (historyController != null) {
            historyController.loadAllData();
        }
    }

    public void showInventoryView() {
        if (viewInventoryHistory != null) {
            viewInventoryHistory.setVisible(false);
            viewInventoryHistory.setManaged(false);
        }
        if (viewMedicineInventory != null) {
            viewMedicineInventory.setVisible(true);
            viewMedicineInventory.setManaged(true);
        }
        loadInventoryData();
    }

    private void ensureHistoryViewLoaded() {
        if (historyRootNode != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/inventory_history.fxml"));
            historyRootNode = loader.load();
            historyController = loader.getController();
            historyController.setOnBackToInventory(this::showInventoryView);
            viewInventoryHistory.getChildren().setAll(historyRootNode);
        } catch (Exception e) {
            log.error("Không thể tải giao diện lịch sử xuất nhập kho: ", e);
        }
    }

    private void ensureImportDrawerLoaded() {
        if (importDrawerRoot != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/import_drawer.fxml"));
            importDrawerRoot = loader.load();
            importController = loader.getController();

            importController.initData(List.of(), inventoryService.getRepository(), this::onStockUpdated);
            importController.setOnCloseCallback(drawerHelper::close);

            drawerHelper.registerDrawer(importDrawerRoot);
        } catch (Exception ex) {
            log.error("Lỗi khi nạp giao diện drawer Nhập kho: ", ex);
        }
    }

    private void ensureExportDrawerLoaded() {
        if (exportDrawerRoot != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/export_drawer.fxml"));
            exportDrawerRoot = loader.load();
            exportController = loader.getController();

            exportController.initData(List.of(), inventoryService.getRepository(), null, this::onStockUpdated);
            exportController.setOnCloseCallback(drawerHelper::close);

            drawerHelper.registerDrawer(exportDrawerRoot);
        } catch (Exception ex) {
            log.error("Lỗi khi nạp giao diện drawer Xuất kho: ", ex);
        }
    }

    private void showImportDrawer() {
        initDrawerInfrastructure();
        ensureImportDrawerLoaded();
        if (importDrawerRoot == null) return;

        if (exportDrawerRoot != null) {
            exportDrawerRoot.setVisible(false);
        }

        importController.clearForm();
        importController.setMasterMedicines(inventoryService.getMedicinesForPicker());
        drawerHelper.open(importDrawerRoot);
    }

    private void showExportDrawer(MedicineInventoryDTO preselectedMedicine) {
        initDrawerInfrastructure();
        ensureExportDrawerLoaded();
        if (exportDrawerRoot == null) return;

        if (importDrawerRoot != null) {
            importDrawerRoot.setVisible(false);
        }

        exportController.setMasterMedicines(inventoryService.getMedicinesForPicker());
        exportController.resetFormWithItem(preselectedMedicine);
        drawerHelper.open(exportDrawerRoot);
    }

    private void startEditing(MedicineInventoryDTO dto) {
        if (dto == null) return;
        editingMedicineId = dto.getMedicineId();
        editDrafts.put(dto.getMedicineId(), new EditDraft(dto));
        inventoryTable.refresh();
        if (tableActionSticky != null) {
            tableActionSticky.refresh();
        }
    }

    private void cancelEditing() {
        if (editingMedicineId != null) {
            editDrafts.remove(editingMedicineId);
            editingMedicineId = null;
            inventoryTable.refresh();
            if (tableActionSticky != null) {
                tableActionSticky.refresh();
            }
        }
    }

    private void saveRowEdit(MedicineInventoryDTO dto) {
        if (dto == null || editingMedicineId == null) return;
        EditDraft draft = editDrafts.get(dto.getMedicineId());
        if (draft == null) return;

        String newName = draft.getName() != null ? draft.getName().trim() : "";
        String newActiveIngredient = draft.getActiveIngredient() != null ? draft.getActiveIngredient().trim() : "";
        String newManufacturer = draft.getManufacturer() != null ? draft.getManufacturer().trim() : "";
        String rawPrice = draft.getSellPriceStr() != null ? draft.getSellPriceStr().trim() : "0";

        if (newName.isEmpty()) {
            DialogUtils.showWarning("Cảnh báo", "Tên thuốc không được để trống!");
            return;
        }

        rawPrice = rawPrice.replaceAll("[đĐ\\s]", "");
        if (rawPrice.matches("^\\d{1,3}(\\.\\d{3})+$")) {
            rawPrice = rawPrice.replace(".", "");
        } else if (rawPrice.matches("^\\d{1,3}(,\\d{3})+$")) {
            rawPrice = rawPrice.replace(",", "");
        } else if (rawPrice.contains(",") && !rawPrice.contains(".")) {
            rawPrice = rawPrice.replace(",", ".");
        }

        BigDecimal newPrice;
        try {
            newPrice = new BigDecimal(rawPrice);
            if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
                DialogUtils.showWarning("Cảnh báo", "Giá bán không được âm!");
                return;
            }
        } catch (Exception e) {
            DialogUtils.showWarning("Cảnh báo", "Giá bán không hợp lệ (ví dụ: 150000)!");
            return;
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return inventoryService.updateMedicine(
                        dto.getMedicineId(),
                        newName,
                        newActiveIngredient,
                        newManufacturer,
                        newPrice
                );
            }
        };

        saveTask.setOnSucceeded(e -> {
            boolean success = Boolean.TRUE.equals(saveTask.getValue());
            if (success) {
                dto.setMedicineName(newName);
                dto.setActiveIngredient(newActiveIngredient);
                dto.setManufacturer(newManufacturer);
                dto.setSellPrice(newPrice);

                editingMedicineId = null;
                editDrafts.remove(dto.getMedicineId());

                inventoryTable.refresh();
                if (tableActionSticky != null) {
                    tableActionSticky.refresh();
                }
                log.info("Cập nhật thành công thuốc ID={}", dto.getMedicineId());
            } else {
                DialogUtils.showError("Lỗi", "Không thể lưu cập nhật thuốc xuống cơ sở dữ liệu!");
            }
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            log.error("Lỗi khi cập nhật thuốc: ", ex);
            DialogUtils.showError("Lỗi", "Đã xảy ra lỗi khi lưu thông tin thuốc: " + (ex != null ? ex.getMessage() : ""));
        });

        BackgroundTasks.run(saveTask);
    }

    public void loadInventoryData() {
        cancelEditing();
        String keyword = (txtSearch != null && txtSearch.getText() != null)
                ? txtSearch.getText().trim()
                : null;
        String stockFilter = activeFilters.get("stockStatus");
        String expiryFilter = activeFilters.get("expiryStatus");

        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        Task<PageResult<MedicineInventoryDTO>> task = new Task<>() {
            @Override
            protected PageResult<MedicineInventoryDTO> call() {
                return inventoryService.getInventoryPage(currentPage, pageSize, keyword, stockFilter, expiryFilter);
            }
        };
        currentLoadTask = task;

        task.setOnSucceeded(e -> {
            if (task != currentLoadTask) return;
            PageResult<MedicineInventoryDTO> result = task.getValue();
            if (result != null) {
                pageData.setAll(result.getItems());
                totalPages = result.getTotalPages();
                totalRecords = result.getTotalElements();
                renderPagination();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("Lỗi khi tải dữ liệu kho: ", ex);
        });

        BackgroundTasks.run(task);
    }

    private void loadSummaryCounts() {
        Task<InventoryRepository.InventorySummary> summaryTask = new Task<>() {
            @Override
            protected InventoryRepository.InventorySummary call() {
                return inventoryService.getInventorySummary();
            }
        };
        summaryTask.setOnSucceeded(e -> {
            InventoryRepository.InventorySummary summary = summaryTask.getValue();
            if (summary != null) {
                if (lblTotalMedicines != null) lblTotalMedicines.setText(String.valueOf(summary.totalMedicines()));
                if (lblInStockMedicines != null) lblInStockMedicines.setText(String.valueOf(summary.inStockMedicines()));
            }
        });
        BackgroundTasks.run(summaryTask);
    }

    private void renderPagination() {
        cancelEditing();
        com.pharmacy.common.PaginationHelper.render(
                paginationBox,
                lblRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                pageSize,
                page -> {
                    currentPage = page;
                    TableSyncHelper.resetScrollToTop(inventoryTable, tableActionSticky);
                    loadInventoryData();
                }
        );
    }
}
