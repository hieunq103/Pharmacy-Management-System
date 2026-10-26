package com.pharmacy.inventory.ui;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.common.TableSyncHelper;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class MedicineLotDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MedicineLotDialogController.class);

    @FXML private Button btnClose;
    @FXML private Label lblMedicineCode;
    @FXML private Label lblPrescriptionTag;
    @FXML private Label lblMedicineName;
    @FXML private Label lblActiveIngredient;
    @FXML private Label lblCategory;
    @FXML private Label lblManufacturer;
    @FXML private Label lblUnit;
    @FXML private Label lblSellPrice;

    @FXML private TextField txtSearchLot;
    @FXML private ComboBox<String> cbStatusFilter;

    @FXML private TableView<MedicineLotDTO> tableLots;
    @FXML private TableColumn<MedicineLotDTO, Number> colIndex;
    @FXML private TableColumn<MedicineLotDTO, String> colLotNumber;
    @FXML private TableColumn<MedicineLotDTO, LocalDate> colExpiryDate;
    @FXML private TableColumn<MedicineLotDTO, MedicineLotDTO> colStatus;
    @FXML private TableColumn<MedicineLotDTO, String> colQuantityIn;
    @FXML private TableColumn<MedicineLotDTO, String> colQuantityRemaining;
    @FXML private TableColumn<MedicineLotDTO, String> colImportPrice;
    @FXML private TableColumn<MedicineLotDTO, LocalDateTime> colTransactionDate;
    @FXML private TableColumn<MedicineLotDTO, String> colManufacturer;
    @FXML private TableColumn<MedicineLotDTO, String> colSupplier;

    @FXML private HBox lotPaginationBox;
    @FXML private Label lblLotRecordRange;

    private MedicineInventoryDTO medicineDTO;
    private InventoryRepository repository;

    private final ObservableList<MedicineLotDTO> masterLots = FXCollections.observableArrayList();
    private FilteredList<MedicineLotDTO> filteredLots;
    private SortedList<MedicineLotDTO> sortedLots;
    private final ObservableList<MedicineLotDTO> pageLots = FXCollections.observableArrayList();

    private int currentPage = 1;
    private final int pageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int totalPages = 1;
    private long totalRecords = 0;

    private enum SortTarget { EXPIRY_DATE, TRANSACTION_DATE }
    private SortTarget currentSortTarget = SortTarget.EXPIRY_DATE;
    private boolean isSortAscending = true;

    private Label lblExpirySortIcon;
    private Label lblTransactionSortIcon;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        TableSyncHelper.enableAutoRowHeight(tableLots, null, AppConstants.DEFAULT_PAGE_SIZE);
        TableSyncHelper.setupTableScrollProtection(tableLots, AppConstants.DEFAULT_PAGE_SIZE);
    }

    /** Nút X tròn đỏ ở góc phải: đóng cửa sổ chi tiết lô hàng. */
    @FXML
    private void handleClose() {
        if (btnClose != null && btnClose.getScene() != null
                && btnClose.getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.close();
        }
    }

    public void setMedicineData(MedicineInventoryDTO dto, InventoryRepository repo) {
        this.medicineDTO = dto;
        this.repository = repo != null ? repo : new InventoryRepository();

        if (dto == null) return;

        // 1. Hiển thị thông tin thuốc cơ bản
        lblMedicineCode.setText(dto.getMedicineCode() != null ? dto.getMedicineCode() : "");
        lblMedicineName.setText(dto.getMedicineName() != null ? dto.getMedicineName() : "");
        lblActiveIngredient.setText(dto.getActiveIngredient() != null && !dto.getActiveIngredient().isBlank()
                ? dto.getActiveIngredient() : "—");
        lblCategory.setText(dto.getCategoryName() != null && !dto.getCategoryName().isBlank()
                ? dto.getCategoryName() : "—");
        lblManufacturer.setText(dto.getManufacturer() != null && !dto.getManufacturer().isBlank()
                ? dto.getManufacturer() : "—");
        lblUnit.setText(dto.getUnit() != null ? dto.getUnit() : "—");

        if (dto.getSellPrice() != null) {
            lblSellPrice.setText(com.pharmacy.common.FormatUtils.formatCurrency(dto.getSellPrice()) + " đ");
        } else {
            lblSellPrice.setText("0 đ");
        }

        boolean needPrescription = Boolean.TRUE.equals(dto.getRequiresPrescription());
        lblPrescriptionTag.setVisible(needPrescription);
        lblPrescriptionTag.setManaged(needPrescription);

        // 2. Tải danh sách lô hàng trong luồng nền
        loadLotData(dto.getMedicineId());
    }

    private void setupTableColumns() {
        // Cột STT - Căn trái
        colIndex.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                (currentPage - 1) * pageSize + tableLots.getItems().indexOf(cellData.getValue()) + 1
        ));
        colIndex.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView() == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(String.valueOf((currentPage - 1) * pageSize + getIndex() + 1));
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Số lô - Căn trái
        colLotNumber.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getLotNumber()));
        colLotNumber.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #1E293B; -fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Hạn dùng - Căn trái
        colExpiryDate.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpiryDate()));
        colExpiryDate.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(FormatUtils.formatDate(item));
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Trạng thái (Pill Badge) - Căn trái
        colStatus.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
                badge.getStyleClass().add("lot-badge-pill");
                badge.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(MedicineLotDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    badge.setText(item.getStatusLabel());
                    badge.getStyleClass().removeAll(
                            "lot-badge-active", "lot-badge-near-expiry",
                            "lot-badge-expired", "lot-badge-depleted"
                    );
                    badge.getStyleClass().add(item.getStatusStyleClass());
                    setText(null);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Số lượng nhập - Căn trái
        colQuantityIn.setCellValueFactory(cell -> {
            String unit = medicineDTO != null && medicineDTO.getUnit() != null ? " " + medicineDTO.getUnit() : "";
            return new ReadOnlyObjectWrapper<>(cell.getValue().getQuantityIn() + unit);
        });
        colQuantityIn.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Số lượng tồn - Căn trái
        colQuantityRemaining.setCellValueFactory(cell -> {
            String unit = medicineDTO != null && medicineDTO.getUnit() != null ? " " + medicineDTO.getUnit() : "";
            return new ReadOnlyObjectWrapper<>(cell.getValue().getQuantityRemaining() + unit);
        });
        colQuantityRemaining.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-text-fill: #0F172A; -fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Giá nhập - Căn trái
        colImportPrice.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getFormattedImportPrice()));
        colImportPrice.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Ngày nhập - Căn trái
        colTransactionDate.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTransactionAt()));
        colTransactionDate.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(FormatUtils.formatDateTime(item));
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Nhà sản xuất - Căn trái
        colManufacturer.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().getManufacturer() != null && !cell.getValue().getManufacturer().isBlank()
                        ? cell.getValue().getManufacturer() : "—"));
        colManufacturer.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Cột Nhà cung cấp - Căn trái
        Label lblSupplierTitle = new Label("Nhà cung cấp");
        lblSupplierTitle.setStyle("-fx-alignment: CENTER_LEFT;");
        colSupplier.setText(null);
        colSupplier.setGraphic(lblSupplierTitle);

        colSupplier.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getSupplierName()));
        colSupplier.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("lot-col-left");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                    setTooltip(new Tooltip(item));
                }
            }
        });

        setupSortableHeaders();
        tableLots.setPlaceholder(new Label("Thuốc này hiện chưa có lô hàng nào được ghi nhận trong kho."));
    }

    private void setupSortableHeaders() {
        // Cột Hạn sử dụng
        Label lblExpiryTitle = new Label("Hạn sử dụng");
        lblExpiryTitle.getStyleClass().add("label");
        lblExpirySortIcon = new Label("▴");
        lblExpirySortIcon.getStyleClass().addAll("btn-header-sort", "sort-active");

        HBox expiryHeader = new HBox(8, lblExpiryTitle, lblExpirySortIcon);
        expiryHeader.setAlignment(Pos.CENTER_LEFT);
        expiryHeader.setCursor(Cursor.HAND);
        expiryHeader.setOnMouseClicked(e -> toggleSort(SortTarget.EXPIRY_DATE));

        colExpiryDate.setText(null);
        colExpiryDate.setGraphic(expiryHeader);
        colExpiryDate.setSortable(false);

        // Cột Ngày nhập
        Label lblTransTitle = new Label("Ngày nhập");
        lblTransTitle.getStyleClass().add("label");
        lblTransactionSortIcon = new Label("▾");
        lblTransactionSortIcon.getStyleClass().add("btn-header-sort");

        HBox transHeader = new HBox(8, lblTransTitle, lblTransactionSortIcon);
        transHeader.setAlignment(Pos.CENTER_LEFT);
        transHeader.setCursor(Cursor.HAND);
        transHeader.setOnMouseClicked(e -> toggleSort(SortTarget.TRANSACTION_DATE));

        colTransactionDate.setText(null);
        colTransactionDate.setGraphic(transHeader);
        colTransactionDate.setSortable(false);
    }

    private void toggleSort(SortTarget target) {
        if (currentSortTarget == target) {
            isSortAscending = !isSortAscending;
        } else {
            currentSortTarget = target;
            isSortAscending = true;
        }
        updateSortUIAndApply();
    }

    private void updateSortUIAndApply() {
        if (lblExpirySortIcon == null || lblTransactionSortIcon == null || sortedLots == null) return;

        if (currentSortTarget == SortTarget.EXPIRY_DATE) {
            lblExpirySortIcon.setText(isSortAscending ? "▴" : "▾");
            if (!lblExpirySortIcon.getStyleClass().contains("sort-active")) {
                lblExpirySortIcon.getStyleClass().add("sort-active");
            }

            lblTransactionSortIcon.setText("▾");
            lblTransactionSortIcon.getStyleClass().remove("sort-active");

            Comparator<MedicineLotDTO> comp = Comparator.comparing(
                    MedicineLotDTO::getExpiryDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            sortedLots.setComparator(isSortAscending ? comp : comp.reversed());
        } else {
            lblTransactionSortIcon.setText(isSortAscending ? "▴" : "▾");
            if (!lblTransactionSortIcon.getStyleClass().contains("sort-active")) {
                lblTransactionSortIcon.getStyleClass().add("sort-active");
            }

            lblExpirySortIcon.setText("▾");
            lblExpirySortIcon.getStyleClass().remove("sort-active");

            Comparator<MedicineLotDTO> comp = Comparator.comparing(
                    MedicineLotDTO::getTransactionAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            sortedLots.setComparator(isSortAscending ? comp : comp.reversed());
        }

        renderPagination();
    }

    private void renderPagination() {
        if (sortedLots == null) return;
        totalRecords = sortedLots.size();
        totalPages = (int) Math.max(1, Math.ceil((double) totalRecords / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = (int) Math.min(fromIndex + pageSize, totalRecords);

        List<MedicineLotDTO> slice;
        if (totalRecords > 0 && fromIndex < totalRecords) {
            slice = sortedLots.subList(fromIndex, toIndex);
        } else {
            slice = List.of();
        }

        pageLots.setAll(slice);
        TableSyncHelper.resetScrollToTop(tableLots, null);
        Platform.runLater(() -> TableSyncHelper.resetScrollToTop(tableLots, null));

        PaginationHelper.render(
                lotPaginationBox,
                lblLotRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                pageSize,
                page -> {
                    currentPage = page;
                    TableSyncHelper.resetScrollToTop(tableLots, null);
                    Platform.runLater(() -> TableSyncHelper.resetScrollToTop(tableLots, null));
                    renderPagination();
                }
        );

        tableLots.refresh();
    }

    private void setupFilters() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái",
                "Còn hạn",
                "Cận hạn (<=90 ngày)",
                "Đã hết hạn",
                "Đã hết hàng"
        ));
        cbStatusFilter.setValue("Tất cả trạng thái");

        filteredLots = new FilteredList<>(masterLots, p -> true);
        sortedLots = new SortedList<>(filteredLots);
        tableLots.setItems(pageLots);
        updateSortUIAndApply();

        // Lọc khi người dùng tìm kiếm theo text (Debounce)
        com.pharmacy.common.DebounceUtil.debounce(txtSearchLot, AppConstants.SEARCH_DEBOUNCE, text -> reapplyLotFilters());

        // Lọc khi người dùng chọn ComboBox
        cbStatusFilter.setOnAction(e -> reapplyLotFilters());
    }

    private void reapplyLotFilters() {
        String keyword = txtSearchLot.getText() != null ? txtSearchLot.getText().trim().toLowerCase() : "";
        String statusFilter = cbStatusFilter.getValue();

        filteredLots.setPredicate(lot -> {
            if (lot == null) return false;

            // 1. Lọc theo từ khóa tìm kiếm (số lô, nhà sản xuất hoặc nhà cung cấp)
            if (!keyword.isEmpty()) {
                boolean matchLot = lot.getLotNumber() != null && lot.getLotNumber().toLowerCase().contains(keyword);
                boolean matchSupplier = lot.getSupplierName() != null && lot.getSupplierName().toLowerCase().contains(keyword);
                boolean matchManufacturer = lot.getManufacturer() != null && lot.getManufacturer().toLowerCase().contains(keyword);
                if (!matchLot && !matchSupplier && !matchManufacturer) return false;
            }

            // 2. Lọc theo trạng thái
            if (statusFilter != null && !statusFilter.equals("Tất cả trạng thái")) {
                if (statusFilter.startsWith("Còn hạn") && lot.getStatus() != MedicineLotDTO.LotStatus.ACTIVE) {
                    return false;
                }
                if (statusFilter.startsWith("Cận hạn") && lot.getStatus() != MedicineLotDTO.LotStatus.NEAR_EXPIRY) {
                    return false;
                }
                if (statusFilter.startsWith("Đã hết hạn") && lot.getStatus() != MedicineLotDTO.LotStatus.EXPIRED) {
                    return false;
                }
                if (statusFilter.startsWith("Đã hết hàng") && lot.getStatus() != MedicineLotDTO.LotStatus.DEPLETED) {
                    return false;
                }
            }

            return true;
        });

        currentPage = 1;
        renderPagination();
    }

    private void loadLotData(int medicineId) {
        Task<List<MedicineLotDTO>> task = new Task<>() {
            @Override
            protected List<MedicineLotDTO> call() {
                return repository.getLotsByMedicineId(medicineId);
            }
        };

        task.setOnSucceeded(e -> {
            List<MedicineLotDTO> list = task.getValue();
            if (list != null) {
                masterLots.setAll(list);
                currentPage = 1;
                reapplyLotFilters();
                updateSortUIAndApply();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("Lỗi khi tải dữ liệu lô hàng của thuốc ID {}: ", medicineId, ex);
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}
