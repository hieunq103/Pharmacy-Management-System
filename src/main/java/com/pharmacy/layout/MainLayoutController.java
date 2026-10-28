package com.pharmacy.layout;

import com.pharmacy.account.Account;
import com.pharmacy.auth.AuthService;
import com.pharmacy.auth.AuthServiceImpl;
import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UiScaleManager;
import com.pharmacy.common.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class MainLayoutController {
    private static final Logger log = LoggerFactory.getLogger(MainLayoutController.class);
    private static MainLayoutController instance;
    private final AuthService authService;

    public static MainLayoutController getInstance() {
        return instance;
    }

    public MainLayoutController() {
        this.authService = new AuthServiceImpl();
    }

    @FXML
    private BorderPane rootPane;
    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnSales;
    @FXML
    private HBox boxInventoryNav;
    @FXML
    private Button btnInventory;
    @FXML
    private Button btnEmployee;
    @FXML
    private Button btnReport;

    @FXML
    private Label lblUserAvatar;
    @FXML
    private Label lblUserName;
    @FXML
    private Label lblUserRole;

    @FXML
    private Label lblHeaderTitle;
    @FXML
    private Label lblHeaderSubtitle;

    private static final String REPORT_FXML = "/fxml/report/report.fxml";
    private Parent reportView;
    private com.pharmacy.report.ReportController reportController;
    private boolean reportPreloading = false;
    private boolean reportPendingShow = false;

    @FXML
    public void initialize() {
        instance = this;
        initUserInfoAndPermissions();
        initHeaderDate();
        initUiScaling();

        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            handleNavSales(null);
        } else {
            handleNavDashboard(null);
        }
        Platform.runLater(this::preloadReportView);
    }

    private void initUserInfoAndPermissions() {
        if (!UserSession.isLoggedIn()) {
            log.warn("Không tìm thấy UserSession! Quay lại màn hình đăng nhập.");
            handleLogout(null);
            return;
        }

        UserSession session = UserSession.getInstance();
        Account currentAccount = session.getAccount();

        String fullName = session.getEmployeeName();
        lblUserName.setText(fullName);
        lblUserAvatar.setText(getInitials(fullName));

        String roleName = (currentAccount.getRole() != null && currentAccount.getRole().getName() != null)
                ? currentAccount.getRole().getName()
                : "Chưa phân quyền";
        lblUserRole.setText(roleName);

        boolean isManager = session.isManager();
        applyNodeVisibility(btnDashboard, isManager);
        applyNodeVisibility(boxInventoryNav != null ? boxInventoryNav : btnInventory, isManager);
        applyNodeVisibility(btnEmployee, isManager);
    }

    private void initHeaderDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'tháng' M, yyyy", Locale.of("vi", "VN"));
        String dateStr = today.format(formatter);
        dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        lblHeaderSubtitle.setText(dateStr);
    }

    private void applyNodeVisibility(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String getInitials(String fullName) {
        return FormatUtils.getInitials(fullName);
    }

    @FXML
    private void handleNavDashboard(ActionEvent event) {
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập màn hình Tổng quan! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return;
        }
        setActiveButton(btnDashboard);
        lblHeaderTitle.setText("Tổng quan");
        loadCenterView("/fxml/dashboard/dashboard.fxml");
    }

    @FXML
    private void handleNavSales(ActionEvent event) {
        setActiveButton(btnSales);
        lblHeaderTitle.setText("Bán hàng");
        loadCenterView("/fxml/sales/sales.fxml");
    }

    @FXML
    private void handleNavInventory(ActionEvent event) {
        navigateToInventory();
    }

    public void navigateToInventory() {
        navigateToInventoryWithFilter(null, null);
    }

    public void navigateToInventoryWithFilter(String[] stockFilters, String[] expiryFilters) {
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập màn hình Kho thuốc! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return;
        }
        setActiveButton(btnInventory);
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Kho thuốc");
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/inventory/inventory.fxml");
        if (loader != null && loader.getController() instanceof com.pharmacy.inventory.InventoryController invCtrl) {
            invCtrl.applyFilters(stockFilters, expiryFilters);
        }
    }

    public void navigateToSalesHistoryForMonth(int year, int month, Runnable onBack) {
        com.pharmacy.sales.SalesController salesCtrl = showSalesPage();
        if (salesCtrl != null) {
            salesCtrl.openSalesHistoryForMonth(year, month, onBack);
        }
    }

    public void navigateToSalesHistoryForDate(java.time.LocalDate date, Runnable onBack) {
        com.pharmacy.sales.SalesController salesCtrl = showSalesPage();
        if (salesCtrl != null) {
            salesCtrl.openSalesHistoryForDate(date, onBack);
        }
    }

    public void navigateToDashboard() {
        handleNavDashboard(null);
    }

    public void navigateToReport() {
        handleNavReport(null);
    }

    private com.pharmacy.sales.SalesController showSalesPage() {
        setActiveButton(btnSales);
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Bán hàng");
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/sales/sales.fxml");
        return loader != null && loader.getController() instanceof com.pharmacy.sales.SalesController salesCtrl
                ? salesCtrl
                : null;
    }

    public void navigateToProfileFromEmployees() {
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Thông tin cá nhân");
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/profile/profile.fxml");
        if (loader != null && loader.getController() instanceof com.pharmacy.profile.ProfileController profileCtrl) {
            profileCtrl.setOnBack(() -> handleNavEmployee(null));
        }
    }

    @FXML
    private void handleNavProfile(javafx.scene.input.MouseEvent event) {
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Thông tin cá nhân");
        }
        setActiveButton(null);
        loadCenterView("/fxml/profile/profile.fxml");
    }

    @FXML
    private void handleNavEmployee(ActionEvent event) {
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập quản lý Nhân viên! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return;
        }
        setActiveButton(btnEmployee);
        lblHeaderTitle.setText("Quản lý nhân viên");
        loadCenterView("/fxml/employee/employee.fxml");
    }

    @FXML
    private void handleNavReport(ActionEvent event) {
        setActiveButton(btnReport);
        lblHeaderTitle.setText("Báo cáo & Thống kê");
        if (reportView == null) {
            reportPendingShow = true;
            contentArea.getChildren().setAll(createLoadingPlaceholder("Đang tải báo cáo..."));
            preloadReportView();
            return;
        }
        reportPendingShow = false;
        contentArea.getChildren().setAll(reportView);
        reportController.refresh();
    }

    private javafx.scene.Node createLoadingPlaceholder(String text) {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(54, 54);
        spinner.setStyle("-fx-progress-color: #0066FF;");
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 14px; -fx-text-fill: #475569;");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, spinner, label);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    private void preloadReportView() {
        if (reportPreloading || reportView != null)
            return;
        reportPreloading = true;
        BackgroundTasks.run(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(REPORT_FXML));
                Parent view = loader.load();
                Platform.runLater(() -> {
                    reportPreloading = false;
                    if (reportView == null) {
                        reportView = view;
                        reportController = loader.getController();
                    }

                    if (reportPendingShow) {
                        reportPendingShow = false;
                        contentArea.getChildren().setAll(reportView);
                    }
                });
            } catch (Exception e) {
                log.error("Không nạp được trang Báo cáo: ", e);
                Platform.runLater(() -> {
                    reportPreloading = false;
                    if (reportPendingShow) {
                        reportPendingShow = false;
                        Label error = new Label("Không tải được trang Báo cáo: " + e.getMessage());
                        error.setStyle("-fx-font-size: 14px; -fx-text-fill: #DC2626;");
                        contentArea.getChildren().setAll(error);
                    }
                });
            }
        });
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (!UserSession.isLoggedIn()) {
            log.warn("Thao tác đăng xuất bị từ chối: Không tìm thấy phiên hợp lệ.");
            forceRedirectToLogin();
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtils.configure(confirmDialog,
                rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
        confirmDialog.setTitle("Xác nhận");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            cleanupDashboardResources();

            authService.logout();

            forceRedirectToLogin();
        } catch (Exception e) {
            log.error("Lỗi phát sinh trong quá trình đăng xuất: ", e);
            showFriendlyMessage("Thông báo",
                    "Có lỗi xảy ra trong quá trình đăng xuất. Hệ thống sẽ đưa bạn về trang đăng nhập.");
            forceRedirectToLogin();
        }
    }

    private void cleanupDashboardResources() {
        try {
            if (contentArea != null) {
                contentArea.getChildren().clear();
            }
        } catch (Exception ex) {
            log.warn("Lỗi khi giải phóng tài nguyên layout: {}", ex.getMessage());
        }
    }

    private void initUiScaling() {
        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                UiScaleManager.getInstance().install(rootPane.getScene());
            }
        });
    }

    private void forceRedirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);

            var css = getClass().getResource("/css/app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setMaximized(false);
            stage.setResizable(true);
            stage.setWidth(1280);
            stage.setHeight(800);
            stage.setMinWidth(1024);
            stage.setMinHeight(580);
            stage.setScene(scene);
            stage.setTitle("PharmaOS — Quản lý cửa hàng thuốc");
            stage.centerOnScreen();
            UiScaleManager.getInstance().install(scene);
        } catch (IOException e) {
            log.error("Không thể nạp giao diện đăng nhập: ", e);
            showFriendlyMessage("Lỗi giao diện", "Không thể tải màn hình đăng nhập. Vui lòng khởi động lại ứng dụng.");
        }
    }

    private void showFriendlyMessage(String title, String message) {
        DialogUtils.showInfo(rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null,
                title, message);
    }

    private FXMLLoader loadCenterViewWithLoader(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                Label placeholder = new Label("Giao diện đang được phát triển: " + fxmlPath);
                placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748B;");
                contentArea.getChildren().setAll(placeholder);
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            return loader;
        } catch (IOException e) {
            log.error("Không thể tải view: {}", fxmlPath, e);
            return null;
        }
    }

    private void loadCenterView(String fxmlPath) {
        loadCenterViewWithLoader(fxmlPath);
    }

    private void setActiveButton(Button activeButton) {
        if (activeButton != btnReport) {
            reportPendingShow = false;
        }
        Button[] allButtons = { btnDashboard, btnSales, btnInventory, btnEmployee, btnReport };
        for (Button btn : allButtons) {
            if (btn == null)
                continue;
            if (btn == activeButton) {
                if (!btn.getStyleClass().contains("active")) {
                    btn.getStyleClass().add("active");
                }
            } else {
                btn.getStyleClass().remove("active");
            }
        }
    }
}
