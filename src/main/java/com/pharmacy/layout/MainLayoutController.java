package com.pharmacy.layout;

import com.pharmacy.account.Account;
import com.pharmacy.auth.AuthService;
import com.pharmacy.auth.AuthServiceImpl;
import com.pharmacy.common.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private final AuthService authService;

    public MainLayoutController() {
        this.authService = new AuthServiceImpl();
    }

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;

    // Menu Buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnSales;
    @FXML private Button btnInventory;
    @FXML private Label lblInventoryBadge;
    @FXML private Button btnEmployee;
    @FXML private Button btnReport;

    // Phân quyền cho nhóm Hệ thống
    @FXML private VBox boxSystemSection;
    @FXML private Button btnAccountPermission;

    // Thông tin người dùng
    @FXML private Label lblUserAvatar;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Button btnLogout;

    // Header
    @FXML private Label lblHeaderTitle;
    @FXML private Label lblHeaderSubtitle;
    @FXML private TextField txtGlobalSearch;

    @FXML
    public void initialize() {
        initUserInfoAndPermissions();
        initHeaderDate();

        // Nạp màn hình Dashboard mặc định khi vừa vào
        handleNavDashboard(null);
    }

    private void initUserInfoAndPermissions() {
        if (!UserSession.isLoggedIn()) {
            log.warn("Không tìm thấy UserSession! Quay lại màn hình đăng nhập.");
            handleLogout(null);
            return;
        }

        UserSession session = UserSession.getInstance();
        Account currentAccount = session.getAccount();

        // 1. Hiển thị thông tin người dùng từ UserSession (đã loại bỏ getUsername)
        String fullName = session.getEmployeeName();
        lblUserName.setText(fullName);
        lblUserAvatar.setText(getInitials(fullName));

        String roleName = (currentAccount.getRole() != null && currentAccount.getRole().getName() != null)
                ? currentAccount.getRole().getName()
                : "Chưa phân quyền";
        lblUserRole.setText(roleName);

        // 2. Phân quyền: Nếu không phải Quản lý (MANAGER) thì ẩn menu Quản lý Nhân viên và mục Hệ thống
        boolean isManager = session.isManager();
        applyNodeVisibility(btnEmployee, isManager);
        applyNodeVisibility(boxSystemSection, isManager);
    }

    private void initHeaderDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'tháng' M, yyyy", new Locale("vi", "VN"));
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
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    // ==================== ĐIỀU HƯỚNG TỪNG TAB ====================

    @FXML
    private void handleNavDashboard(ActionEvent event) {
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
        setActiveButton(btnInventory);
        lblHeaderTitle.setText("Kho thuốc");
        loadCenterView("/fxml/inventory/inventory.fxml");
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
        setActiveButton(btnEmployee);
        lblHeaderTitle.setText("Quản lý nhân viên");
        loadCenterView("/fxml/employee/employee.fxml");
    }

    @FXML
    private void handleNavReport(ActionEvent event) {
        setActiveButton(btnReport);
        lblHeaderTitle.setText("Báo cáo & Thống kê");
        loadCenterView("/fxml/report/report.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (!UserSession.isLoggedIn()) {
            log.warn("Thao tác đăng xuất bị từ chối: Không tìm thấy phiên hợp lệ.");
            forceRedirectToLogin();
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            cleanupDashboardResources();

            // Invalidate session hoàn toàn
            authService.logout();

            // Điều hướng an toàn về màn hình đăng nhập
            forceRedirectToLogin();

        } catch (Exception e) {
            log.error("Lỗi phát sinh trong quá trình đăng xuất: ", e);
            showFriendlyMessage("Thông báo", "Có lỗi xảy ra trong quá trình đăng xuất. Hệ thống sẽ đưa bạn về trang đăng nhập.");
            forceRedirectToLogin();
        }
    }

    /**
     * Dọn dẹp tài nguyên layout, giải phóng luồng và bộ nhớ trước khi hủy giao diện.
     */
    private void cleanupDashboardResources() {
        try {
            if (contentArea != null) {
                contentArea.getChildren().clear();
            }
        } catch (Exception ex) {
            log.warn("Lỗi khi giải phóng tài nguyên layout: {}", ex.getMessage());
        }
    }

    private void forceRedirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1160, 720);

            var css = getClass().getResource("/css/app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setMaximized(false);
            stage.setScene(scene);
            stage.setTitle("PharmaOS — Quản lý cửa hàng thuốc");
            stage.centerOnScreen();
        } catch (IOException e) {
            log.error("Không thể nạp giao diện đăng nhập: ", e);
            showFriendlyMessage("Lỗi giao diện", "Không thể tải màn hình đăng nhập. Vui lòng khởi động lại ứng dụng.");
        }
    }

    private void showFriendlyMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadCenterView(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                Label placeholder = new Label("Giao diện đang được phát triển: " + fxmlPath);
                placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748B;");
                contentArea.getChildren().setAll(placeholder);
                return;
            }
            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Không thể tải view: {}", fxmlPath, e);
        }
    }

    private void setActiveButton(Button activeButton) {
        Button[] allButtons = {btnDashboard, btnSales, btnInventory, btnEmployee, btnReport, btnAccountPermission};
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