package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.TogglePasswordField;
import com.pharmacy.common.UserSession;
import com.pharmacy.infra.exception.AppException;
import com.pharmacy.infra.exception.ErrorCode;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML private GridPane rootBox;
    @FXML private StackPane heroPane;
    @FXML private ImageView heroImageView;
    @FXML private VBox formPane;

    @FXML private TextField txtUsername;
    @FXML private TogglePasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    private final AuthService authService = new AuthServiceImpl();
    private boolean isInternalUpdating = false;

    @FXML
    public void initialize() {
        if (heroImageView != null && heroPane != null) {
            heroImageView.fitWidthProperty().bind(heroPane.widthProperty());
            heroImageView.fitHeightProperty().bind(heroPane.heightProperty());

            if (heroPane.getClip() instanceof Rectangle clipRect) {
                clipRect.widthProperty().bind(heroPane.widthProperty());
                clipRect.heightProperty().bind(heroPane.heightProperty());
            }
        }

        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
            lblError.setWrapText(true);
        }

        // Tự động xóa lỗi khi người dùng thay đổi dữ liệu nhập
        if (txtUsername != null) {
            txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isInternalUpdating) {
                    clearError();
                }
            });
        }
        if (txtPassword != null) {
            txtPassword.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isInternalUpdating) {
                    clearError();
                }
            });
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String identifier = txtUsername != null ? txtUsername.getText().trim() : "";
        String password = txtPassword != null ? txtPassword.getText() : "";

        // Validate form rỗng và định dạng SĐT / CCCD
        FormValidator.ValidationResult validationResult = FormValidator.validateLoginForm(identifier, password);
        if (!validationResult.isValid()) {
            showError(validationResult.errorCode());
            if (validationResult.targetField() == FormValidator.TargetField.IDENTIFIER
                    || validationResult.targetField() == FormValidator.TargetField.BOTH) {
                if (txtUsername != null) txtUsername.requestFocus();
            } else if (validationResult.targetField() == FormValidator.TargetField.PASSWORD) {
                if (txtPassword != null) txtPassword.requestFocus();
            }
            return;
        }

        clearError();
        setLoading(true);
        log.debug("Bắt đầu xác thực cho SĐT/CCCD: {}", identifier);

        Task<Account> loginTask = new Task<>() {
            @Override
            protected Account call() {
                return authService.login(new LoginRequest(identifier, password));
            }
        };

        loginTask.setOnSucceeded(e -> {
            setLoading(false);
            Account account = loginTask.getValue();
            String employeeName = UserSession.getInstance().getEmployeeName();

            log.info("Xác thực thành công cho nhân viên [{}]", employeeName);

            try {
                if (account.isMustChangePassword()) {
                    log.info("Tài khoản nhân viên [{}] yêu cầu đổi mật khẩu lần đầu", employeeName);
                }
                navigateToMainLayout();
            } catch (IOException ioEx) {
                log.error("Không thể tải màn hình chính: ", ioEx);
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Lỗi giao diện");
                alert.setHeaderText("Không thể nạp màn hình chính");
                alert.setContentText(ioEx.getMessage());
                alert.showAndWait();
            }
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = loginTask.getException();
            log.warn("Đăng nhập thất bại cho [{}]: {}", identifier, ex.getMessage());

            if (ex instanceof AppException appEx) {
                showError(appEx.getMessage());
            } else {
                log.error("Lỗi hạ tầng hoặc cơ sở dữ liệu: ", ex);
                showError(ErrorCode.DATABASE_CONNECTION_ERROR);
            }

            // Xóa ô nhập mật khẩu dưới cờ chặn để listener không xóa thông báo lỗi vừa hiển thị
            isInternalUpdating = true;
            try {
                if (txtPassword != null) {
                    txtPassword.clear();
                }
            } finally {
                isInternalUpdating = false;
            }

            if (txtPassword != null) {
                txtPassword.requestFocus();
            }
        });

        EXECUTOR.submit(loginTask);
    }

    private void navigateToMainLayout() throws IOException {
        URL mainLayoutFxml = getClass().getResource("/fxml/layout/main_layout.fxml");
        if (mainLayoutFxml == null) {
            throw new IOException("Không tìm thấy file giao diện /fxml/layout/main_layout.fxml");
        }

        Parent root = FXMLLoader.load(mainLayoutFxml);
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        Scene scene = new Scene(root, 1280, 800);

        URL cssResource = getClass().getResource("/css/app.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setTitle("PharmaOS — Quản lý cửa hàng thuốc");
        stage.setScene(scene);
        stage.setMinWidth(1160);
        stage.setMinHeight(720);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    private void setLoading(boolean loading) {
        if (btnLogin != null) {
            btnLogin.setDisable(loading);
            btnLogin.setText(loading ? "Đang xác thực..." : "Đăng nhập  →");
        }
    }

    private void showError(ErrorCode errorCode) {
        if (errorCode != null) {
            showError(errorCode.getDefaultMessage());
        }
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
            lblError.setStyle("-fx-text-fill: #E53935; -fx-font-size: 13px; -fx-font-weight: 500;");
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
        if (btnLogin != null && !btnLogin.isDisabled()) {
            btnLogin.setStyle(
                    "-fx-font-family: 'Inter 18pt SemiBold';" +
                            "-fx-background-color: #0052B4;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;"
            );
        }
    }

    @FXML
    public void onLoginHoverExit(javafx.scene.input.MouseEvent event) {
        if (btnLogin != null && !btnLogin.isDisabled()) {
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
}