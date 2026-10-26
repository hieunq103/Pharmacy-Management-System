package com.pharmacy.inventory.ui;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TransactionDetailDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(TransactionDetailDialogController.class);

    @FXML private Button btnClose;
    @FXML private Label lblDialogTitle;
    @FXML private Label lblTransactionTime;

    // Info card
    @FXML private Label lblEmployee;
    @FXML private Label lblPartnerLabel;
    @FXML private Label lblPartnerValue;
    @FXML private Label lblNote;
    @FXML private Label lblItemCount;
    @FXML private Label lblTotalQuantity;
    @FXML private HBox boxTotalAmount;
    @FXML private Label lblTotalAmount;

    // Search & Counter
    @FXML private TextField txtSearchDetail;
    @FXML private Label lblRowCount;

    // Table & Columns
    @FXML private TableView<InventoryTransactionDetailDTO> tableDetails;
    @FXML private TableColumn<InventoryTransactionDetailDTO, Integer> colIndex;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colMedCode;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colMedName;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colCategory;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colManufacturer;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colLotNumber;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colExpiryDate;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colUnit;
    @FXML private TableColumn<InventoryTransactionDetailDTO, Integer> colQuantity;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colUnitPrice;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colTotalPrice;
    @FXML private TableColumn<InventoryTransactionDetailDTO, String> colReason;

    @FXML private HBox detailPaginationBox;
    @FXML private Label lblDetailRecordRange;

    private final ObservableList<InventoryTransactionDetailDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<InventoryTransactionDetailDTO> filteredData;
    private final ObservableList<InventoryTransactionDetailDTO> pageData = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int pageSize = com.pharmacy.common.AppConstants.DEFAULT_PAGE_SIZE;
    private int totalPages = 1;
    private long totalRecords = 0;

    /** Nút X ở góc phải: đóng cửa sổ chi tiết phiếu nhập / xuất kho. */
    @FXML
    private void handleClose() {
        if (btnClose != null && btnClose.getScene() != null
                && btnClose.getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.close();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        filteredData = new FilteredList<>(masterData, p -> true);
        tableDetails.setItems(pageData);

        com.pharmacy.common.TableSyncHelper.enableAutoRowHeight(tableDetails, null, com.pharmacy.common.AppConstants.DEFAULT_PAGE_SIZE);
        com.pharmacy.common.TableSyncHelper.setupTableScrollProtection(tableDetails, com.pharmacy.common.AppConstants.DEFAULT_PAGE_SIZE);

        if (txtSearchDetail != null) {
            com.pharmacy.common.DebounceUtil.debounce(txtSearchDetail, com.pharmacy.common.AppConstants.SEARCH_DEBOUNCE, text -> {
                applyFilter(text);
            });
        }
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(cellData -> {
            int idx = tableDetails.getItems().indexOf(cellData.getValue());
            int absoluteIndex = (currentPage - 1) * pageSize + idx + 1;
            return new ReadOnlyObjectWrapper<>(absoluteIndex);
        });
        setupLeftAlignedCell(colIndex);

        colMedCode.setCellValueFactory(new PropertyValueFactory<>("medicineCode"));
        colMedCode.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("code-pill");
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    setAlignment(Pos.CENTER_LEFT);
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colMedName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        setupLeftAlignedCell(colMedName);

        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        setupLeftAlignedCell(colCategory);

        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        setupLeftAlignedCell(colManufacturer);

        colLotNumber.setCellValueFactory(new PropertyValueFactory<>("lotNumber"));
        setupLeftAlignedCell(colLotNumber);

        colExpiryDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedExpiryDate()));
        setupLeftAlignedCell(colExpiryDate);

        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        setupLeftAlignedCell(colUnit);

        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        setupLeftAlignedCell(colQuantity);

        colUnitPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getUnitPrice())));
        setupLeftAlignedCell(colUnitPrice);

        colTotalPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getTotalPrice())));
        setupLeftAlignedCell(colTotalPrice);

        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        setupLeftAlignedCell(colReason);
    }

    private <T> void setupLeftAlignedCell(TableColumn<InventoryTransactionDetailDTO, T> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(item));
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });
    }

    public void setTransactionData(InventoryTransactionDTO tx, InventoryService inventoryService) {
        if (tx == null) return;

        boolean isImport = "IMPORT".equalsIgnoreCase(tx.getType());

        // Tiêu đề
        if (isImport) {
            lblDialogTitle.setText("Chi tiết Phiếu Nhập Kho Dược Phẩm");

            lblPartnerLabel.setText("Nhà cung cấp:");
            lblPartnerValue.setText(tx.getSupplierName() != null ? tx.getSupplierName() : "Không xác định");

            if (boxTotalAmount != null) {
                boxTotalAmount.setVisible(true);
                boxTotalAmount.setManaged(true);
                lblTotalAmount.setText(FormatUtils.formatCurrency(tx.getTotalAmount()));
            }

            // Hiện cột giá tiền, ẩn cột lý do
            colUnitPrice.setVisible(true);
            colTotalPrice.setVisible(true);
            colReason.setVisible(false);

        } else {
            lblDialogTitle.setText("Chi tiết Phiếu Xuất Kho / Xuất Hủy Dược Phẩm");

            lblPartnerLabel.setText("Hình thức xuất:");
            lblPartnerValue.setText(tx.getExportTypeLabel() != null ? tx.getExportTypeLabel() : "Xuất kho");

            if (boxTotalAmount != null) {
                boxTotalAmount.setVisible(false);
                boxTotalAmount.setManaged(false);
            }

            // Ẩn cột giá tiền, hiện cột lý do xuất cụ thể
            colUnitPrice.setVisible(false);
            colTotalPrice.setVisible(false);
            colReason.setVisible(true);
        }

        lblTransactionTime.setText("Thời gian lập phiếu: " + tx.getFormattedDateTime());
        lblEmployee.setText(tx.getEmployeeName() != null ? tx.getEmployeeName() : "N/A");
        lblNote.setText(tx.getNote() != null && !tx.getNote().isBlank() ? tx.getNote() : "Không có ghi chú");
        lblItemCount.setText(tx.getItemCount() + " mặt hàng");
        lblTotalQuantity.setText(tx.getTotalQuantity() + " sản phẩm");

        // Tải chi tiết các mặt hàng thuốc trong phiếu từ DB
        Task<List<InventoryTransactionDetailDTO>> task = new Task<>() {
            @Override
            protected List<InventoryTransactionDetailDTO> call() {
                return inventoryService.getTransactionDetails(tx.getId(), tx.getType());
            }
        };

        task.setOnSucceeded(e -> {
            List<InventoryTransactionDetailDTO> list = task.getValue();
            masterData.setAll(list != null ? list : List.of());
            currentPage = 1;
            renderPagination();
            updateRowCount(masterData.size());
        });

        task.setOnFailed(e -> {
            log.error("Lỗi khi tải chi tiết phiếu ID {}: ", tx.getId(), task.getException());
        });

        new Thread(task).start();
    }

    private void renderPagination() {
        totalRecords = filteredData.size();
        totalPages = (int) Math.max(1, Math.ceil((double) totalRecords / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = (int) Math.min(fromIndex + pageSize, totalRecords);

        List<InventoryTransactionDetailDTO> slice;
        if (fromIndex < totalRecords) {
            slice = filteredData.subList(fromIndex, toIndex);
        } else {
            slice = List.of();
        }

        pageData.setAll(slice);
        com.pharmacy.common.TableSyncHelper.resetScrollToTop(tableDetails, null);
        Platform.runLater(() -> com.pharmacy.common.TableSyncHelper.resetScrollToTop(tableDetails, null));

        PaginationHelper.render(
                detailPaginationBox,
                lblDetailRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                pageSize,
                page -> {
                    currentPage = page;
                    com.pharmacy.common.TableSyncHelper.resetScrollToTop(tableDetails, null);
                    Platform.runLater(() -> com.pharmacy.common.TableSyncHelper.resetScrollToTop(tableDetails, null));
                    renderPagination();
                }
        );
    }

    private void applyFilter(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredData.setPredicate(item -> true);
        } else {
            String lower = keyword.trim().toLowerCase();
            filteredData.setPredicate(item -> {
                if (item.getMedicineCode() != null && item.getMedicineCode().toLowerCase().contains(lower)) return true;
                if (item.getMedicineName() != null && item.getMedicineName().toLowerCase().contains(lower)) return true;
                if (item.getLotNumber() != null && item.getLotNumber().toLowerCase().contains(lower)) return true;
                if (item.getCategoryName() != null && item.getCategoryName().toLowerCase().contains(lower)) return true;
                if (item.getManufacturer() != null && item.getManufacturer().toLowerCase().contains(lower)) return true;
                return item.getReason() != null && item.getReason().toLowerCase().contains(lower);
            });
        }
        currentPage = 1;
        renderPagination();
        updateRowCount(filteredData.size());
    }

    private void updateRowCount(int count) {
        if (lblRowCount != null) {
            lblRowCount.setText("Danh sách " + count + " mặt hàng thuốc:");
        }
    }
}
