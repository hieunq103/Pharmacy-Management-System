package com.pharmacy.layout;

import com.pharmacy.account.Account;
import com.pharmacy.auth.AuthService;
import com.pharmacy.common.BackgroundTasks;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UiScaleManager;
import com.pharmacy.common.UserSession;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
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

public class MainLayoutController {

    private static final Logger log = LoggerFactory.getLogger(MainLayoutController.class);
    private static MainLayoutController instance;
    private final AuthService authService;

    public static MainLayoutController getInstance() {
        return instance;
    }

    public MainLayoutController() {
        this.authService = new AuthService();
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

    private String headerDateText;
    private javafx.animation.Timeline headerClock;

    private static final String REPORT_FXML = "/fxml/report/report.fxml";
    private final java.util.Map<String, Parent> viewCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Object> controllerCache = new java.util.concurrent.ConcurrentHashMap<>();
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
        Platform.runLater(this::preloadViews);
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

        String roleName =
            currentAccount.getRole() != null && currentAccount.getRole().getName() != null
                ? currentAccount.getRole().getName()
                : "Chưa phân quyền";
        lblUserRole.setText(roleName);

        boolean isManager = session.isManager();
        applyNodeVisibility(btnDashboard, isManager);
        applyNodeVisibility(boxInventoryNav != null ? boxInventoryNav : btnInventory, isManager);
        applyNodeVisibility(btnEmployee, isManager);
    }

