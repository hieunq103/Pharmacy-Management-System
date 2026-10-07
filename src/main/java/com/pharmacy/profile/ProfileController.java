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
import javafx.scene.control.ComboBox;
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
    private static final String PHONE_REGEX = "^0[35789]\\d{8}$";

    public enum StatusType {
        SUCCESS, ERROR, INFO
    }

    @FXML private Label lblStatusMessage;

    @FXML private TextField txtFullName;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtDob;
    @FXML private TextField txtGender;
    @FXML private ComboBox<String> cbGender;
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

        cbGender.getItems().setAll("Nam", "Nữ", "Khác");
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
            cbGender.setValue(emp.getGender() != null ? emp.getGender() : "");
        }

        if (account.getRole() != null) {
            txtRole.setText(account.getRole().getName());
        }
    }

    private void setEditMode(boolean editing) {
        boolean isManager = UserSession.isLoggedIn() && UserSession.getInstance().isManager();

        txtPhone.setEditable(editing);
        txtAddress.setEditable(editing);
        applyInputState(txtPhone, editing);
        applyInputState(txtAddress, editing);

        boolean managerEditable = editing && isManager;
        txtFullName.setEditable(managerEditable);
        txtNationalId.setEditable(managerEditable);
        txtDob.setEditable(managerEditable);

        applyInputState(txtFullName, managerEditable);
        applyInputState(txtNationalId, managerEditable);
        applyInputState(txtDob, managerEditable);

        txtGender.setVisible(!managerEditable);
        txtGender.setManaged(!managerEditable);

        cbGender.setVisible(managerEditable);
        cbGender.setManaged(managerEditable);
        if (managerEditable) {
            cbGender.setValue(txtGender.getText());
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

        boolean isManager = UserSession.getInstance().isManager();

        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String address = txtAddress.getText() != null ? txtAddress.getText().trim() : "";

        if (phone.isEmpty()) {
            showStatus("Số điện thoại không được để trống.", StatusType.ERROR);
            txtPhone.requestFocus();
            return;
        }

        if (!phone.matches(PHONE_REGEX)) {
            showStatus("Số điện thoại không hợp lệ (phải gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).", StatusType.ERROR);
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

        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        String nationalId = txtNationalId.getText() != null ? txtNationalId.getText().trim() : "";
        String gender = isManager
                ? (cbGender.getValue() != null ? cbGender.getValue() : "")
                : (txtGender.getText() != null ? txtGender.getText().trim() : "");
        java.time.LocalDate parsedDob = emp.getDob();

        if (isManager) {
            if (fullName.isEmpty()) {
                showStatus("Họ và tên không được để trống.", StatusType.ERROR);
                txtFullName.requestFocus();
                return;
            }

            if (!nationalId.matches("^\\d{12}$")) {
                showStatus("Căn cước công dân phải đúng 12 chữ số.", StatusType.ERROR);
                txtNationalId.requestFocus();
                return;
            }

            String dobStr = txtDob.getText() != null ? txtDob.getText().trim() : "";
            try {
                parsedDob = java.time.LocalDate.parse(dobStr, DATE_FORMATTER);
                if (parsedDob.isAfter(java.time.LocalDate.now().minusYears(18))) {
                    showStatus("Nhân viên phải từ đủ 18 tuổi trở lên.", StatusType.ERROR);
                    txtDob.requestFocus();
                    return;
                }
            } catch (Exception ex) {
                showStatus("Ngày sinh sai định dạng (dd/MM/yyyy).", StatusType.ERROR);
                txtDob.requestFocus();
                return;
            }
        }

        String currentPhone = emp.getPhone() != null ? emp.getPhone().trim() : "";
        String currentAddress = emp.getAddress() != null ? emp.getAddress().trim() : "";

        boolean hasChanged;
        if (isManager) {
            String currentFullName = emp.getFullName() != null ? emp.getFullName().trim() : "";
            String currentNationalId = emp.getNationalId() != null ? emp.getNationalId().trim() : "";
            String currentGender = emp.getGender() != null ? emp.getGender().trim() : "";
            java.time.LocalDate currentDob = emp.getDob();

            boolean phoneChanged = !phone.equals(currentPhone);
            boolean addressChanged = !address.equals(currentAddress);
            boolean nameChanged = !fullName.equals(currentFullName);
            boolean nationalIdChanged = !nationalId.equals(currentNationalId);
            boolean genderChanged = !gender.equals(currentGender);
            boolean dobChanged = (parsedDob != null && !parsedDob.equals(currentDob))
                    || (parsedDob == null && currentDob != null);

            hasChanged = phoneChanged || addressChanged || nameChanged
                    || nationalIdChanged || genderChanged || dobChanged;
        } else {
            hasChanged = !phone.equals(currentPhone) || !address.equals(currentAddress);
        }

        if (!hasChanged) {
            showStatus("Thông tin được giữ nguyên, không có thay đổi nào cần lưu.", StatusType.INFO);
            setEditMode(false);
            return;
        }

        final ProfileUpdateRequest request;
        if (isManager) {
            request = new ProfileUpdateRequest(emp.getId(), phone, address, fullName, nationalId, parsedDob, gender);
        } else {
            request = new ProfileUpdateRequest(emp.getId(), phone, address);
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
            if (request.isManagerUpdate()) {
                emp.setFullName(request.fullName());
                emp.setNationalId(request.nationalId());
                emp.setDob(request.dob());
                emp.setGender(request.gender());
            }

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
            } else if (errorMsg != null && (errorMsg.contains("Duplicate entry") || errorMsg.contains("ConstraintViolationException"))) {
                showStatus("Số điện thoại hoặc CCCD này đã được sử dụng bởi một nhân viên khác.", StatusType.ERROR);
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