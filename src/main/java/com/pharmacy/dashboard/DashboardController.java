package com.pharmacy.dashboard;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.dashboard.dto.DashboardStatsDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.layout.MainLayoutController;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label lblTodayRevenue;
    @FXML private Label lblTodayOrders;
    @FXML private Label lblLowStockCount;
    @FXML private Label lblNearExpiryCount;

    @FXML private BarChart<String, Number> chartWeeklySales;
    @FXML private NumberAxis axisSalesAmount;

    @FXML private VBox boxInventoryAlerts;
    @FXML private VBox boxRecentTransactions;

    // Mini Tabs cho Cảnh báo kho (Hạn dùng / Tồn kho)
    @FXML private Button btnAlertTabExpiry;
    @FXML private Button btnAlertTabStock;

    private enum AlertTab { EXPIRY, LOW_STOCK }
    private AlertTab currentAlertTab = AlertTab.EXPIRY;
    private List<InventoryAlertDTO> rawAlerts = new java.util.ArrayList<>();

    private final DashboardService dashboardService = new DashboardServiceImpl();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dashboard-data-loader");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChartAxes();
        loadAllDashboardData();
    }

    private void setupChartAxes() {
        if (axisSalesAmount != null) {
            axisSalesAmount.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axisSalesAmount) {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (val >= 1_000_000) {
                        return String.format("%.0fM", val / 1_000_000);
                    } else if (val >= 1_000) {
                        return String.format("%.0fK", val / 1_000);
                    }
                    return String.format("%.0f", val);
                }
            });
        }
    }

    public void loadAllDashboardData() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                DashboardStatsDTO stats = dashboardService.getStats();
                List<DailySalesDTO> weeklySales = dashboardService.getLast7DaysSales();
                List<InventoryAlertDTO> alerts = dashboardService.getInventoryAlerts(6);
                List<SalesOrderDTO> recentTx = dashboardService.getRecentTransactions(5);

                Platform.runLater(() -> {
                    updateStatsUI(stats);
                    updateChartUI(weeklySales);
                    rawAlerts = alerts != null ? alerts : java.util.Collections.emptyList();
                    renderAlertsUI();
                    updateTransactionsUI(recentTx);
                });

                return null;
            }
        };

        task.setOnFailed(e -> log.error("Lỗi khi tải dữ liệu tổng quan: ", task.getException()));
        executor.submit(task);
    }

    @FXML
    private void handleTabExpiryAlerts(ActionEvent event) {
        currentAlertTab = AlertTab.EXPIRY;
        updateAlertTabStyles();
        renderAlertsUI();
    }

    @FXML
    private void handleTabStockAlerts(ActionEvent event) {
        currentAlertTab = AlertTab.LOW_STOCK;
        updateAlertTabStyles();
        renderAlertsUI();
    }

    private void updateAlertTabStyles() {
        if (btnAlertTabExpiry != null) {
            btnAlertTabExpiry.getStyleClass().remove("pill-tab-active");
            if (currentAlertTab == AlertTab.EXPIRY) btnAlertTabExpiry.getStyleClass().add("pill-tab-active");
        }
        if (btnAlertTabStock != null) {
            btnAlertTabStock.getStyleClass().remove("pill-tab-active");
            if (currentAlertTab == AlertTab.LOW_STOCK) btnAlertTabStock.getStyleClass().add("pill-tab-active");
        }
    }

    private void renderAlertsUI() {
        if (boxInventoryAlerts == null) return;
        boxInventoryAlerts.getChildren().clear();

        List<InventoryAlertDTO> displayList;
        if (currentAlertTab == AlertTab.EXPIRY) {
            displayList = rawAlerts.stream().filter(InventoryAlertDTO::isExpiryAlert).limit(5).toList();
        } else {
            displayList = rawAlerts.stream().filter(a -> !a.isExpiryAlert()).limit(5).toList();
        }

        if (displayList.isEmpty()) {
            String emptyMsg = currentAlertTab == AlertTab.EXPIRY
                    ? "Không có thuốc nào cận hoặc hết hạn sử dụng."
                    : "Không có thuốc nào dưới ngưỡng tồn kho.";
            Label empty = new Label(emptyMsg);
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-padding: 16 0;");
            boxInventoryAlerts.getChildren().add(empty);
            return;
        }

        for (InventoryAlertDTO alert : displayList) {
            HBox row = new HBox(12.0);
            row.getStyleClass().add("alert-item-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(Region.USE_PREF_SIZE);
            row.setPrefHeight(Region.USE_COMPUTED_SIZE);

            // Icon circle
            StackPane iconCircle = new StackPane();
            SVGPath svg = new SVGPath();
            svg.getStyleClass().add("alert-svg");

            if (alert.isExpiryAlert()) {
                iconCircle.getStyleClass().setAll("alert-circle-icon", "alert-circle-danger");
                svg.setContent("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z");
            } else {
                iconCircle.getStyleClass().setAll("alert-circle-icon", "alert-circle-warning");
                svg.setContent("M20 7.5L12 3 4 7.5v9L12 21l8-4.5v-9zM12 5.2l5.6 3.1-5.6 3.2-5.6-3.2L12 5.2zM6 9.8l5 2.8v5.8l-5-2.8V9.8zm7 8.6v-5.8l5-2.8v5.8l-5 2.8z");
            }
            iconCircle.getChildren().add(svg);

            // Titles: Wrap text to ensure full medicine names are always displayed cleanly
            VBox textBox = new VBox(3.0);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            Label title = new Label(alert.getTitle());
            title.getStyleClass().add("alert-title");
            title.setWrapText(true);
            title.setMaxWidth(Double.MAX_VALUE);

            Label subtitle = new Label(alert.getSubtitle());
            subtitle.getStyleClass().add("alert-subtitle");
            subtitle.setWrapText(true);
            subtitle.setMaxWidth(Double.MAX_VALUE);
            textBox.getChildren().addAll(title, subtitle);

            // Badge
            Label badge = new Label(alert.getQuantityBadgeText());
            badge.setMinHeight(Region.USE_PREF_SIZE);
            badge.setMinWidth(Region.USE_PREF_SIZE);
            if (alert.isExpiryAlert()) {
                badge.getStyleClass().setAll("alert-badge-danger");
            } else {
                badge.getStyleClass().setAll("alert-badge-warning");
            }

            row.getChildren().addAll(iconCircle, textBox, badge);
            boxInventoryAlerts.getChildren().add(row);
        }

        // Nút xem chi tiết toàn bộ kho thuốc tương ứng theo tab đang chọn
        String viewMoreText = (currentAlertTab == AlertTab.EXPIRY)
                ? "Xem tất cả thuốc cận & hết hạn trong Kho thuốc →"
                : "Xem tất cả thuốc sắp hết & hết hàng trong Kho thuốc →";
        Button btnViewMore = new Button(viewMoreText);
        btnViewMore.getStyleClass().add("link-action");
        btnViewMore.setOnAction(this::handleNavInventory);
        btnViewMore.setMaxWidth(Double.MAX_VALUE);
        btnViewMore.setAlignment(Pos.CENTER);
        btnViewMore.setStyle("-fx-padding: 8 0 4 0; -fx-font-size: 12px; -fx-text-fill: #0066FF; -fx-background-color: transparent; -fx-cursor: hand;");
        boxInventoryAlerts.getChildren().add(btnViewMore);
    }

    private void updateStatsUI(DashboardStatsDTO stats) {
        if (stats == null) return;

        if (lblTodayRevenue != null) {
            lblTodayRevenue.setText(stats.getFormattedTodayRevenue());
        }

        if (lblTodayOrders != null) {
            lblTodayOrders.setText(String.valueOf(stats.getTodayOrderCount()));
        }

        if (lblLowStockCount != null) {
            lblLowStockCount.setText(String.valueOf(stats.getLowStockCount()));
        }

        if (lblNearExpiryCount != null) {
            lblNearExpiryCount.setText(String.valueOf(stats.getNearExpiryCount()));
        }
    }

    private void updateChartUI(List<DailySalesDTO> weeklySales) {
        if (chartWeeklySales == null || weeklySales == null) return;

        chartWeeklySales.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DailySalesDTO item : weeklySales) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(item.getDayOfWeekLabel(), item.getAmount());
            series.getData().add(data);
        }

        chartWeeklySales.getData().add(series);

        // Gắn Tooltip hiển thị số tiền chính xác khi di chuột vào từng cột
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    double amount = data.getYValue().doubleValue();
                    String tooltipText = String.format("%s: %s", data.getXValue(), FormatUtils.formatCurrency(java.math.BigDecimal.valueOf(amount)));
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setShowDelay(Duration.millis(80));
                    Tooltip.install(data.getNode(), tooltip);
                }
            }
        });
    }

    private void updateTransactionsUI(List<SalesOrderDTO> transactions) {
        if (boxRecentTransactions == null) return;
        boxRecentTransactions.getChildren().clear();

        if (transactions == null || transactions.isEmpty()) {
            Label empty = new Label("Chưa có giao dịch bán hàng nào phát sinh.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-padding: 16 0;");
            boxRecentTransactions.getChildren().add(empty);
            return;
        }

        for (SalesOrderDTO tx : transactions) {
            HBox row = new HBox();
            row.getStyleClass().add("recent-tx-row");
            row.setAlignment(Pos.CENTER_LEFT);

            // 1. Mã HĐ: Căn trái
            Label lblCode = new Label(tx.getCode());
            lblCode.setPrefWidth(160.0);
            lblCode.setAlignment(Pos.CENTER_LEFT);
            lblCode.getStyleClass().add("tx-code");

            // 2. Khách hàng: Căn trái. Nếu không có số điện thoại thì hiển thị "Khách lẻ"
            String phone = tx.getCustomerPhone();
            boolean isGuest = (phone == null || phone.isBlank() || "Khách lẻ".equalsIgnoreCase(phone) || "null".equalsIgnoreCase(phone));
            Label lblCustomer = new Label(isGuest ? "Khách lẻ" : phone);
            lblCustomer.setPrefWidth(170.0);
            lblCustomer.setAlignment(Pos.CENTER_LEFT);
            lblCustomer.getStyleClass().add("tx-customer");
            if (isGuest) {
                lblCustomer.getStyleClass().add("tx-customer-guest");
            }

            // 3. Nhân viên: Initials avatar pill + Tên (Căn trái)
            HBox empBox = new HBox(8.0);
            empBox.setPrefWidth(210.0);
            empBox.setAlignment(Pos.CENTER_LEFT);
            Label lblAvatar = new Label(tx.getEmployeeInitials());
            lblAvatar.getStyleClass().add("avatar-initials");
            Label lblEmpName = new Label(tx.getEmployeeName());
            lblEmpName.getStyleClass().add("employee-name");
            empBox.getChildren().addAll(lblAvatar, lblEmpName);

            // 4. Thời gian: Hiển thị cả Giờ và Ngày (Căn trái)
            String formattedDateTime = tx.getCreatedAt() != null
                    ? tx.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                    : "--:--";
            Label lblTime = new Label(formattedDateTime);
            lblTime.setPrefWidth(170.0);
            lblTime.setAlignment(Pos.CENTER_LEFT);
            lblTime.getStyleClass().add("tx-time");

            // 5. Thanh toán: Khung ô căn trái 140px, phần badge pill ôm sát chữ
            HBox paymentBox = new HBox();
            paymentBox.setPrefWidth(140.0);
            paymentBox.setAlignment(Pos.CENTER_LEFT);
            Label lblPayment = new Label(tx.getFormattedPaymentMethod());
            lblPayment.getStyleClass().setAll("payment-badge", tx.getPaymentBadgeClass());
            paymentBox.getChildren().add(lblPayment);

            // 6. Region grow
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // 7. Tổng tiền: Căn trái theo yêu cầu
            Label lblAmount = new Label(tx.getFormattedTotal());
            lblAmount.setPrefWidth(140.0);
            lblAmount.setAlignment(Pos.CENTER_LEFT);
            lblAmount.getStyleClass().add("tx-amount");

            row.getChildren().addAll(lblCode, lblCustomer, empBox, lblTime, paymentBox, spacer, lblAmount);
            boxRecentTransactions.getChildren().add(row);
        }
    }

    @FXML
    private void handleNavInventory(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            if (currentAlertTab == AlertTab.EXPIRY) {
                MainLayoutController.getInstance().navigateToInventoryWithFilter(null, new String[]{"Có lô cận hạn", "Có lô hết hạn"});
            } else {
                MainLayoutController.getInstance().navigateToInventoryWithFilter(new String[]{"Sắp hết hàng", "Hết hàng"}, null);
            }
        }
    }

    @FXML
    private void handleNavSalesHistory(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToSalesHistory();
        }
    }
}
