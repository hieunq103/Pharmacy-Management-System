package com.pharmacy.sales;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.DebounceUtil;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.common.TableSyncHelper;
import com.pharmacy.common.UserSession;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller quản lý lịch sử bán hàng.
 * - Phân quyền: Quản lý xem toàn hệ thống, Nhân viên chỉ xem đơn của chính mình.
 * - Lọc Theo ngày (mặc định hôm nay) hoặc Theo tháng (giống trang Báo cáo).
 * - Bấm vào từng dòng để mở xem chi tiết đơn hàng.
 */
public class SalesHistoryController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SalesHistoryController.class);

    private final SalesService salesService = new SalesServiceImpl();

    // Callback đóng màn hình lịch sử quay về bán hàng
    private Runnable onCloseHistory;

    // Header controls
    @FXML private TextField txtSearch;
    @FXML private DatePicker dpFilterDate;
    @FXML private Button btnFilterDay;
    @FXML private Button btnFilterMonth;
    @FXML private TextField txtFilterYear;
    @FXML private ComboBox<String> cbFilterMonth;

    // Table controls
    @FXML private TableView<SalesOrderDTO> tableSalesOrders;
    @FXML private TableColumn<SalesOrderDTO, String> colIndex;
    @FXML private TableColumn<SalesOrderDTO, String> colCode;
    @FXML private TableColumn<SalesOrderDTO, String> colDateTime;
    @FXML private TableColumn<SalesOrderDTO, String> colEmployee;
    @FXML private TableColumn<SalesOrderDTO, String> colCustomerPhone;
    @FXML private TableColumn<SalesOrderDTO, String> colItemSummary;
    @FXML private TableColumn<SalesOrderDTO, String> colSubtotal;
    @FXML private TableColumn<SalesOrderDTO, String> colDiscount;
    @FXML private TableColumn<SalesOrderDTO, String> colTotalAmount;
    @FXML private TableColumn<SalesOrderDTO, String> colPaymentMethod;

    @FXML private VBox paneEmptyHistory;

    // Pagination controls
    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;

    // Dữ liệu và trạng thái
    private final ObservableList<SalesOrderDTO> pageData = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int pageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int totalPages = 1;
    private long totalRecords = 0;

    private LocalDate selectedDate = LocalDate.now();
    private boolean monthMode = false;
    private int selectedYear = LocalDate.now().getYear();
    private int selectedMonth = LocalDate.now().getMonthValue();
    // Chặn sự kiện onAction khi chương trình tự gán giá trị cho các ô lọc
    private boolean updatingControls = false;
    private String currentKeyword = "";
    private Integer filterEmployeeId = null;
    private boolean isManager = true;

    private final ExecutorService historyExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sales-history-loader");
        t.setDaemon(true);
        return t;
    });

    private Task<PageResult<SalesOrderDTO>> runningTask;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRoleAccess();
        setupFilterControls();
        setupTableColumns();
        setupEventHandlers();

        tableSalesOrders.setItems(pageData);

        // Tự động căn chỉnh chiều cao dòng và chống giật VirtualFlow
        TableSyncHelper.enableAutoRowHeight(tableSalesOrders, null, pageSize);
        TableSyncHelper.setupTableScrollProtection(tableSalesOrders, pageSize);

        // Tải dữ liệu ngày hôm nay
        loadSalesHistoryData();
    }

    public void setOnCloseHistory(Runnable onCloseHistory) {
        this.onCloseHistory = onCloseHistory;
    }

    /**
     * Xác định quyền hạn: Quản lý xem lịch sử của toàn cửa hàng, Nhân viên chỉ xem đơn của bản thân.
     */
    private void setupRoleAccess() {
        if (UserSession.isLoggedIn()) {
            isManager = UserSession.getInstance().isManager();
            // Cùng phạm vi với Tổng quan/Báo cáo: Quản lý xem toàn cửa hàng, Nhân viên chỉ xem đơn của mình
            filterEmployeeId = UserSession.scopeEmployeeId(null);
        } else {
            // Mặc định an toàn nếu chưa đăng nhập (môi trường dev/test)
            isManager = true;
            filterEmployeeId = null;
        }
    }

    /**
     * Khởi tạo bộ lọc: DatePicker dd/MM/yyyy (mặc định hôm nay), ô Năm và danh sách Tháng.
     */
    private void setupFilterControls() {
        selectedDate = LocalDate.now();
        updatingControls = true;
        try {
            if (dpFilterDate != null) {
                dpFilterDate.setValue(selectedDate);
                FormatUtils.setupDatePicker(dpFilterDate);
                // Khi người dùng gõ ngày rồi ấn Enter -> áp dụng lọc ngay
                dpFilterDate.getEditor().setOnAction(e -> applyDateFilter());
            }
            if (txtFilterYear != null) {
                txtFilterYear.setText(String.valueOf(selectedYear));
                txtFilterYear.focusedProperty().addListener((obs, wasFocused, focused) -> {
                    if (!focused) handleYearAction(null);
                });
            }
            if (cbFilterMonth != null) {
                cbFilterMonth.getItems().clear();
                for (int m = 1; m <= 12; m++) {
                    cbFilterMonth.getItems().add("Tháng " + m);
                }
                cbFilterMonth.setValue("Tháng " + selectedMonth);
            }
        } finally {
            updatingControls = false;
        }
        updateFilterButtonStyles();
    }

    private void updateFilterButtonStyles() {
        if (btnFilterDay != null) {
            btnFilterDay.getStyleClass().remove("active");
            if (!monthMode) btnFilterDay.getStyleClass().add("active");
        }
        if (btnFilterMonth != null) {
            btnFilterMonth.getStyleClass().remove("active");
            if (monthMode) btnFilterMonth.getStyleClass().add("active");
        }
    }

    /**
     * Mở lịch sử bán hàng của cả một tháng (dùng khi bấm "Xem đầy đủ..." ở trang Báo cáo).
     */
    public void showMonth(int year, int month) {
        selectedYear = year;
        selectedMonth = month;
        monthMode = true;
        updatingControls = true;
        try {
            if (txtFilterYear != null) txtFilterYear.setText(String.valueOf(year));
            if (cbFilterMonth != null) cbFilterMonth.setValue("Tháng " + month);
        } finally {
            updatingControls = false;
        }
        updateFilterButtonStyles();
        currentPage = 1;
        loadSalesHistoryData();
    }

    /** Mở lịch sử bán hàng của một ngày (dùng khi bấm thẻ Doanh số/Hóa đơn hôm nay ở Tổng quan). */
    public void showDate(LocalDate date) {
        selectedDate = date;
        monthMode = false;
        updatingControls = true;
        try {
            if (dpFilterDate != null) dpFilterDate.setValue(date);
        } finally {
            updatingControls = false;
        }
        updateFilterButtonStyles();
        currentPage = 1;
        loadSalesHistoryData();
    }

    @FXML
    private void handleSelectDayMode(ActionEvent event) {
        applyDateFilter();
    }

    @FXML
    private void handleSelectMonthMode(ActionEvent event) {
        if (!readYearInput()) return;
        readMonthInput();
        monthMode = true;
        updateFilterButtonStyles();
        currentPage = 1;
        loadSalesHistoryData();
    }

    @FXML
    private void handleYearAction(ActionEvent event) {
        if (updatingControls) return;
        int before = selectedYear;
        if (!readYearInput()) return;
        // Nhập năm (Enter) hoặc đổi năm -> chuyển sang lọc Theo tháng
        if (event != null || before != selectedYear) {
            monthMode = true;
            updateFilterButtonStyles();
            currentPage = 1;
            loadSalesHistoryData();
        }
    }

    @FXML
    private void handleMonthChanged(ActionEvent event) {
        if (updatingControls) return;
        readMonthInput();
        monthMode = true;
        updateFilterButtonStyles();
        currentPage = 1;
        loadSalesHistoryData();
    }

    /** Đọc ô Năm; trả về false (và khôi phục giá trị cũ) nếu năm không hợp lệ. */
    private boolean readYearInput() {
        if (txtFilterYear == null) return true;
        String text = txtFilterYear.getText() != null ? txtFilterYear.getText().trim() : "";
        try {
            int y = Integer.parseInt(text);
            if (y >= 2000 && y <= 2100) {
                selectedYear = y;
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        com.pharmacy.common.DialogUtils.showWarning("Năm không hợp lệ",
                "Vui lòng nhập năm từ 2000 trở đi (ví dụ: " + LocalDate.now().getYear() + ").");
        txtFilterYear.setText(String.valueOf(selectedYear));
        return false;
    }

    private void readMonthInput() {
        if (cbFilterMonth != null && cbFilterMonth.getValue() != null) {
            try {
                selectedMonth = Integer.parseInt(cbFilterMonth.getValue().replace("Tháng", "").trim());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(cellData -> {
            int idx = (currentPage - 1) * pageSize + tableSalesOrders.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        setupLeftAlignedCell(colIndex);

        colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        setupLeftAlignedCell(colCode);

        colDateTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedTime()));
        setupLeftAlignedCell(colDateTime);

        colEmployee.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getEmployeeName();
            String code = cellData.getValue().getEmployeeCode();
            if (code != null && !code.isBlank()) {
                name = name + " (" + code + ")";
            }
            return new SimpleStringProperty(name != null ? name : "-");
        });
        setupLeftAlignedCell(colEmployee);

        colCustomerPhone.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCustomerPhone()));
        setupLeftAlignedCell(colCustomerPhone);

        colItemSummary.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedItemSummary()));
        setupLeftAlignedCell(colItemSummary);

        colSubtotal.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedSubtotal()));
        setupLeftAlignedCell(colSubtotal);

        colDiscount.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedDiscount()));
        setupLeftAlignedCell(colDiscount);

        colTotalAmount.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedTotal()));
        setupLeftAlignedCell(colTotalAmount);

        colPaymentMethod.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedPaymentMethod()));
        setupLeftAlignedCell(colPaymentMethod);

        // Bấm vào từng dòng sẽ mở popup chi tiết đơn hàng
        tableSalesOrders.setRowFactory(tv -> {
            TableRow<SalesOrderDTO> row = new TableRow<>();
            row.setStyle("-fx-cursor: hand;");
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    showOrderDetailDialog(row.getItem());
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
        if (txtSearch != null) {
            DebounceUtil.debounce(txtSearch, Duration.millis(250), kw -> {
                currentKeyword = kw != null ? kw.trim() : "";
                currentPage = 1;
                loadSalesHistoryData();
            });
        }
    }

    @FXML
    private void handleFilterDate(ActionEvent event) {
        applyDateFilter();
    }

    private void applyDateFilter() {
        if (dpFilterDate == null || updatingControls) return;

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

        // Mặc định là ngày hôm nay nếu chưa chọn ngày
        if (targetDate == null) {
            targetDate = LocalDate.now();
            dpFilterDate.setValue(targetDate);
        }

        selectedDate = targetDate;
        monthMode = false;
        updateFilterButtonStyles();
        currentPage = 1;
        loadSalesHistoryData();
    }

    /**
     * Tải dữ liệu lịch sử bán hàng (theo ngày hoặc cả tháng) bất đồng bộ để giữ UI luôn mượt mà.
     */
    public void loadSalesHistoryData() {
        setupRoleAccess();
        if (runningTask != null && runningTask.isRunning()) {
            runningTask.cancel();
        }

        final LocalDate fromDate = monthMode ? LocalDate.of(selectedYear, selectedMonth, 1) : selectedDate;
        final LocalDate toDate = monthMode ? fromDate.withDayOfMonth(fromDate.lengthOfMonth()) : selectedDate;
        final Integer empId = filterEmployeeId;
        final String keyword = currentKeyword;
        final int page = currentPage;
        final int size = pageSize;

        runningTask = new Task<>() {
            @Override
            protected PageResult<SalesOrderDTO> call() {
                return salesService.getSalesHistory(fromDate, toDate, empId, keyword, page, size);
            }
        };

        runningTask.setOnSucceeded(e -> {
            PageResult<SalesOrderDTO> result = runningTask.getValue();
            pageData.clear();
            if (result != null && result.getItems() != null) {
                pageData.addAll(result.getItems());
                totalRecords = result.getTotalElements();
                totalPages = result.getTotalPages();
            } else {
                totalRecords = 0;
                totalPages = 1;
            }

            if (paneEmptyHistory != null) {
                paneEmptyHistory.setVisible(pageData.isEmpty());
            }

            renderPagination();
            Platform.runLater(() -> TableSyncHelper.resetScrollToTop(tableSalesOrders, null));
        });

        runningTask.setOnFailed(e -> {
            log.error("Lỗi khi tải lịch sử bán hàng: ", runningTask.getException());
        });

        historyExecutor.submit(runningTask);
    }

    private void renderPagination() {
        PaginationHelper.render(
                paginationBox,
                lblRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                pageSize,
                page -> {
                    currentPage = page;
                    loadSalesHistoryData();
                }
        );
    }

    @FXML
    private void handleClose(ActionEvent event) {
        if (onCloseHistory != null) {
            onCloseHistory.run();
        }
    }

    /**
     * Hiển thị popup modal chi tiết đơn hàng khi người dùng click vào dòng.
     */
    private void showOrderDetailDialog(SalesOrderDTO order) {
        if (order == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sales/sales_order_detail_dialog.fxml"));
            Parent root = loader.load();

            SalesOrderDetailDialogController controller = loader.getController();

            // Lấy danh sách mặt hàng của đơn hàng này
            List<SalesOrderDetailDTO> items = salesService.getSalesOrderDetails(order.getId());
            controller.setOrderData(order, items);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (tableSalesOrders != null && tableSalesOrders.getScene() != null) {
                dialogStage.initOwner(tableSalesOrders.getScene().getWindow());
            }
            dialogStage.setTitle("Chi tiết đơn hàng - " + order.getCode());

            Scene scene = new Scene(root);
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    dialogStage.close();
                }
            });

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setWidth(1140);
            dialogStage.setHeight(660);
            dialogStage.setMinWidth(960);
            dialogStage.setMinHeight(520);
            dialogStage.showAndWait();

        } catch (IOException ex) {
            log.error("Lỗi khi mở popup chi tiết đơn hàng bán lẻ: ", ex);
        }
    }
}
