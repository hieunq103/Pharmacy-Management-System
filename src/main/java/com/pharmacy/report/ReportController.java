package com.pharmacy.report;

import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.common.UserSession;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "report-data-loader");
        t.setDaemon(true);
        return t;
    });

    private Task<?> currentChartTask;
    private Task<?> currentMedTask;


    // Filter Controls
    @FXML private Label lblFilterPeriodTitle;
    @FXML private Button btnFilterDay;
    @FXML private DatePicker dpSelectedDate;
    @FXML private Button btnFilterMonth;
    @FXML private TextField txtFilterYear;
    @FXML private ComboBox<String> cbFilterMonth;

    // Stat Cards
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblCashRevenue;
    @FXML private Label lblTransferRevenue;
    @FXML private Label lblTotalMedicinesSold;
    @FXML private Label lblTotalOrders;

    // Main TabPane
    @FXML private TabPane tabPaneReport;
    @FXML private Tab tabRevenue;
    @FXML private Tab tabMedicines;

    // Tab 1: Doanh số & Biểu đồ
    @FXML private Label lblChartTitle;
    @FXML private BarChart<String, Number> barChartRevenue;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    // Tab 2: Số thuốc đã bán & Biểu đồ
    @FXML private Label lblMedChartTitle;
    @FXML private BarChart<String, Number> barChartMedicines;
    @FXML private CategoryAxis chartMedXAxis;
    @FXML private NumberAxis chartMedYAxis;
    @FXML private TextField txtSearchMedicine;
    @FXML private TableView<SoldMedicineDTO> tblSoldMedicines;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedIndex;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedCode;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedName;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedCategory;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedUnit;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedQtySold;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedRevenue;
    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;
    @FXML private Label lblMedTableTitle;
    @FXML private HBox boxMedPagination;
    @FXML private HBox boxViewFullMonth;

    // Trạng thái lọc thời gian hiện tại
    private enum PeriodMode { DAY, MONTH }
    private PeriodMode currentMode = PeriodMode.DAY;
    private LocalDate selectedDate = LocalDate.now();
    private int selectedYear = LocalDate.now().getYear();
    private int selectedMonth = LocalDate.now().getMonthValue();

    // Trạng thái phân trang bảng thuốc
    private int currentPage = 1;
    private static final int PAGE_SIZE = 7;
    private final ObservableList<SoldMedicineDTO> filteredSoldMedicines = FXCollections.observableArrayList();
    private final ObservableList<SoldMedicineDTO> pageSoldMedicines = FXCollections.observableArrayList();

    // Cờ kiểm soát lazy loading cho tab thuốc đã bán
    private boolean medicinesLoadedForCurrentPeriod = false;

    private final ObservableList<SoldMedicineDTO> masterSoldMedicines = FXCollections.observableArrayList();

    // Chế độ Theo tháng: bảng chỉ hiện top N thuốc bán chạy, kèm link "Xem đầy đủ..." sang Lịch sử bán hàng
    private static final int TOP_MONTH_LIMIT = 5;
    private boolean medTopMode = false;
    private int medTopYear;
    private int medTopMonth;

    public ReportController() {
        this.reportService = new ReportServiceImpl();
    }

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private int currentTimelineCount = 7;

    @FXML
    public void initialize() {
        setupChartAxes();
        initTableColumns();
        initFilterControls();
        initSearchListener();
        initTabSelectionListener();

        // Nạp dữ liệu trong lượt render tiếp theo để giao diện hiển thị ngay tức thì mà không giật/xoay
        Platform.runLater(() -> handleSelectDayMode(null));
    }

    private void setupChartAxes() {
        if (chartYAxis != null) {
            chartYAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(chartYAxis) {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (val >= 1_000_000_000) {
                        return String.format("%.1fB", val / 1_000_000_000);
                    } else if (val >= 1_000_000) {
                        return String.format("%.0fM", val / 1_000_000);
                    } else if (val >= 1_000) {
                        return String.format("%.0fK", val / 1_000);
                    }
                    return String.format("%.0f", val);
                }
            });
        }
        if (barChartRevenue != null) {
            barChartRevenue.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != null && newVal != null && Math.abs(newVal.doubleValue() - oldVal.doubleValue()) > 2.0) {
                    adjustSingleChartGap(barChartRevenue);
                }
            });
        }

        if (chartMedYAxis != null) {
            chartMedYAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(chartMedYAxis) {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (val >= 1_000_000) {
                        return String.format("%.1fM", val / 1_000_000);
                    } else if (val >= 1_000) {
                        return String.format("%.1fK", val / 1_000);
                    }
                    return String.format("%.0f", val);
                }
            });
        }
        if (barChartMedicines != null) {
            barChartMedicines.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != null && newVal != null && Math.abs(newVal.doubleValue() - oldVal.doubleValue()) > 2.0) {
                    adjustSingleChartGap(barChartMedicines);
                }
            });
        }
    }

    private void adjustBarChartGap() {
        adjustSingleChartGap(barChartRevenue);
        adjustSingleChartGap(barChartMedicines);
    }

    private void adjustSingleChartGap(BarChart<String, Number> chart) {
        if (chart == null || currentTimelineCount <= 0 || !chart.isVisible()) return;

        // Chiều rộng cột mong muốn tương đương như hiển thị theo tháng (~26px - 30px)
        final double targetBarWidth = 28.0;

        double chartWidth = chart.getWidth();
        if (chartWidth <= 0) return;

        double plotWidth = Math.max(300.0, chartWidth - 80.0);
        double slotWidth = plotWidth / currentTimelineCount;
        double calculatedGap = (slotWidth > targetBarWidth) ? (slotWidth - targetBarWidth) : 4.0;

        // Chỉ cập nhật khi khoảng cách chênh lệch > 1px để triệt tiêu hoàn toàn vòng lặp layout (cyclic relayout)
        if (Math.abs(chart.getCategoryGap() - calculatedGap) > 1.0) {
            chart.setCategoryGap(calculatedGap);
        }
    }

    private void initTableColumns() {
        // Cấu hình bảng Thuốc đã bán
        colMedIndex.setCellFactory(col -> new TableCell<SoldMedicineDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    int index = (currentPage - 1) * PAGE_SIZE + getIndex() + 1;
                    setText(String.valueOf(index));
                }
            }
        });

        colMedCode.setCellValueFactory(new PropertyValueFactory<>("medicineCode"));
        colMedName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colMedCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colMedUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colMedQtySold.setCellValueFactory(new PropertyValueFactory<>("formattedQuantity"));

        colMedRevenue.setCellValueFactory(new PropertyValueFactory<>("formattedRevenue"));
        colMedRevenue.setCellFactory(col -> new TableCell<SoldMedicineDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        colMedRevenue.setSortable(false);

        Label emptyPlaceholder = new Label("Không có dữ liệu thuốc đã bán trong kỳ này");
        emptyPlaceholder.setStyle("-fx-font-family: 'Inter 18pt'; -fx-font-size: 14px; -fx-text-fill: #64748B;");
        tblSoldMedicines.setPlaceholder(emptyPlaceholder);

        tblSoldMedicines.setItems(pageSoldMedicines);
    }


    private void initFilterControls() {
        setupDatePickerFormat(dpSelectedDate);
        if (dpSelectedDate != null) {
            dpSelectedDate.setValue(selectedDate);
        }

        if (txtFilterYear != null) {
            txtFilterYear.setText(String.valueOf(selectedYear));
            txtFilterYear.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) { // Lost focus
                    handleYearAction(null);
                }
            });
        }

        if (cbFilterMonth != null) {
            cbFilterMonth.getItems().clear();
            for (int m = 1; m <= 12; m++) {
                cbFilterMonth.getItems().add("Tháng " + m);
            }
            cbFilterMonth.setValue("Tháng " + selectedMonth);
        }

        updateFilterButtonStyles(true);
        if (lblFilterPeriodTitle != null) {
            lblFilterPeriodTitle.setText("Hôm nay (" + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
    }

    private void setupDatePickerFormat(DatePicker datePicker) {
        if (datePicker == null) return;
        FormatUtils.setupDatePicker(datePicker);
        datePicker.setPromptText("dd/MM/yyyy");
    }

    private void initSearchListener() {
        if (txtSearchMedicine != null) {
            txtSearchMedicine.textProperty().addListener((obs, oldVal, newVal) -> filterSoldMedicines(newVal));
        }
    }

    private void initTabSelectionListener() {
        if (tabPaneReport != null) {
            tabPaneReport.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == tabMedicines) {
                    Platform.runLater(() -> adjustSingleChartGap(barChartMedicines));
                    if (!medicinesLoadedForCurrentPeriod) {
                        loadMedicinesData();
                    }
                } else if (newTab == tabRevenue) {
                    Platform.runLater(() -> adjustSingleChartGap(barChartRevenue));
                }
            });
        }
    }


    // ==================== CÁC NÚT BỘ LỌC THỜI GIAN ====================

    @FXML
    public void handleSelectDayMode(ActionEvent event) {
        currentMode = PeriodMode.DAY;
        updateFilterButtonStyles(true);
        if (lblFilterPeriodTitle != null) {
            if (selectedDate.equals(LocalDate.now())) {
                lblFilterPeriodTitle.setText("Hôm nay (" + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
            } else {
                lblFilterPeriodTitle.setText("Ngày " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        }
        loadAllData();
    }

    @FXML
    public void handleDatePicked(ActionEvent event) {
        LocalDate date = dpSelectedDate != null ? dpSelectedDate.getValue() : null;
        if (date == null) {
            date = LocalDate.now();
            if (dpSelectedDate != null) {
                dpSelectedDate.setValue(date);
            }
        }

        currentMode = PeriodMode.DAY;
        selectedDate = date;
        updateFilterButtonStyles(true);

        if (lblFilterPeriodTitle != null) {
            if (date.equals(LocalDate.now())) {
                lblFilterPeriodTitle.setText("Hôm nay (" + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
            } else {
                lblFilterPeriodTitle.setText("Ngày " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        }

        loadAllData();
    }

    @FXML
    public void handleSelectMonthMode(ActionEvent event) {
        currentMode = PeriodMode.MONTH;
        syncMonthYearFromInputs();
        updateFilterButtonStyles(false);
        if (lblFilterPeriodTitle != null) {
            lblFilterPeriodTitle.setText("Tháng " + selectedMonth + "/" + selectedYear);
        }
        loadAllData();
    }

    @FXML
    public void handleYearAction(ActionEvent event) {
        if (txtFilterYear != null && txtFilterYear.getText() != null) {
            String text = txtFilterYear.getText().trim();
            if (text.isEmpty()) {
                DialogUtils.showWarning("Năm không hợp lệ", "Vui lòng nhập số năm (ví dụ: " + LocalDate.now().getYear() + ").");
                txtFilterYear.setText(String.valueOf(selectedYear));
                return;
            }
            try {
                int y = Integer.parseInt(text);
                if (y >= 2000 && y <= 2100) {
                    boolean yearChanged = (selectedYear != y);
                    boolean modeChanged = (currentMode != PeriodMode.MONTH);
                    selectedYear = y;
                    if (modeChanged) {
                        currentMode = PeriodMode.MONTH;
                        updateFilterButtonStyles(false);
                    }
                    if (lblFilterPeriodTitle != null) {
                        lblFilterPeriodTitle.setText("Tháng " + selectedMonth + "/" + selectedYear);
                    }
                    if (yearChanged || modeChanged || event != null) {
                        loadAllData();
                    }
                } else {
                    DialogUtils.showWarning("Năm không hợp lệ", "Vui lòng nhập từ năm 2000 trở đi.");
                    txtFilterYear.setText(String.valueOf(selectedYear));
                }
            } catch (NumberFormatException e) {
                DialogUtils.showWarning("Năm không hợp lệ", "Vui lòng nhập định dạng năm hợp lệ (ví dụ: " + LocalDate.now().getYear() + ").");
                txtFilterYear.setText(String.valueOf(selectedYear));
            }
        }
    }

    @FXML
    public void handleMonthChanged(ActionEvent event) {
        if (cbFilterMonth != null && cbFilterMonth.getValue() != null) {
            String val = cbFilterMonth.getValue().replace("Tháng", "").trim();
            try {
                selectedMonth = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
            if (currentMode == PeriodMode.MONTH) {
                if (lblFilterPeriodTitle != null) {
                    lblFilterPeriodTitle.setText("Tháng " + selectedMonth + "/" + selectedYear);
                }
                loadAllData();
            }
        }
    }

    private void syncMonthYearFromInputs() {
        if (txtFilterYear != null && txtFilterYear.getText() != null) {
            String text = txtFilterYear.getText().trim();
            try {
                int y = Integer.parseInt(text);
                if (y >= 2000 && y <= 2100) {
                    selectedYear = y;
                } else {
                    DialogUtils.showWarning("Năm không hợp lệ", "Vui lòng nhập từ năm 2000 trở đi.");
                    txtFilterYear.setText(String.valueOf(selectedYear));
                }
            } catch (NumberFormatException e) {
                DialogUtils.showWarning("Năm không hợp lệ", "Vui lòng nhập định dạng năm hợp lệ (ví dụ: " + LocalDate.now().getYear() + ").");
                txtFilterYear.setText(String.valueOf(selectedYear));
            }
        }
        if (cbFilterMonth != null && cbFilterMonth.getValue() != null) {
            String val = cbFilterMonth.getValue().replace("Tháng", "").trim();
            try {
                selectedMonth = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
    }


    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAllData();
    }

    private void updateFilterButtonStyles(boolean isDay) {
        if (btnFilterDay != null) {
            btnFilterDay.getStyleClass().remove("active");
            if (isDay) btnFilterDay.getStyleClass().add("active");
        }
        if (btnFilterMonth != null) {
            btnFilterMonth.getStyleClass().remove("active");
            if (!isDay) btnFilterMonth.getStyleClass().add("active");
        }
    }

    // ==================== LOAD DỮ LIỆU BẤT ĐỒNG BỘ ====================

    /** Tải lại số liệu theo bộ lọc hiện tại (khi quay lại trang Báo cáo đã được nạp sẵn). */
    public void refresh() {
        loadAllData();
    }

    private void loadAllData() {
        medicinesLoadedForCurrentPeriod = false;

        // Ưu tiên 1: Tải nhanh dữ liệu thống kê và cả 2 biểu đồ (Doanh số & Sản lượng thuốc)
        loadChartsAndSummaryData();

        // Ưu tiên 2: Nếu người dùng đang đứng ở Tab Thuốc đã bán thì nạp luôn bảng chi tiết
        if (tabPaneReport != null && tabPaneReport.getSelectionModel().getSelectedItem() == tabMedicines) {
            loadMedicinesData();
        }
    }

    private Integer getCurrentEmployeeId() {
        if (UserSession.isLoggedIn()) {
            return UserSession.getInstance().getEmployeeId();
        }
        return null;
    }

    private void loadChartsAndSummaryData() {
        final PeriodMode mode = currentMode;
        final LocalDate targetDate = selectedDate;
        final int targetYear = selectedYear;
        final int targetMonth = selectedMonth;
        final Integer empId = getCurrentEmployeeId();

        Task<Void> loadTask = new Task<>() {
            ReportSummaryDTO summary;
            List<RevenueTimelineDTO> revTimeline;
            List<MedicineTimelineDTO> medTimeline;
            String revChartTitle;
            String medChartTitle;

            @Override
            protected Void call() {
                if (mode == PeriodMode.DAY) {
                    // Thống kê ngày được chọn của cá nhân
                    summary = reportService.getSummary(targetDate, targetDate, empId);

                    // Biểu đồ vẽ 7 ngày: ngày đó và 6 ngày gần nhất
                    revTimeline = reportService.get7DaysRevenueTimelineUpTo(targetDate, empId);
                    medTimeline = reportService.get7DaysMedicineTimelineUpTo(targetDate, empId);

                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    revChartTitle = "Biểu đồ doanh số 7 ngày (tính đến " + targetDate.format(dtf) + ")";
                    medChartTitle = "Biểu đồ sản lượng thuốc đã bán 7 ngày (tính đến " + targetDate.format(dtf) + ")";
                } else {
                    // Chế độ Tháng: Thống kê cho tháng được chọn trong năm đó
                    LocalDate firstDay = LocalDate.of(targetYear, targetMonth, 1);
                    LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
                    summary = reportService.getSummary(firstDay, lastDay, empId);

                    // Biểu đồ vẽ đủ 12 tháng của năm đã chọn
                    revTimeline = reportService.get12MonthsRevenueTimeline(targetYear, empId);
                    medTimeline = reportService.get12MonthsMedicineTimeline(targetYear, empId);

                    revChartTitle = "Biểu đồ doanh số 12 tháng năm " + targetYear + " (Tháng " + targetMonth + " )";
                    medChartTitle = "Biểu đồ sản lượng thuốc đã bán 12 tháng năm " + targetYear + " (Tháng " + targetMonth + " )";
                }
                return null;
            }

            @Override
            protected void succeeded() {
                updateStatCards(summary);

                boolean hasYearRevenue = revTimeline != null && revTimeline.stream().anyMatch(p -> p.revenue() != null && p.revenue().compareTo(BigDecimal.ZERO) > 0);
                boolean hasMedQty = medTimeline != null && medTimeline.stream().anyMatch(p -> p.quantitySold() > 0);
                boolean hasYearData = hasYearRevenue || hasMedQty;

                if (mode == PeriodMode.MONTH) {
                    if (!hasYearData) {
                        revChartTitle += " (Chưa có dữ liệu)";
                        medChartTitle += " (Chưa có dữ liệu)";
                    }
                } else {
                    boolean hasDayData = (summary != null && summary.totalOrders() > 0);
                    if (!hasDayData && !hasYearRevenue) {
                        revChartTitle += " (Chưa có dữ liệu)";
                        medChartTitle += " (Chưa có dữ liệu)";
                    }
                }

                if (lblChartTitle != null) {
                    lblChartTitle.setText(revChartTitle);
                }
                if (lblMedChartTitle != null) {
                    lblMedChartTitle.setText(medChartTitle);
                }
                updateRevenueChart(revTimeline);
                updateMedicinesChart(medTimeline);
            }

            @Override
            protected void failed() {
                log.error("Lỗi khi tải dữ liệu báo cáo: ", getException());
            }
        };

        if (currentChartTask != null && currentChartTask.isRunning()) {
            currentChartTask.cancel();
        }
        currentChartTask = loadTask;
        executor.submit(loadTask);
    }

    private void loadMedicinesData() {
        final PeriodMode mode = currentMode;
        final LocalDate targetDate = selectedDate;
        final int targetYear = selectedYear;
        final int targetMonth = selectedMonth;
        final Integer empId = getCurrentEmployeeId();

        Task<List<SoldMedicineDTO>> medTask = new Task<>() {
            @Override
            protected List<SoldMedicineDTO> call() {
                if (mode == PeriodMode.DAY) {
                    return reportService.getSoldMedicines(targetDate, targetDate, null, empId);
                } else {
                    LocalDate firstDay = LocalDate.of(targetYear, targetMonth, 1);
                    LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
                    return reportService.getSoldMedicines(firstDay, lastDay, null, empId);
                }
            }

            @Override
            protected void succeeded() {
                medicinesLoadedForCurrentPeriod = true;
                List<SoldMedicineDTO> soldMedicines = getValue();
                masterSoldMedicines.setAll(soldMedicines != null ? soldMedicines : List.of());
                applyMedicineTableMode(mode == PeriodMode.MONTH, targetYear, targetMonth);
                filterSoldMedicines(txtSearchMedicine != null ? txtSearchMedicine.getText() : null);

                if (masterSoldMedicines.isEmpty()) {
                    String periodText = (mode == PeriodMode.DAY)
                            ? ("ngày " + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            : ("Tháng " + targetMonth + "/" + targetYear);
                    Label emptyPlaceholder = new Label("Không có dữ liệu thuốc đã bán trong " + periodText);
                    emptyPlaceholder.setStyle("-fx-font-family: 'Inter 18pt'; -fx-font-size: 14px; -fx-text-fill: #64748B;");
                    tblSoldMedicines.setPlaceholder(emptyPlaceholder);
                }
            }

            @Override
            protected void failed() {
                log.error("Lỗi khi tải danh sách chi tiết thuốc đã bán: ", getException());
            }
        };

        if (currentMedTask != null && currentMedTask.isRunning()) {
            currentMedTask.cancel();
        }
        currentMedTask = medTask;
        executor.submit(medTask);
    }


    private void updateStatCards(ReportSummaryDTO summary) {
        if (summary == null) summary = ReportSummaryDTO.empty();

        BigDecimal rev = summary.totalRevenue() != null ? summary.totalRevenue() : BigDecimal.ZERO;
        long orders = summary.totalOrders();

        lblTotalRevenue.setText(FormatUtils.formatCurrency(rev) + "đ");
        if (lblCashRevenue != null) {
            lblCashRevenue.setText(FormatUtils.formatCurrency(summary.cashRevenue()) + "đ");
        }
        if (lblTransferRevenue != null) {
            lblTransferRevenue.setText(FormatUtils.formatCurrency(summary.transferRevenue()) + "đ");
        }
        lblTotalMedicinesSold.setText(String.format("%,d sản phẩm", summary.totalMedicinesSold()));
        lblTotalOrders.setText(String.format("%,d đơn", orders));
    }

    private void updateRevenueChart(List<RevenueTimelineDTO> timeline) {
        if (barChartRevenue == null) return;
        barChartRevenue.setAnimated(false);
        if (chartXAxis != null) chartXAxis.setAnimated(false);
        barChartRevenue.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh số (VNĐ)");

        final String selectedLabel = (currentMode == PeriodMode.DAY)
                ? selectedDate.format(DateTimeFormatter.ofPattern("dd/MM"))
                : ("Thg " + selectedMonth);

        if (timeline != null && !timeline.isEmpty()) {
            currentTimelineCount = timeline.size();
            for (RevenueTimelineDTO point : timeline) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(point.timeLabel(), point.revenue());
                series.getData().add(data);
                attachTooltipToData(data, resolveTooltipText(point));
            }
        } else {
            currentTimelineCount = 1;
            series.getData().add(new XYChart.Data<>("Không có dữ liệu", 0));
        }

        barChartRevenue.getData().add(series);

        for (XYChart.Data<String, Number> d : series.getData()) {
            boolean isSelected = selectedLabel.equals(d.getXValue());
            applyBarStyling(d, isSelected, true);
            if (d.getNode() != null && d.getExtraValue() instanceof String text) {
                installBarTooltip(d.getNode(), text);
            }
        }

        adjustBarChartGap();
    }

    private void updateMedicinesChart(List<MedicineTimelineDTO> timeline) {
        if (barChartMedicines == null) return;
        barChartMedicines.setAnimated(false);
        if (chartMedXAxis != null) chartMedXAxis.setAnimated(false);
        barChartMedicines.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số thuốc (Sản phẩm)");

        final String selectedLabel = (currentMode == PeriodMode.DAY)
                ? selectedDate.format(DateTimeFormatter.ofPattern("dd/MM"))
                : ("Thg " + selectedMonth);

        if (timeline != null && !timeline.isEmpty()) {
            for (MedicineTimelineDTO point : timeline) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(point.timeLabel(), point.quantitySold());
                series.getData().add(data);
                attachTooltipToData(data, resolveMedTooltipText(point));
            }
        } else {
            series.getData().add(new XYChart.Data<>("Không có dữ liệu", 0));
        }

        barChartMedicines.getData().add(series);

        for (XYChart.Data<String, Number> d : series.getData()) {
            boolean isSelected = selectedLabel.equals(d.getXValue());
            applyBarStyling(d, isSelected, true);
            if (d.getNode() != null && d.getExtraValue() instanceof String text) {
                installBarTooltip(d.getNode(), text);
            }
        }

        adjustBarChartGap();
    }

    private void applyBarStyling(XYChart.Data<String, Number> data, boolean isSelected, boolean isGreenChart) {
        String styleClass = isSelected ? "chart-bar-orange" : (isGreenChart ? "chart-bar-green" : "chart-bar-blue");
        Runnable applyStyle = () -> {
            Node node = data.getNode();
            if (node != null) {
                node.getStyleClass().removeAll("chart-bar-orange", "chart-bar-blue", "chart-bar-green");
                node.getStyleClass().add(styleClass);
            }
        };
        if (data.getNode() != null) {
            applyStyle.run();
        } else {
            data.nodeProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    applyStyle.run();
                }
            });
        }
    }

    private String resolveMedTooltipText(MedicineTimelineDTO point) {
        if (point == null) return "";
        if (point.tooltipText() != null && !point.tooltipText().isBlank()) {
            return point.tooltipText();
        }
        if (currentMode == PeriodMode.MONTH) {
            return "Tháng " + point.timeLabel().replace("Thg ", "").trim() + "/" + selectedYear 
                    + ": " + String.format("%,d", point.quantitySold()) + " sản phẩm";
        } else {
            return "Ngày " + point.timeLabel() + "/" + selectedDate.getYear() 
                    + ": " + String.format("%,d", point.quantitySold()) + " sản phẩm";
        }
    }

    private void attachTooltipToData(XYChart.Data<String, Number> data, String tooltipText) {
        if (data == null || tooltipText == null || tooltipText.isBlank()) return;
        data.setExtraValue(tooltipText);

        if (data.getNode() != null) {
            installBarTooltip(data.getNode(), tooltipText);
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    installBarTooltip(newNode, tooltipText);
                }
            });
        }
    }

    private void installBarTooltip(javafx.scene.Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle("""
            -fx-font-size: 13px;
            -fx-font-family: 'Inter 18pt SemiBold', 'Inter', sans-serif;
            -fx-background-color: #0F172A;
            -fx-text-fill: #FFFFFF;
            -fx-background-radius: 6px;
            -fx-padding: 6px 12px;
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);
        """);
        tooltip.setShowDelay(javafx.util.Duration.millis(50));
        tooltip.setHideDelay(javafx.util.Duration.millis(150));
        Tooltip.install(node, tooltip);
    }

    private String resolveTooltipText(RevenueTimelineDTO point) {
        if (point == null) return "";
        if (point.tooltipText() != null && !point.tooltipText().isBlank()) {
            return point.tooltipText();
        }
        if (currentMode == PeriodMode.MONTH) {
            return "Tháng " + point.timeLabel().replace("Thg ", "").trim() + "/" + selectedYear 
                    + ": " + FormatUtils.formatCurrency(point.revenue()) + " VNĐ";
        } else {
            return "Ngày " + point.timeLabel() + "/" + selectedDate.getYear() 
                    + ": " + FormatUtils.formatCurrency(point.revenue()) + " VNĐ";
        }
    }

    /**
     * Theo ngày: bảng đầy đủ có tìm kiếm + phân trang.
     * Theo tháng: chỉ top 5 thuốc bán chạy nhất (dữ liệu đã sắp theo số lượng bán giảm dần),
     * ẩn tìm kiếm/phân trang và hiện link "Xem đầy đủ..." mở Lịch sử bán hàng của tháng đó.
     */
    private void applyMedicineTableMode(boolean topMode, int year, int month) {
        medTopMode = topMode;
        medTopYear = year;
        medTopMonth = month;
        if (lblMedTableTitle != null) {
            lblMedTableTitle.setText(topMode
                    ? "Top " + TOP_MONTH_LIMIT + " thuốc bán chạy nhất tháng " + month + "/" + year
                    : "Bảng chi tiết sản lượng từng mặt hàng thuốc");
        }
        if (tblSoldMedicines != null) {
            // Top 5: thu gọn bảng vừa đủ 5 dòng thay vì chiều cao của trang 7 dòng
            tblSoldMedicines.setPrefHeight(topMode ? 335 : 425);
        }
        setShown(txtSearchMedicine, !topMode);
        setShown(boxMedPagination, !topMode);
        setShown(boxViewFullMonth, topMode && !masterSoldMedicines.isEmpty());
    }

    private static void setShown(Node node, boolean shown) {
        if (node != null) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }

    @FXML
    private void handleViewFullMonth(ActionEvent event) {
        com.pharmacy.layout.MainLayoutController layout = com.pharmacy.layout.MainLayoutController.getInstance();
        if (layout != null) {
            layout.navigateToSalesHistoryForMonth(medTopYear, medTopMonth);
        }
    }

    private void filterSoldMedicines(String keyword) {
        if (medTopMode) {
            currentPage = 1;
            pageSoldMedicines.setAll(masterSoldMedicines.subList(0, Math.min(TOP_MONTH_LIMIT, masterSoldMedicines.size())));
            return;
        }
        if (keyword == null || keyword.isBlank()) {
            filteredSoldMedicines.setAll(masterSoldMedicines);
        } else {
            String lower = keyword.trim().toLowerCase();
            List<SoldMedicineDTO> matches = masterSoldMedicines.stream()
                    .filter(med ->
                            (med.getMedicineCode() != null && med.getMedicineCode().toLowerCase().contains(lower)) ||
                            (med.getMedicineName() != null && med.getMedicineName().toLowerCase().contains(lower)) ||
                            (med.getCategoryName() != null && med.getCategoryName().toLowerCase().contains(lower))
                    )
                    .toList();
            filteredSoldMedicines.setAll(matches);
        }
        currentPage = 1;
        renderPagination();
    }

    private void renderPagination() {
        int totalRecords = filteredSoldMedicines.size();
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages <= 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalRecords);

        if (fromIndex < totalRecords) {
            pageSoldMedicines.setAll(filteredSoldMedicines.subList(fromIndex, toIndex));
        } else {
            pageSoldMedicines.clear();
        }

        PaginationHelper.render(
                paginationBox,
                lblRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                PAGE_SIZE,
                this::goToPage
        );
    }

    private void goToPage(int page) {
        this.currentPage = page;
        renderPagination();
    }
}
