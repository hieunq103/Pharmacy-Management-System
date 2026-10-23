package com.pharmacy.inventory.ui;

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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryHistoryController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(InventoryHistoryController.class);

    private final InventoryService inventoryService = new InventoryServiceImpl();

    // Callback quay lại danh sách kho thuốc
    private Runnable onBackToInventory;

    // Header & Filter controls
    @FXML private DatePicker dpFilterDate;
    @FXML private Button btnFilterDate;
    @FXML private Tooltip tooltipFilter;

    // Quản lý trạng thái lọc theo ngày (Toggle Filter: xám = tất cả, xanh = lọc ngày đó)
    private boolean isDateFiltered = false;
    private LocalDate appliedFilterDate = null;

    // Tabs
    @FXML private TabPane historyTabPane;
    @FXML private Tab tabImport;
    @FXML private Tab tabExport;

    // Tab Nhập kho controls
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

    // Tab Xuất kho controls
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

    // Dữ liệu phân trang cho Nhập kho
    private final ObservableList<InventoryTransactionDTO> importPageData = FXCollections.observableArrayList();
    private final List<InventoryTransactionDTO> importMasterList = new ArrayList<>();
    private int importCurrentPage = 1;
    private final int importPageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int importTotalPages = 1;
    private long importTotalRecords = 0;

    // Dữ liệu phân trang cho Xuất kho
    private final ObservableList<InventoryTransactionDTO> exportPageData = FXCollections.observableArrayList();
    private final List<InventoryTransactionDTO> exportMasterList = new ArrayList<>();
    private int exportCurrentPage = 1;
    private final int exportPageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int exportTotalPages = 1;
    private long exportTotalRecords = 0;

    // Quản lý lazy load cho 2 tab để tránh tải thừa dữ liệu
    private boolean isImportDirty = false;
    private boolean isExportDirty = false;

    private final ExecutorService historyExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "history-load-worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupImportTableColumns();
        setupExportTableColumns();

        tableImport.setItems(importPageData);
        tableExport.setItems(exportPageData);

        // 1. Tự động căn chỉnh chiều cao dòng lấp đầy 7 dòng không để lại khoảng trắng phía dưới
        TableSyncHelper.enableAutoRowHeight(tableImport, null, AppConstants.DEFAULT_PAGE_SIZE);
        TableSyncHelper.enableAutoRowHeight(tableExport, null, AppConstants.DEFAULT_PAGE_SIZE);

        // 2. Chặn lỗi vuốt trôi hàng của VirtualFlow (khắc phục lỗi vuốt lên chuyển trang chỉ còn 1 dòng)
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

        // Nhấp đúp chuột 2 lần vào hàng để mở chi tiết
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

        // Nhấp đúp chuột 2 lần vào hàng để mở chi tiết
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

            // Khi gõ ngày và nhấn Enter trong ô ngày -> kích hoạt áp dụng lọc ngay
            dpFilterDate.getEditor().setOnAction(e -> applyDateFilter());

            // Khi người dùng xóa trắng ô ngày trong editor -> hủy lọc về màu xám nếu đang lọc
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
            // Đang lọc màu xanh:
            // Nếu người dùng chọn/nhập sang một ngày khác -> áp dụng lọc cho ngày mới (vẫn giữ màu xanh)
            if (currentPickerDate != null && !currentPickerDate.equals(appliedFilterDate)) {
                applyDateFilter();
            } else {
                // Nếu bấm lại phễu khi đang lọc cùng ngày đó -> HỦY LỌC -> chuyển về màu xám, xem tất cả ngày
                clearDateFilter();
            }
        } else {
            // Đang màu xám -> bấm phễu để kích hoạt lọc màu xanh
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

        // Nếu người dùng chưa chọn ngày nào mà bấm phễu lọc -> mặc định lấy ngày hiện tại
        if (targetDate == null) {
            targetDate = LocalDate.now();
            dpFilterDate.setValue(targetDate);
        }

        isDateFiltered = true;
        appliedFilterDate = targetDate;

        // Chuyển phễu sang màu xanh
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

        // Chuyển phễu về màu xám
        if (btnFilterDate != null) {
            btnFilterDate.getStyleClass().remove("active");
        }
        if (tooltipFilter != null) {
            tooltipFilter.setText("Nhấn để lọc theo ngày");
        }

        loadFilteredData();
    }

    private Task<List<InventoryTransactionDTO>> runningImportTask;
    private Task<List<InventoryTransactionDTO>> runningExportTask;

    /**
     * Tải dữ liệu chỉ cho tab đang hoạt động để tối ưu hiệu năng (tránh spam 2 query cùng lúc).
     */
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

        runningImportTask = new Task<>() {
            @Override
            protected List<InventoryTransactionDTO> call() {
                return inventoryService.getImportTransactions(null, filterDate, filterDate);
            }
        };

        runningImportTask.setOnSucceeded(e -> {
            List<InventoryTransactionDTO> list = runningImportTask.getValue();
            importMasterList.clear();
            if (list != null) {
                importMasterList.addAll(list);
            }
            importTotalRecords = importMasterList.size();
            importTotalPages = (int) Math.max(1, Math.ceil((double) importTotalRecords / importPageSize));
            if (importCurrentPage > importTotalPages) importCurrentPage = importTotalPages;
            if (importCurrentPage < 1) importCurrentPage = 1;
            renderImportPagination();
        });

        runningImportTask.setOnFailed(e -> {
            log.error("Lỗi khi tải lịch sử Nhập kho: ", runningImportTask.getException());
        });

        historyExecutor.submit(runningImportTask);
    }

    private void loadExportData() {
        if (runningExportTask != null && runningExportTask.isRunning()) {
            runningExportTask.cancel();
        }

        LocalDate filterDate = isDateFiltered ? appliedFilterDate : null;

        runningExportTask = new Task<>() {
            @Override
            protected List<InventoryTransactionDTO> call() {
                return inventoryService.getExportTransactions(null, filterDate, filterDate);
            }
        };

        runningExportTask.setOnSucceeded(e -> {
            List<InventoryTransactionDTO> list = runningExportTask.getValue();
            exportMasterList.clear();
            if (list != null) {
                exportMasterList.addAll(list);
            }
            exportTotalRecords = exportMasterList.size();
            exportTotalPages = (int) Math.max(1, Math.ceil((double) exportTotalRecords / exportPageSize));
            if (exportCurrentPage > exportTotalPages) exportCurrentPage = exportTotalPages;
            if (exportCurrentPage < 1) exportCurrentPage = 1;
            renderExportPagination();
        });

        runningExportTask.setOnFailed(e -> {
            log.error("Lỗi khi tải lịch sử Xuất kho: ", runningExportTask.getException());
        });

        historyExecutor.submit(runningExportTask);
    }

    private void renderImportPagination() {
        int fromIndex = (importCurrentPage - 1) * importPageSize;
        int toIndex = (int) Math.min(fromIndex + importPageSize, importTotalRecords);

        List<InventoryTransactionDTO> pageSlice;
        if (fromIndex < importTotalRecords) {
            pageSlice = importMasterList.subList(fromIndex, toIndex);
        } else {
            pageSlice = List.of();
        }

        importPageData.setAll(pageSlice);
        TableSyncHelper.resetScrollToTop(tableImport, null);

        PaginationHelper.render(
                importPaginationBox,
                lblImportRecordRange,
                importCurrentPage,
                importTotalPages,
                importTotalRecords,
                importPageSize,
                page -> {
                    importCurrentPage = page;
                    TableSyncHelper.resetScrollToTop(tableImport, null);
                    renderImportPagination();
                }
        );
    }

    private void renderExportPagination() {
        int fromIndex = (exportCurrentPage - 1) * exportPageSize;
        int toIndex = (int) Math.min(fromIndex + exportPageSize, exportTotalRecords);

        List<InventoryTransactionDTO> pageSlice;
        if (fromIndex < exportTotalRecords) {
            pageSlice = exportMasterList.subList(fromIndex, toIndex);
        } else {
            pageSlice = List.of();
        }

        exportPageData.setAll(pageSlice);
        TableSyncHelper.resetScrollToTop(tableExport, null);

        PaginationHelper.render(
                exportPaginationBox,
                lblExportRecordRange,
                exportCurrentPage,
                exportTotalPages,
                exportTotalRecords,
                exportPageSize,
                page -> {
                    exportCurrentPage = page;
                    TableSyncHelper.resetScrollToTop(tableExport, null);
                    renderExportPagination();
                }
        );
    }

    @FXML
    private void handleClose(ActionEvent event) {
        if (onBackToInventory != null) {
            onBackToInventory.run();
        }
    }

    /**
     * Mở popup chi tiết của lần xuất / nhập kho tương tự như popup chi tiết lô hàng của thuốc.
     */
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
            // ESC key để đóng
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    dialogStage.close();
                }
            });

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
