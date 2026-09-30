package com.pharmacy.profile;

import com.pharmacy.account.Account;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.Employee;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum StatusType {
        SUCCESS, ERROR, INFO
    }

    @FXML private Label lblStatusMessage;

    @FXML private TextField txtFullName;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtDob;
    @FXML private TextField txtGender;
    @FXML private TextField txtRole;

    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;

    @FXML private Button btnEdit;
    @FXML private HBox boxEditActions;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private Button btnChangePassword;

    private final ProfileService profileService = new ProfileServiceImpl();

    private PauseTransition statusTimer;

    @FXML
    public void initialize() {
        loadEmployeeData();
        setEditMode(false);

        txtPhone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblStatusMessage.isVisible()) {
                clearStatus();
            }
        });

        txtAddress.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblStatusMessage.isVisible()) {
                clearStatus();
            }
        });
    }

    private void loadEmployeeData() {
        if (!UserSession.isLoggedIn()) {
            return;
        }

        Account account = UserSession.getInstance().getAccount();
        if (account == null) return;

        Employee emp = account.getEmployee();
        if (emp != null) {
            txtFullName.setText(emp.getFullName() != null ? emp.getFullName() : "");
            txtNationalId.setText(emp.getNationalId() != null ? emp.getNationalId() : "");

            if (emp.getDob() != null) {
                txtDob.setText(emp.getDob().format(DATE_FORMATTER));
            } else {
                txtDob.setText("");
            }

            txtGender.setText(emp.getGender() != null ? emp.getGender() : "");
            txtPhone.setText(emp.getPhone() != null ? emp.getPhone() : "");
            txtAddress.setText(emp.getAddress() != null ? emp.getAddress() : "");
        }

        if (account.getRole() != null) {
            txtRole.setText(account.getRole().getName());
        }
    }

    private void setEditMode(boolean editing) {
        txtPhone.setEditable(editing);
        txtAddress.setEditable(editing);

        applyInputState(txtPhone, editing);
        applyInputState(txtAddress, editing);

        btnChangePassword.setVisible(true);
        btnChangePassword.setManaged(true);

        btnEdit.setVisible(!editing);
        btnEdit.setManaged(!editing);

        boxEditActions.setVisible(editing);
        boxEditActions.setManaged(editing);
    }

    private void applyInputState(Node node, boolean isEditable) {
        node.getStyleClass().removeAll("readonly", "editable");
        node.getStyleClass().add(isEditable ? "editable" : "readonly");
    }

    @FXML
    private void handleStartEditing(ActionEvent event) {
        clearStatus();
        setEditMode(true);
        txtPhone.requestFocus();
        txtPhone.positionCaret(txtPhone.getText() != null ? txtPhone.getText().length() : 0);
    }

    @FXML
    private void handleCancelEditing(ActionEvent event) {
        clearStatus();
        loadEmployeeData();
        setEditMode(false);
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        clearStatus();

        Employee emp = UserSession.getInstance().getEmployee();
        if (emp == null) {
            showStatus("Lỗi phiên làm việc: Không tìm thấy thông tin nhân viên.", StatusType.ERROR);
            return;
        }

        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String address = txtAddress.getText() != null ? txtAddress.getText().trim() : "";

        String currentPhone = emp.getPhone() != null ? emp.getPhone().trim() : "";
        String currentAddress = emp.getAddress() != null ? emp.getAddress().trim() : "";

        if (phone.equals(currentPhone) && address.equals(currentAddress)) {
            showStatus("Thông tin được giữ nguyên, không có thay đổi nào cần lưu.", StatusType.INFO);
            setEditMode(false);
            return;
        }

        ProfileUpdateRequest request;
        try {
            request = new ProfileUpdateRequest(emp.getId(), phone, address);
        } catch (IllegalArgumentException ex) {
            showStatus(ex.getMessage(), StatusType.ERROR);
            txtPhone.requestFocus();
            return;
        }

        btnSave.setDisable(true);
        btnCancel.setDisable(true);

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                profileService.updateProfile(request);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            btnSave.setDisable(false);
            btnCancel.setDisable(false);

            emp.setPhone(request.phone());
            emp.setAddress(request.address());

            showStatus("Cập nhật thông tin liên hệ thành công!", StatusType.SUCCESS);
            setEditMode(false);
        });

        saveTask.setOnFailed(e -> {
            btnSave.setDisable(false);
            btnCancel.setDisable(false);
            Throwable ex = saveTask.getException();
            log.error("Lưu hồ sơ thất bại: ", ex);

            String errorMsg = ex.getMessage();
            if (ex instanceof IllegalArgumentException) {
                showStatus(errorMsg, StatusType.ERROR);
                txtPhone.requestFocus();
            } else if (errorMsg != null && (errorMsg.contains("Duplicate entry") || errorMsg.contains("ConstraintViolationException"))) {
                showStatus("Số điện thoại này đã được sử dụng bởi một nhân viên khác.", StatusType.ERROR);
                txtPhone.requestFocus();
            } else {
                showStatus("Lỗi khi lưu dữ liệu: " + (errorMsg != null ? errorMsg : "Không xác định"), StatusType.ERROR);
            }
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        clearStatus();

        Employee emp = UserSession.getInstance().getEmployee();
        if (emp == null || emp.getPhone() == null || emp.getPhone().isBlank()) {
            showStatus("Không tìm thấy thông tin số điện thoại của tài khoản.", StatusType.ERROR);
            return;
        }

        Window owner = btnChangePassword.getScene().getWindow();
        ChangePasswordDialog dialog = new ChangePasswordDialog(
                owner,
                profileService,
                emp.getPhone(),
                successMsg -> showStatus(successMsg, StatusType.SUCCESS)
        );

        dialog.showAndWait();
    }

    private void showStatus(String message, StatusType type) {
        if (statusTimer != null) {
            statusTimer.stop();
        }

        lblStatusMessage.setText(message);
        lblStatusMessage.setVisible(true);
        lblStatusMessage.setManaged(true);

        switch (type) {
            case ERROR -> lblStatusMessage.setStyle(
                    "-fx-background-color: #FEE2E2; " +
                            "-fx-text-fill: #DC2626; " +
                            "-fx-padding: 8 12; " +
                            "-fx-background-radius: 6; " +
                            "-fx-font-family: 'Inter 18pt', 'Inter', sans-serif; " +
                            "-fx-font-size: 13.5px;"
            );
            case SUCCESS -> {
                lblStatusMessage.setStyle(
                        "-fx-background-color: #DCFCE7; " +
                                "-fx-text-fill: #16A34A; " +
                                "-fx-padding: 8 12; " +
                                "-fx-background-radius: 6; " +
                                "-fx-font-family: 'Inter 18pt', 'Inter', sans-serif; " +
                                "-fx-font-size: 13.5px;"
                );
                statusTimer = new PauseTransition(Duration.seconds(3));
                statusTimer.setOnFinished(event -> clearStatus());
                statusTimer.play();
            }
            case INFO -> {
                lblStatusMessage.setStyle(
                        "-fx-background-color: #EFF6FF; " +
                                "-fx-text-fill: #2563EB; " +
                                "-fx-padding: 8 12; " +
                                "-fx-background-radius: 6; " +
                                "-fx-font-family: 'Inter 18pt', 'Inter', sans-serif; " +
                                "-fx-font-size: 13.5px;"
                );
                statusTimer = new PauseTransition(Duration.seconds(3));
                statusTimer.setOnFinished(event -> clearStatus());
                statusTimer.play();
            }
        }
    }

    private void clearStatus() {
        if (statusTimer != null) {
            statusTimer.stop();
        }
        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
        lblStatusMessage.setText("");
    }
}