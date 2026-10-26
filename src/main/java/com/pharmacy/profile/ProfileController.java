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

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.FormValidator;

public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    public enum StatusType {
        SUCCESS, ERROR, INFO
    }

    @FXML
    private Label lblStatusMessage;

    @FXML
    private TextField txtEmployeeCode;
    @FXML
    private TextField txtHiredDate;
    @FXML
    private TextField txtFullName;
    @FXML
    private TextField txtNationalId;
    @FXML
    private TextField txtDob;
    @FXML
    private TextField txtGender;
    @FXML
    private TextField txtRole;

    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtAddress;

    @FXML
    private Button btnEdit;
    @FXML
    private HBox boxEditActions;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnChangePassword;

    private final ProfileService profileService = new ProfileServiceImpl();
    private PauseTransition statusTimer;

    @FXML
    public void initialize() {
        loadEmployeeData();
        setEditMode(false);

        txtFullName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblStatusMessage.isVisible()) {
                clearStatus();
            }
        });
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
        if (account == null)
            return;

        Employee emp = account.getEmployee();
        if (emp != null) {
            txtEmployeeCode.setText(emp.getCode() != null ? emp.getCode() : "");
            txtHiredDate.setText(emp.getHiredDate() != null ? FormatUtils.formatDate(emp.getHiredDate()) : "Chưa cập nhật");
            txtFullName.setText(emp.getFullName() != null ? emp.getFullName() : "");
            txtNationalId.setText(emp.getNationalId() != null ? emp.getNationalId() : "");

            if (emp.getDob() != null) {
                txtDob.setText(FormatUtils.formatDate(emp.getDob()));
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
        // Cả Nhân viên và Quản lý chỉ được đổi Họ và tên, Số điện thoại, Địa chỉ của chính mình
        for (TextField editable : new TextField[]{txtFullName, txtPhone, txtAddress}) {
            editable.setEditable(editing);
            applyInputState(editable, editing);
        }
        for (TextField locked : new TextField[]{txtEmployeeCode, txtHiredDate, txtNationalId, txtDob, txtGender}) {
            locked.setEditable(false);
            applyInputState(locked, false);
        }

        txtRole.setEditable(false);
        applyInputState(txtRole, false);

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
        txtFullName.requestFocus();
        txtFullName.positionCaret(txtFullName.getText() != null ? txtFullName.getText().length() : 0);
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

        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String address = txtAddress.getText() != null ? txtAddress.getText().trim() : "";

        if (fullName.isEmpty()) {
            showStatus("Họ và tên không được để trống.", StatusType.ERROR);
            txtFullName.requestFocus();
            return;
        }

        if (fullName.length() > 100) {
            showStatus("Họ và tên không được vượt quá 100 ký tự.", StatusType.ERROR);
            txtFullName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            showStatus("Số điện thoại không được để trống.", StatusType.ERROR);
            txtPhone.requestFocus();
            return;
        }

        if (!FormValidator.isValidPhone(phone)) {
            showStatus("Số điện thoại không hợp lệ (phải gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).",
                    StatusType.ERROR);
            txtPhone.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            showStatus("Địa chỉ không được để trống.", StatusType.ERROR);
            txtAddress.requestFocus();
            return;
        }

        if (address.length() > 255) {
            showStatus("Địa chỉ không được vượt quá 255 ký tự.", StatusType.ERROR);
            txtAddress.requestFocus();
            return;
        }

        boolean hasChanged = !fullName.equals(emp.getFullName() != null ? emp.getFullName().trim() : "")
                || !phone.equals(emp.getPhone() != null ? emp.getPhone().trim() : "")
                || !address.equals(emp.getAddress() != null ? emp.getAddress().trim() : "");

        if (!hasChanged) {
            showStatus("Thông tin được giữ nguyên, không có thay đổi nào cần lưu.", StatusType.INFO);
            setEditMode(false);
            return;
        }

        final ProfileUpdateRequest request = new ProfileUpdateRequest(emp.getId(), fullName, phone, address);

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

            emp.setFullName(request.fullName());
            emp.setPhone(request.phone());
            emp.setAddress(request.address());

            showStatus("Cập nhật thông tin thành công!", StatusType.SUCCESS);
            loadEmployeeData();
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
            } else if (errorMsg != null
                    && (errorMsg.contains("Duplicate entry") || errorMsg.contains("ConstraintViolationException"))) {
                showStatus("Số điện thoại này đã được sử dụng bởi một nhân viên khác.", StatusType.ERROR);
                txtPhone.requestFocus();
            } else {
                showStatus("Lỗi khi lưu dữ liệu: " + (errorMsg != null ? errorMsg : "Không xác định"),
                        StatusType.ERROR);
            }
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        clearStatus();

        Account acc = UserSession.getInstance().getAccount();
        String username = (acc != null && acc.getUsername() != null && !acc.getUsername().isBlank())
                ? acc.getUsername()
                : (UserSession.getInstance().getEmployee() != null ? UserSession.getInstance().getEmployee().getCode() : null);

        if (username == null || username.isBlank()) {
            showStatus("Không tìm thấy thông tin tài khoản đăng nhập.", StatusType.ERROR);
            return;
        }

        Window owner = btnChangePassword.getScene().getWindow();
        ChangePasswordDialog dialog = new ChangePasswordDialog(
                owner,
                profileService,
                username,
                successMsg -> showStatus(successMsg, StatusType.SUCCESS));

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
                            "-fx-font-family: 'Inter 18pt SemiBold'; " +
                            "-fx-font-size: 13.5px;");
            case SUCCESS -> {
                lblStatusMessage.setStyle(
                        "-fx-background-color: #DCFCE7; " +
                                "-fx-text-fill: #16A34A; " +
                                "-fx-padding: 8 12; " +
                                "-fx-background-radius: 6; " +
                                "-fx-font-family: 'Inter 18pt SemiBold'; " +
                                "-fx-font-size: 13.5px;");
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
                                "-fx-font-family: 'Inter 18pt SemiBold'; " +
                                "-fx-font-size: 13.5px;");
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