    // Thứ, ngày tháng năm và giờ (chạy từng giây) nằm ở bên phải thanh tiêu đề
    private void initHeaderDate() {
        updateHeaderClock();
        headerClock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateHeaderClock())
        );
        headerClock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        headerClock.play();
    }

    private void updateHeaderClock() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String dateText = now
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M, yyyy", Locale.of("vi", "VN")));
        if (!dateText.equals(headerDateText)) {
            headerDateText = Character.toUpperCase(dateText.charAt(0)) + dateText.substring(1);
        }
        lblHeaderSubtitle.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  |  " + headerDateText);
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

    /**
     * Xóa các "ngữ cảnh điều hướng tạm" (nút Quay lại + bộ lọc áp từ Tổng quan, lịch sử bán mở từ Tổng quan...)
     * khi người dùng chuyển trang bằng thanh Menu, để các trang trở về trạng thái ban đầu.
     */
    private void resetContexts() {
        if (controllerCache.get("/fxml/sales/sales.fxml") instanceof com.pharmacy.sales.SalesController sales) {
            sales.resetContext();
        }
        if (
            controllerCache.get("/fxml/inventory/inventory.fxml") instanceof
                com.pharmacy.inventory.InventoryController inventory
        ) {
            inventory.resetContext();
        }
        if (
            controllerCache.get("/fxml/profile/profile.fxml") instanceof com.pharmacy.profile.ProfileController profile
        ) {
            profile.setOnBack(null);
        }
    }

    @FXML
    private void handleNavDashboard(ActionEvent event) {
        resetContexts();
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập màn hình Tổng quan! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return;
        }
        setActiveButton(btnDashboard);
        lblHeaderTitle.setText("Tổng quan");
        Parent cached = viewCache.get("/fxml/dashboard/dashboard.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            if (
                controllerCache.get("/fxml/dashboard/dashboard.fxml") instanceof
                    com.pharmacy.dashboard.DashboardController dashCtrl
            ) {
                dashCtrl.loadAllDashboardData();
            }
            return;
        }
        loadCenterView("/fxml/dashboard/dashboard.fxml");
    }

    @FXML
    private void handleNavSales(ActionEvent event) {
        resetContexts();
        setActiveButton(btnSales);
        lblHeaderTitle.setText("Bán hàng");
        Parent cached = viewCache.get("/fxml/sales/sales.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            return;
        }
        loadCenterView("/fxml/sales/sales.fxml");
    }

    @FXML
    private void handleNavInventory(ActionEvent event) {
        navigateToInventory();
    }

    public void navigateToInventory() {
        resetContexts();
        com.pharmacy.inventory.InventoryController ctrl = showInventoryPage();
        if (ctrl != null) {
            ctrl.resetToDefault();
        }
    }

    /** Mở Kho thuốc với bộ lọc có sẵn; {@code onBack} hiện thành nút Quay lại và khôi phục bộ lọc cũ khi thoát. */
    public void navigateToInventoryWithFilter(String[] stockFilters, String[] expiryFilters, Runnable onBack) {
        com.pharmacy.inventory.InventoryController ctrl = showInventoryPage();
        if (ctrl != null) {
            ctrl.enterFilteredContext(stockFilters, expiryFilters, onBack);
        }
    }

    private com.pharmacy.inventory.InventoryController showInventoryPage() {
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập màn hình Kho thuốc! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return null;
        }
        setActiveButton(btnInventory);
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Kho thuốc");
        }
        Parent cached = viewCache.get("/fxml/inventory/inventory.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            return (com.pharmacy.inventory.InventoryController) controllerCache.get("/fxml/inventory/inventory.fxml");
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/inventory/inventory.fxml");
        return loader != null && loader.getController() instanceof com.pharmacy.inventory.InventoryController ctrl
            ? ctrl
            : (com.pharmacy.inventory.InventoryController) controllerCache.get("/fxml/inventory/inventory.fxml");
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
        Parent cached = viewCache.get("/fxml/sales/sales.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            return (com.pharmacy.sales.SalesController) controllerCache.get("/fxml/sales/sales.fxml");
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/sales/sales.fxml");
        return loader != null && loader.getController() instanceof com.pharmacy.sales.SalesController salesCtrl
            ? salesCtrl
            : (com.pharmacy.sales.SalesController) controllerCache.get("/fxml/sales/sales.fxml");
    }

    public void navigateToProfileFromEmployees() {
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Thông tin cá nhân");
        }
        Parent cached = viewCache.get("/fxml/profile/profile.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            if (
                controllerCache.get("/fxml/profile/profile.fxml") instanceof
                    com.pharmacy.profile.ProfileController profileCtrl
            ) {
                profileCtrl.setOnBack(() -> showEmployeePage(false));
            }
            return;
        }
        FXMLLoader loader = loadCenterViewWithLoader("/fxml/profile/profile.fxml");
        if (loader != null && loader.getController() instanceof com.pharmacy.profile.ProfileController profileCtrl) {
            profileCtrl.setOnBack(() -> showEmployeePage(false));
        }
    }

    @FXML
    private void handleNavProfile(javafx.scene.input.MouseEvent event) {
        resetContexts();
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Thông tin cá nhân");
        }
        setActiveButton(null);
        Parent cached = viewCache.get("/fxml/profile/profile.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            return;
        }
        loadCenterView("/fxml/profile/profile.fxml");
    }

    @FXML
    private void handleNavEmployee(ActionEvent event) {
        showEmployeePage(true);
    }

    /** {@code reset}: true khi vào bằng Menu (trang về trạng thái ban đầu); false khi quay lại từ Hồ sơ cá nhân. */
    private void showEmployeePage(boolean reset) {
        resetContexts();
        if (UserSession.isLoggedIn() && !UserSession.getInstance().isManager()) {
            log.warn("Nhân viên không có quyền truy cập quản lý Nhân viên! Điều hướng về Bán hàng.");
            handleNavSales(null);
            return;
        }
        setActiveButton(btnEmployee);
        lblHeaderTitle.setText("Quản lý nhân viên");
        Parent cached = viewCache.get("/fxml/employee/employee.fxml");
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            if (
                reset &&
                controllerCache.get("/fxml/employee/employee.fxml") instanceof
                    com.pharmacy.employee.EmployeeController ctrl
            ) {
                ctrl.resetState();
            }
            return;
        }
        loadCenterView("/fxml/employee/employee.fxml");
    }

    @FXML
    private void handleNavReport(ActionEvent event) {
        resetContexts();
        setActiveButton(btnReport);
        lblHeaderTitle.setText("Báo cáo & Thống kê");
        Parent cached = viewCache.get(REPORT_FXML);
        if (cached == null) {
            reportPendingShow = true;
            contentArea.getChildren().setAll(createLoadingPlaceholder("Đang tải báo cáo..."));
            preloadViews();
            return;
        }
        reportPendingShow = false;
        contentArea.getChildren().setAll(cached);
        if (controllerCache.get(REPORT_FXML) instanceof com.pharmacy.report.ReportController rc) {
            rc.refresh();
        }
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

    private void preloadViews() {
        String[] viewsToPreload = {
            "/fxml/dashboard/dashboard.fxml",
            "/fxml/sales/sales.fxml",
            "/fxml/inventory/inventory.fxml",
            "/fxml/employee/employee.fxml",
            REPORT_FXML,
        };
        for (String fxmlPath : viewsToPreload) {
            if (viewCache.containsKey(fxmlPath)) continue;
            BackgroundTasks.run(() -> {
                try {
                    var resource = getClass().getResource(fxmlPath);
                    if (resource != null) {
                        FXMLLoader loader = new FXMLLoader(resource);
                        Parent view = loader.load();
                        Object controller = loader.getController();
                        Platform.runLater(() -> {
                            if (!viewCache.containsKey(fxmlPath)) {
                                viewCache.put(fxmlPath, view);
                                if (controller != null) {
                                    controllerCache.put(fxmlPath, controller);
                                }
                            }
                            if (REPORT_FXML.equals(fxmlPath) && reportPendingShow) {
                                reportPendingShow = false;
                                contentArea.getChildren().setAll(view);
                                if (controller instanceof com.pharmacy.report.ReportController rc) {
                                    rc.refresh();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    log.debug("Preload view {} chưa sẵn sàng: {}", fxmlPath, e.getMessage());
                }
            });
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (headerClock != null) headerClock.stop();
        if (!UserSession.isLoggedIn()) {
            log.warn("Thao tác đăng xuất bị từ chối: Không tìm thấy phiên hợp lệ.");
            forceRedirectToLogin();
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtils.configure(
            confirmDialog,
            rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null
        );
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
            showFriendlyMessage(
                "Thông báo",
                "Có lỗi xảy ra trong quá trình đăng xuất. Hệ thống sẽ đưa bạn về trang đăng nhập."
            );
            forceRedirectToLogin();
        }
    }

    private void cleanupDashboardResources() {
        try {
            viewCache.clear();
            controllerCache.clear();
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
        DialogUtils.showInfo(
            rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null,
            title,
            message
        );
    }

    private FXMLLoader loadCenterViewWithLoader(String fxmlPath) {
        Parent cached = viewCache.get(fxmlPath);
        if (cached != null) {
            contentArea.getChildren().setAll(cached);
            return null;
        }
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
            viewCache.put(fxmlPath, view);
            if (loader.getController() != null) {
                controllerCache.put(fxmlPath, loader.getController());
            }
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
            if (btn == null) continue;
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
