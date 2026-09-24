package com.pharmacy.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String ICON_EYE =
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String ICON_EYE_OFF =
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML
    private GridPane rootBox;

    @FXML
    private StackPane heroPane;

    @FXML
    private ImageView heroImageView;

    @FXML
    private VBox formPane;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPasswordVisible;

    @FXML
    private SVGPath iconEye;

    @FXML
    private Label lblError;

    @FXML
    private Button btnLogin;

    @FXML
    public void initialize() {
        if (heroImageView != null && heroPane != null) {
            // Ép ảnh tự co giãn theo kích thước của StackPane khi chạy thật
            heroImageView.fitWidthProperty().bind(heroPane.widthProperty());
            heroImageView.fitHeightProperty().bind(heroPane.heightProperty());

            // Tự động kéo giãn luôn cả khung Clip khi phóng to / thu nhỏ
            if (heroPane.getClip() instanceof Rectangle clipRect) {
                clipRect.widthProperty().bind(heroPane.widthProperty());
                clipRect.heightProperty().bind(heroPane.heightProperty());
            }
        }

        // Đồng bộ nội dung giữa ô mật khẩu (chấm tròn) và ô hiện chữ
        if (txtPassword != null && txtPasswordVisible != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }

        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername != null ? txtUsername.getText().trim() : "";
        String password = txtPassword != null ? txtPassword.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        log.info("Đang kiểm tra đăng nhập cho tài khoản: {}", username);

        // TODO: thay bằng kiểm tra thật với bảng accounts (BCrypt) khi làm AuthService
        if ("admin".equals(username) && "Admin@123".equals(password)) {
            clearError();
            log.info("Đăng nhập thành công với quyền Admin!");
        } else {
            showError("Tên đăng nhập hoặc mật khẩu không chính xác!");
            txtPassword.clear(); // ô hiện chữ tự xóa theo nhờ bind
            txtPassword.requestFocus();
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean show = !txtPasswordVisible.isVisible();

        txtPasswordVisible.setVisible(show);
        txtPasswordVisible.setManaged(show);
        txtPassword.setVisible(!show);
        txtPassword.setManaged(!show);

        iconEye.setContent(show ? ICON_EYE_OFF : ICON_EYE);

        // Giữ con trỏ ở cuối ô đang hiển thị để gõ tiếp
        TextField active = show ? txtPasswordVisible : txtPassword;
        active.requestFocus();
        active.positionCaret(active.getText().length());
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }

    private void clearError() {
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    @FXML
    public void onLoginHover(javafx.scene.input.MouseEvent event) {
        btnLogin.setStyle(
                "-fx-font-family: 'Inter 18pt SemiBold';" +
                        "-fx-background-color: #0052B4;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
    }

    @FXML
    public void onLoginHoverExit(javafx.scene.input.MouseEvent event) {
        btnLogin.setStyle(
                "-fx-font-family: 'Inter 18pt SemiBold';" +
                        "-fx-background-color: #1D70E2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
    }
}