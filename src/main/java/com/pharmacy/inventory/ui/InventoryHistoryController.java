package com.pharmacy.inventory.ui;

import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.AppConstants;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.common.TableSyncHelper;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.InventoryServiceImpl;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class InventoryHistoryController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(InventoryHistoryController.class);

    private final InventoryService inventoryService = new InventoryServiceImpl();

    private Runnable onBackToInventory;

    @FXML private DatePicker dpFilterDate;
    @FXML private Button btnFilterDate;
    @FXML private Tooltip tooltipFilter;

    private boolean isDateFiltered = false;
    private LocalDate appliedFilterDate = null;

    @FXML private TabPane historyTabPane;
    @FXML private Tab tabImport;
    @FXML private Tab tabExport;

    @FXML private TableView<InventoryTransactionDTO> tableImport;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportIndex;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportDateTime;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportEmployee;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportSupplier;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportItemCount;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportTotalQty;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportTotalAmount;
    @FXML private TableColumn<InventoryTransactionDTO, String> colImportNote;
    @FXML private HBox importPaginationBox;
    @FXML private Label lblImportRecordRange;

    @FXML private TableView<InventoryTransactionDTO> tableExport;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportIndex;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportDateTime;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportEmployee;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportType;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportItemCount;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportTotalQty;
    @FXML private TableColumn<InventoryTransactionDTO, String> colExportNote;
    @FXML private HBox exportPaginationBox;
    @FXML private Label lblExportRecordRange;

    private final ObservableList<InventoryTransactionDTO> importPageData = FXCollections.observableArrayList();
    private int importCurrentPage = 1;
    private final int importPageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int importTotalPages = 1;
    private long importTotalRecords = 0;

    private final ObservableList<InventoryTransactionDTO> exportPageData = FXCollections.observableArrayList();
    private int exportCurrentPage = 1;
    private final int exportPageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int exportTotalPages = 1;
    private long exportTotalRecords = 0;

    private boolean isImportDirty = false;
    private boolean isExportDirty = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupImportTableColumns();
        setupExportTableColumns();

        tableImport.setItems(importPageData);
        tableExport.setItems(exportPageData);

        TableSyncHelper.enableAutoRowHeight(tableImport, null, AppConstants.DEFAULT_PAGE_SIZE);
        TableSyncHelper.enableAutoRowHeight(tableExport, null, AppConstants.DEFAULT_PAGE_SIZE);

        TableSyncHelper.setupTableScrollProtection(tableImport, AppConstants.DEFAULT_PAGE_SIZE);
        TableSyncHelper.setupTableScrollProtection(tableExport, AppConstants.DEFAULT_PAGE_SIZE);

        if (historyTabPane != null) {
            historyTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                Platform.runLater(() -> {
                    TableSyncHelper.resetScrollToTop(tableImport, null);
                    TableSyncHelper.resetScrollToTop(tableExport, null);
                    TableSyncHelper.attachVirtualFlowProtection(tableImport);
                    TableSyncHelper.attachVirtualFlowProtection(tableExport);
                });

                if (newTab == tabImport) {
                    if (isImportDirty) {
                        isImportDirty = false;
                        importCurrentPage = 1;
                        loadImportData();
                    }
                } else if (newTab == tabExport) {
                    if (isExportDirty) {
                        isExportDirty = false;
                        exportCurrentPage = 1;
                        loadExportData();
                    }
                }
            });
        }

        setupEventHandlers();
        loadAllData();
    }

    public void setOnBackToInventory(Runnable onBackToInventory) {
        this.onBackToInventory = onBackToInventory;
    }

    private void setupImportTableColumns() {
        colImportIndex.setCellValueFactory(cellData -> {
            int idx = (importCurrentPage - 1) * importPageSize + tableImport.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        setupLeftAlignedCell(colImportIndex);

        colImportDateTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));
        setupLeftAlignedCell(colImportDateTime);

        colImportEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        setupLeftAlignedCell(colImportEmployee);

        colImportSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        setupLeftAlignedCell(colImportSupplier);

        colImportItemCount.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemCount() + " mặt hàng"));
        setupLeftAlignedCell(colImportItemCount);

        colImportTotalQty.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%,d", cellData.getValue().getTotalQuantity())));
        setupLeftAlignedCell(colImportTotalQty);

        colImportTotalAmount.setCellValueFactory(cellData ->
                new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getTotalAmount())));
        setupLeftAlignedCell(colImportTotalAmount);

        colImportNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        setupLeftAlignedCell(colImportNote);

        tableImport.setRowFactory(tv -> {
            TableRow<InventoryTransactionDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showTransactionDetailDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupExportTableColumns() {
        colExportIndex.setCellValueFactory(cellData -> {
            int idx = (exportCurrentPage - 1) * exportPageSize + tableExport.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        setupLeftAlignedCell(colExportIndex);

        colExportDateTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));
        setupLeftAlignedCell(colExportDateTime);

        colExportEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        setupLeftAlignedCell(colExportEmployee);

        colExportType.setCellValueFactory(new PropertyValueFactory<>("exportTypeLabel"));
        setupLeftAlignedCell(colExportType);

        colExportItemCount.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemCount() + " mặt hàng"));
        setupLeftAlignedCell(colExportItemCount);

        colExportTotalQty.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%,d", cellData.getValue().getTotalQuantity())));
        setupLeftAlignedCell(colExportTotalQty);

        colExportNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        setupLeftAlignedCell(colExportNote);

        tableExport.setRowFactory(tv -> {
            TableRow<InventoryTransactionDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showTransactionDetailDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private <T> void setupLeftAlignedCell(TableColumn<T, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });
    }

    private void setupEventHandlers() {
        if (dpFilterDate != null) {
            FormatUtils.setupDatePicker(dpFilterDate);

            dpFilterDate.getEditor().setOnAction(e -> applyDateFilter());

            dpFilterDate.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    if (isDateFiltered) {
                        clearDateFilter();
                    } else if (dpFilterDate.getValue() != null) {
                        dpFilterDate.setValue(null);
                    }
                }
            });
        }
    }

    @FXML
    private void handleFilterDate() {
        LocalDate currentPickerDate = dpFilterDate != null ? dpFilterDate.getValue() : null;
        if (currentPickerDate == null && dpFilterDate != null) {
            String text = dpFilterDate.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) {
                try {
                    currentPickerDate = dpFilterDate.getConverter().fromString(text.trim());
                    dpFilterDate.setValue(currentPickerDate);
                } catch (Exception ignored) {}
            }
        }

        if (isDateFiltered) {
            if (currentPickerDate != null && !currentPickerDate.equals(appliedFilterDate)) {
                applyDateFilter();
            } else {
                clearDateFilter();
            }
        } else {
            applyDateFilter();
        }
    }

    private void applyDateFilter() {
        if (dpFilterDate == null) return;

        LocalDate targetDate = dpFilterDate.getValue();
        if (targetDate == null) {
            String text = dpFilterDate.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) {
                try {
                    targetDate = dpFilterDate.getConverter().fromString(text.trim());
                    dpFilterDate.setValue(targetDate);
                } catch (Exception ignored) {}
            }
        }

        if (targetDate == null) {
            targetDate = LocalDate.now();
            dpFilterDate.setValue(targetDate);
        }

        isDateFiltered = true;
        appliedFilterDate = targetDate;

        if (btnFilterDate != null && !btnFilterDate.getStyleClass().contains("active")) {
            btnFilterDate.getStyleClass().add("active");
        }
        if (tooltipFilter != null) {
            tooltipFilter.setText("Đang lọc: " + FormatUtils.DATE_FORMATTER.format(appliedFilterDate) + " (Nhấn phễu để hủy lọc)");
        }

        loadFilteredData();
    }

    private void clearDateFilter() {
        isDateFiltered = false;
        appliedFilterDate = null;

        if (dpFilterDate != null) {
            dpFilterDate.setValue(null);
            dpFilterDate.getEditor().clear();
        }

        if (btnFilterDate != null) {
            btnFilterDate.getStyleClass().remove("active");
        }
        if (tooltipFilter != null) {
            tooltipFilter.setText("Nhấn để lọc theo ngày");
        }

        loadFilteredData();
    }

    private Task<PageResult<InventoryTransactionDTO>> runningImportTask;
    private Task<PageResult<InventoryTransactionDTO>> runningExportTask;

    public void loadFilteredData() {
        boolean isExportTabActive = historyTabPane != null && historyTabPane.getSelectionModel().getSelectedItem() == tabExport;
        if (isExportTabActive) {
            exportCurrentPage = 1;
            isExportDirty = false;
            isImportDirty = true;
            loadExportData();
        } else {
            importCurrentPage = 1;
            isImportDirty = false;
            isExportDirty = true;
            loadImportData();
        }
    }

    public void loadAllData() {
        loadFilteredData();
    }

    private void loadImportData() {
        if (runningImportTask != null && runningImportTask.isRunning()) {
            runningImportTask.cancel();
        }

        LocalDate filterDate = isDateFiltered ? appliedFilterDate : null;
        int page = importCurrentPage;

        Task<PageResult<InventoryTransactionDTO>> task = new Task<>() {
            @Override
            protected PageResult<InventoryTransactionDTO> call() {
                return inventoryService.getImportTransactions(null, filterDate, filterDate, page, importPageSize);
            }
        };
        runningImportTask = task;

        task.setOnSucceeded(e -> {
            if (task != runningImportTask) return;
            PageResult<InventoryTransactionDTO> result = task.getValue();
            importTotalRecords = result.getTotalElements();
            importTotalPages = Math.max(1, result.getTotalPages());
            importPageData.setAll(result.getItems());
            TableSyncHelper.resetScrollToTop(tableImport, null);
            PaginationHelper.render(importPaginationBox, lblImportRecordRange, importCurrentPage, importTotalPages,
                    importTotalRecords, importPageSize, p -> {
                        importCurrentPage = p;
                        loadImportData();
                    });
        });

        task.setOnFailed(e -> log.error("Lỗi khi tải lịch sử Nhập kho: ", task.getException()));

        BackgroundTasks.run(task);
    }

    private void loadExportData() {
        if (runningExportTask != null && runningExportTask.isRunning()) {
            runningExportTask.cancel();
        }

        LocalDate filterDate = isDateFiltered ? appliedFilterDate : null;
        int page = exportCurrentPage;

        Task<PageResult<InventoryTransactionDTO>> task = new Task<>() {
            @Override
            protected PageResult<InventoryTransactionDTO> call() {
                return inventoryService.getExportTransactions(null, filterDate, filterDate, page, exportPageSize);
            }
        };
        runningExportTask = task;

        task.setOnSucceeded(e -> {
            if (task != runningExportTask) return;
            PageResult<InventoryTransactionDTO> result = task.getValue();
            exportTotalRecords = result.getTotalElements();
            exportTotalPages = Math.max(1, result.getTotalPages());
            exportPageData.setAll(result.getItems());
            TableSyncHelper.resetScrollToTop(tableExport, null);
            PaginationHelper.render(exportPaginationBox, lblExportRecordRange, exportCurrentPage, exportTotalPages,
                    exportTotalRecords, exportPageSize, p -> {
                        exportCurrentPage = p;
                        loadExportData();
                    });
        });

        task.setOnFailed(e -> log.error("Lỗi khi tải lịch sử Xuất kho: ", task.getException()));

        BackgroundTasks.run(task);
    }

    @FXML
    private void handleClose(ActionEvent event) {
        if (onBackToInventory != null) {
            onBackToInventory.run();
        }
    }

    public void showTransactionDetailDialog(InventoryTransactionDTO tx) {
        if (tx == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/transaction_detail_dialog.fxml"));
            Parent root = loader.load();

            TransactionDetailDialogController controller = loader.getController();
            controller.setTransactionData(tx, inventoryService);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (tableImport != null && tableImport.getScene() != null) {
                dialogStage.initOwner(tableImport.getScene().getWindow());
            }
            dialogStage.setTitle("IMPORT".equalsIgnoreCase(tx.getType()) ? "Chi tiết phiếu nhập kho" : "Chi tiết phiếu xuất kho");

            Scene scene = new Scene(root);

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setWidth(1250);
            dialogStage.setHeight(650);
            dialogStage.setMinWidth(1000);
            dialogStage.setMinHeight(550);
            dialogStage.showAndWait();
        } catch (IOException ex) {
            log.error("Lỗi khi mở popup chi tiết giao dịch kho: ", ex);
        }
    }
}
