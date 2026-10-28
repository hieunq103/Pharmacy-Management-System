package com.pharmacy.report;

import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
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
    @FXML private TableView<SoldMedicineDTO> tblSoldMedicines;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedIndex;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedCode;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedName;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedCategory;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedUnit;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedQtySold;
    @FXML private TableColumn<SoldMedicineDTO, String> colMedRevenue;
    @FXML private Label lblMedTableTitle;
    @FXML private Button btnViewFull;
    @FXML private javafx.scene.control.ScrollPane reportScrollPane;
    @FXML private javafx.scene.layout.VBox loadingOverlay;

    // Lớp phủ "Đang tải": đếm số tác vụ đang chạy, chỉ hiện nếu tải lâu hơn LOADING_DELAY (tránh nháy)
    private static final javafx.util.Duration LOADING_DELAY = javafx.util.Duration.millis(150);
    private int pendingLoads = 0;
    private final javafx.animation.PauseTransition loadingDelay = new javafx.animation.PauseTransition(LOADING_DELAY);

    // Trạng thái lọc thời gian hiện tại
    private enum PeriodMode { DAY, MONTH }
    private PeriodMode currentMode = PeriodMode.DAY;
    private LocalDate selectedDate = LocalDate.now();
    private int selectedYear = LocalDate.now().getYear();
    private int selectedMonth = LocalDate.now().getMonthValue();

    // Bảng thuốc chỉ hiện top N thuốc bán chạy của kỳ đang chọn
    private final ObservableList<SoldMedicineDTO> topSoldMedicines = FXCollections.observableArrayList();

    // Cờ kiểm soát lazy loading cho tab thuốc đã bán
    private boolean medicinesLoadedForCurrentPeriod = false;

    private final ObservableList<SoldMedicineDTO> masterSoldMedicines = FXCollections.observableArrayList();

    // Top N thuốc bán chạy (theo ngày hoặc theo tháng), kèm nút "Xem đầy đủ" sang Lịch sử bán hàng của kỳ đó
    private static final int TOP_LIMIT = 5;
    private PeriodMode medPeriodMode = PeriodMode.DAY;
    private LocalDate medPeriodDate = LocalDate.now();
    private int medPeriodYear;
    private int medPeriodMonth;

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
                    int index = getIndex() + 1;
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

        tblSoldMedicines.setItems(topSoldMedicines);
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
    }

    private void setupDatePickerFormat(DatePicker datePicker) {
        if (datePicker == null) return;
        FormatUtils.setupDatePicker(datePicker);
        datePicker.setPromptText("dd/MM/yyyy");
    }

    private void initTabSelectionListener() {
        if (tabPaneReport != null) {
            applyScrollForTab(tabPaneReport.getSelectionModel().getSelectedItem());
            tabPaneReport.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                applyScrollForTab(newTab);
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


        loadAllData();
    }

    @FXML
    public void handleSelectMonthMode(ActionEvent event) {
        currentMode = PeriodMode.MONTH;
        syncMonthYearFromInputs();
        updateFilterButtonStyles(false);
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

    /** Theo dõi một tác vụ tải số liệu để bật/tắt lớp phủ "Đang tải báo cáo...". */
    private void trackLoading(Task<?> task) {
        pendingLoads++;
        if (pendingLoads == 1 && loadingOverlay != null && !loadingOverlay.isVisible()) {
            loadingDelay.setOnFinished(e -> {
                if (pendingLoads > 0) {
                    loadingOverlay.setVisible(true);
                    loadingOverlay.setManaged(true);
                }
            });
            loadingDelay.playFromStart();
        }
        task.stateProperty().addListener((obs, oldState, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED
                    || state == javafx.concurrent.Worker.State.FAILED
                    || state == javafx.concurrent.Worker.State.CANCELLED) {
                pendingLoads = Math.max(0, pendingLoads - 1);
                if (pendingLoads == 0) {
                    loadingDelay.stop();
                    if (loadingOverlay != null) {
                        loadingOverlay.setVisible(false);
                        loadingOverlay.setManaged(false);
                    }
                }
            }
        });
    }

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

    /** Cùng phạm vi với Tổng quan / Lịch sử bán hàng: Quản lý xem toàn cửa hàng, Nhân viên chỉ xem của mình. */
    private Integer getCurrentEmployeeId() {
        return UserSession.scopeEmployeeId(null);
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

                    revChartTitle = "Biểu đồ doanh số 7 ngày (tính đến " + targetDate.format(FormatUtils.DATE_FORMATTER) + ")";
                    medChartTitle = "Biểu đồ sản lượng thuốc đã bán 7 ngày (tính đến " + targetDate.format(FormatUtils.DATE_FORMATTER) + ")";
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
        trackLoading(loadTask);
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
                applyMedicineTablePeriod(mode, targetDate, targetYear, targetMonth);

                if (masterSoldMedicines.isEmpty()) {
                    String periodText = (mode == PeriodMode.DAY)
                            ? ("ngày " + targetDate.format(FormatUtils.DATE_FORMATTER))
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
        trackLoading(medTask);
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
     * Bảng chỉ hiện top 5 thuốc bán chạy nhất của ngày/tháng đang chọn (dữ liệu đã sắp theo số lượng bán giảm dần);
     * nút "Xem đầy đủ" mở Lịch sử bán hàng lọc đúng ngày/tháng đó.
     */
    private void applyMedicineTablePeriod(PeriodMode mode, LocalDate date, int year, int month) {
        medPeriodMode = mode;
        medPeriodDate = date;
        medPeriodYear = year;
        medPeriodMonth = month;
        if (lblMedTableTitle != null) {
            lblMedTableTitle.setText(mode == PeriodMode.DAY
                    ? "Top " + TOP_LIMIT + " thuốc bán chạy nhất ngày " + date.format(FormatUtils.DATE_FORMATTER)
                    : "Top " + TOP_LIMIT + " thuốc bán chạy nhất tháng " + month + "/" + year);
        }
        topSoldMedicines.setAll(masterSoldMedicines.subList(0, Math.min(TOP_LIMIT, masterSoldMedicines.size())));
        if (btnViewFull != null) {
            btnViewFull.setDisable(masterSoldMedicines.isEmpty());
        }
    }

    @FXML
    private void handleViewFull(ActionEvent event) {
        com.pharmacy.layout.MainLayoutController layout = com.pharmacy.layout.MainLayoutController.getInstance();
        if (layout == null) return;
        if (medPeriodMode == PeriodMode.DAY) {
            layout.navigateToSalesHistoryForDate(medPeriodDate, layout::navigateToReport);
        } else {
            layout.navigateToSalesHistoryForMonth(medPeriodYear, medPeriodMonth, layout::navigateToReport);
        }
    }

    /**
     * Tab Báo cáo doanh số vừa khít màn hình (biểu đồ co giãn, không cuộn); tab thuốc đã bán cuộn bằng chuột.
     * Thanh cuộn dọc luôn ẩn (FXML) để bề rộng trang không đổi khi chuyển tab -> bộ lọc, thẻ, tab giữ nguyên vị trí.
     */
    private void applyScrollForTab(Tab tab) {
        if (reportScrollPane == null) return;
        boolean revenue = tab == tabRevenue;
        reportScrollPane.setFitToHeight(revenue);
        if (revenue) {
            reportScrollPane.setVvalue(0);
        }
    }
}
