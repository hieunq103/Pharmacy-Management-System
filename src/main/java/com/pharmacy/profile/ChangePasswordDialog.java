package com.pharmacy.profile;

import com.pharmacy.common.TogglePasswordField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ChangePasswordDialog extends Dialog<ButtonType> {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordDialog.class);

    private final ProfileService profileService;
    private final String identifier;
    private final Consumer<String> onSuccessCallback;

    private boolean isInternalClearing = false;

    private TogglePasswordField txtCurrentPassword;
    private TogglePasswordField txtNewPassword;
    private TogglePasswordField txtConfirmPassword;
    private Label lblDialogStatus;

    public ChangePasswordDialog(Window owner, ProfileService profileService, String identifier, Consumer<String> onSuccessCallback) {
        this.profileService = profileService;
        this.identifier = identifier;
        this.onSuccessCallback = onSuccessCallback;

        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.WINDOW_MODAL);
        setTitle("Đổi mật khẩu");
        buildUI();

        // Tự động focus vào ô mật khẩu hiện tại khi mở dialog
        Platform.runLater(() -> {
            if (txtCurrentPassword != null) {
                txtCurrentPassword.requestFocus();
            }
        });
    }

    private void buildUI() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        txtCurrentPassword = new TogglePasswordField("Nhập mật khẩu hiện tại");
        txtNewPassword = new TogglePasswordField("Tối thiểu 8 ký tự");
        txtConfirmPassword = new TogglePasswordField("Nhập lại mật khẩu mới");

        lblDialogStatus = new Label();
        lblDialogStatus.setWrapText(true);
        lblDialogStatus.setVisible(false);
        lblDialogStatus.setManaged(false);

        // Chỉ xóa thông báo khi người dùng chủ động gõ phím, không xóa khi code tự dọn ô nhập
        txtCurrentPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInternalClearing) clearError();
        });
        txtNewPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInternalClearing) clearError();
        });
        txtConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInternalClearing) clearError();
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(420);

        content.getChildren().addAll(
                createFieldBox("Mật khẩu hiện tại", txtCurrentPassword),
                createFieldBox("Mật khẩu mới", txtNewPassword),
                createFieldBox("Xác nhận mật khẩu mới", txtConfirmPassword),
                lblDialogStatus
        );

        dialogPane.setContent(content);
        setupButtons(dialogPane);
    }

    private void clearError() {
        if (lblDialogStatus.isVisible()) {
            lblDialogStatus.setText("");
            lblDialogStatus.setVisible(false);
            lblDialogStatus.setManaged(false);
        }
    }

    private VBox createFieldBox(String labelText, TogglePasswordField field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 13px; -fx-text-fill: #334155;");
        return new VBox(6, label, field);
    }

    private void setupButtons(DialogPane dialogPane) {
        Button btnOk = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button btnCancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        btnOk.setText("Xác nhận đổi");
        btnCancel.setText("Hủy");

        String btnBase = "-fx-font-family: 'Inter 18pt', 'Inter', sans-serif; -fx-font-size: 14px; -fx-font-weight: 600; -fx-pref-height: 38px; -fx-min-height: 38px; -fx-background-radius: 6; -fx-cursor: hand; ";

        // Nút xác nhận (.btn-success)
        String btnSuccessStyle = btnBase + "-fx-background-color: #16A34A !important; -fx-text-fill: white !important; -fx-padding: 0 20;";
        String btnSuccessHover = btnBase + "-fx-background-color: #15803D !important; -fx-text-fill: white !important; -fx-padding: 0 20;";
        btnOk.setStyle(btnSuccessStyle);
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(btnSuccessHover));
        btnOk.setOnMouseExited(e -> btnOk.setStyle(btnSuccessStyle));

        // Nút Hủy (.btn-secondary)
        String btnCancelStyle = btnBase + "-fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-padding: 0 20;";
        String btnCancelHover = btnBase + "-fx-background-color: #CBD5E1; -fx-text-fill: #334155; -fx-padding: 0 20;";
        btnCancel.setStyle(btnCancelStyle);
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(btnCancelHover));
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle(btnCancelStyle));

        btnOk.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            handleSubmit(btnOk, btnCancel);
        });
    }

    private void handleSubmit(Button btnOk, Button btnCancel) {
        String currentPass = txtCurrentPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (currentPass.isBlank()) {
            showError("Vui lòng nhập mật khẩu hiện tại.");
            txtCurrentPassword.requestFocus();
            return;
        }
        if (newPass.isBlank()) {
            showError("Vui lòng nhập mật khẩu mới.");
            txtNewPassword.requestFocus();
            return;
        }
        if (newPass.length() < 8) {
            showError("Mật khẩu mới phải có tối thiểu 8 ký tự.");
            txtNewPassword.requestFocus();
            return;
        }
        if (currentPass.equals(newPass)) {
            showError("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
            txtNewPassword.requestFocus();
            return;
        }
        if (confirmPass.isBlank()) {
            showError("Vui lòng xác nhận mật khẩu mới.");
            txtConfirmPassword.requestFocus();
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showError("Mật khẩu xác nhận không khớp.");
            txtConfirmPassword.requestFocus();
            return;
        }

        btnOk.setDisable(true);
        btnCancel.setDisable(true);
        clearError();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                profileService.changePassword(identifier, currentPass, newPass);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccessCallback != null) {
                onSuccessCallback.accept("Đổi mật khẩu thành công.");
            }
            close();
        });

        task.setOnFailed(e -> {
            btnOk.setDisable(false);
            btnCancel.setDisable(false);
            Throwable ex = task.getException();
            log.error("Lỗi đổi mật khẩu: ", ex);

            // Bóc tách đúng thông điệp lỗi từ ngoại lệ gốc (tránh bị nuốt message bởi Task/ExecutionException)
            String errorMsg = "Có lỗi xảy ra, vui lòng thử lại.";
            if (ex != null) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                    errorMsg = cause.getMessage();
                }
            }

            // 1. Hiển thị thông báo lỗi
            showError(errorMsg);

            // 2. Xóa ô mật khẩu hiện tại nhưng bọc cờ chặn để listener không xóa mất thông báo lỗi vừa hiển thị
            isInternalClearing = true;
            try {
                txtCurrentPassword.clear();
            } finally {
                isInternalClearing = false;
            }

            txtCurrentPassword.requestFocus();
        });

        Thread thread = new Thread(task, "change-password-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String message) {
        lblDialogStatus.setText(message);
        lblDialogStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-family: 'Inter 18pt'; -fx-font-size: 13px;");
        lblDialogStatus.setVisible(true);
        lblDialogStatus.setManaged(true);
    }
}