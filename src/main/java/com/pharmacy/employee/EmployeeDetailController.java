package com.pharmacy.employee;

import com.pharmacy.account.Account;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.UserSession;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class EmployeeDetailController {
    private static final Logger log = LoggerFactory.getLogger(EmployeeDetailController.class);

    @FXML private Button btnClose;
    @FXML private Button btnViewHistory;
    @FXML private Button btnUpdateProfile;
    @FXML private HBox boxEditActions;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    @FXML private TextField txtEmpId;
    @FXML private TextField txtFullName;
    @FXML private TextField txtDob;
    @FXML private TextField txtGender;
    @FXML private ComboBox<String> cbGender;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtPhone;
    @FXML private TextField txtHiredDate;
    @FXML private TextField txtAddress;

    @FXML private TextField txtEmpStatus;
    @FXML private ComboBox<String> cbEmpStatus;

    @FXML private TextField txtRole;
    @FXML private ComboBox<String> cbRole;
    @FXML private TextField txtAccountStatus;
    @FXML private ComboBox<String> cbAccountStatus;

    @FXML private Label lblErrFullName;
    @FXML private Label lblErrDob;
    @FXML private Label lblErrNationalId;
    @FXML private Label lblErrPhone;
    @FXML private Label lblErrAddress;

    private Employee employee;
    private Runnable onBackCallback;
    private final EmployeeService employeeService = new EmployeeServiceImpl();

    @FXML
    public void initialize() {
        if (cbGender != null) {
            cbGender.getItems().setAll(Employee.GENDER_MALE, Employee.GENDER_FEMALE, Employee.GENDER_OTHER);
        }
        if (cbEmpStatus != null) {
            cbEmpStatus.getItems().setAll("Đang làm việc", "Đã nghỉ việc");
            cbEmpStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyAccountStatusConstraint(newVal));
        }
        if (cbRole != null) {
            cbRole.getItems().setAll("Nhân viên bán thuốc", "Quản lý cửa hàng");
        }
        if (cbAccountStatus != null) {
            cbAccountStatus.getItems().setAll("Đang hoạt động", "Đang bị khóa");
        }

        setupAutoClearError(txtFullName, lblErrFullName);
        setupAutoClearError(txtDob, lblErrDob);
        setupAutoClearError(txtNationalId, lblErrNationalId);
        setupAutoClearError(txtPhone, lblErrPhone);
        setupAutoClearError(txtAddress, lblErrAddress);

        setEditMode(false);
    }

    public void setEmployeeData(Employee emp, Runnable onBack) {
        this.employee = emp;
        this.onBackCallback = onBack;
        populateData();
    }

    private void populateData() {
        if (employee == null) return;

        boolean isInactive = (employee.getStatus() == Employee.Status.INACTIVE);

        setTextSafe(txtEmpId, (employee.getCode() != null && !employee.getCode().isBlank())
                ? employee.getCode()
                : (employee.getId() != null ? String.valueOf(employee.getId()) : ""));
        setTextSafe(txtFullName, employee.getFullName());
        setTextSafe(txtDob, employee.getDob() != null ? FormatUtils.formatDate(employee.getDob()) : "");
        setTextSafe(txtGender, employee.getGender());
        if (cbGender != null) {
            cbGender.setValue(employee.getGender() != null ? employee.getGender() : "Nam");
        }

        setTextSafe(txtNationalId, employee.getNationalId());
        setTextSafe(txtPhone, employee.getPhone());
        setTextSafe(txtHiredDate, employee.getHiredDate() != null ? FormatUtils.formatDate(employee.getHiredDate()) : "Chưa cập nhật");
        setTextSafe(txtAddress, employee.getAddress());

        String empStatusStr = isInactive ? "Đã nghỉ việc" : "Đang làm việc";
        setTextSafe(txtEmpStatus, empStatusStr);
        if (cbEmpStatus != null) {
            cbEmpStatus.setValue(empStatusStr);
        }

        Account acc = employee.getAccount();
        if (acc != null) {
            String roleName = (acc.getRole() != null && acc.getRole().getName() != null)
                    ? acc.getRole().getName()
                    : "Nhân viên bán thuốc";
            String accStatusStr = (acc.getStatus() == Account.Status.ACTIVE)
                    ? "Đang hoạt động"
                    : "Đang bị khóa";

            setTextSafe(txtRole, roleName);
            if (cbRole != null) {
                cbRole.setValue(roleName);
            }

            setTextSafe(txtAccountStatus, accStatusStr);
            if (cbAccountStatus != null) {
                cbAccountStatus.setValue(accStatusStr);
            }

            if (btnViewHistory != null) {
                btnViewHistory.setDisable(false);
            }
        } else {
            setTextSafe(txtRole, "-");
            setTextSafe(txtAccountStatus, "-");

            if (btnViewHistory != null) {
                btnViewHistory.setDisable(true);
            }
        }

        if (btnUpdateProfile != null) {
            btnUpdateProfile.setDisable(false);
        }
    }

    private void applyAccountStatusConstraint(String empStatusValue) {
        if (cbAccountStatus == null) return;
        boolean isResigned = "Đã nghỉ việc".equalsIgnoreCase(empStatusValue);
        if (isResigned) {
            cbAccountStatus.setValue("Đang bị khóa");
            cbAccountStatus.setDisable(true);
        } else {
            cbAccountStatus.setDisable(false);
        }
    }

    private void setTextSafe(TextField field, String value) {
        if (field != null) {
            field.setText(value != null ? value : "");
        }
    }

    private boolean isEditingSelf() {
        if (!UserSession.isLoggedIn() || employee == null) {
            return false;
        }
        Integer loggedInEmpId = UserSession.getInstance().getEmployeeId();
        if (loggedInEmpId != null && loggedInEmpId.equals(employee.getId())) {
            return true;
        }
        String loggedInCode = UserSession.getInstance().getEmployeeCode();
        return loggedInCode != null && !loggedInCode.isBlank() && loggedInCode.equalsIgnoreCase(employee.getCode());
    }

    private void setEditMode(boolean editing) {
        setEditableSafe(txtEmpId, false);
        setEditableSafe(txtHiredDate, false);

        setEditableSafe(txtFullName, editing);
        setEditableSafe(txtPhone, editing);
        setEditableSafe(txtAddress, editing);

        boolean canEditIdentity = editing && !isEditingSelf();
        setEditableSafe(txtDob, canEditIdentity);
        setEditableSafe(txtNationalId, canEditIdentity);
        toggleField(txtGender, cbGender, canEditIdentity);

        boolean canManageRoleAndStatus = editing && !isEditingSelf();
        toggleField(txtEmpStatus, cbEmpStatus, canManageRoleAndStatus);
        toggleField(txtRole, cbRole, canManageRoleAndStatus);
        toggleField(txtAccountStatus, cbAccountStatus, canManageRoleAndStatus);

        setEditableSafe(txtEmpStatus, false);
        setEditableSafe(txtRole, false);
        setEditableSafe(txtAccountStatus, false);

        if (editing) {
            Tooltip fixedTooltip = new Tooltip("Mã nhân viên và ngày vào làm là thông tin cố định, không thể chỉnh sửa.");
            if (txtEmpId != null) txtEmpId.setTooltip(fixedTooltip);
            if (txtHiredDate != null) txtHiredDate.setTooltip(fixedTooltip);

            if (isEditingSelf()) {
                Tooltip selfTooltip = new Tooltip("Với hồ sơ của chính mình, chỉ có thể thay đổi Họ và tên, Số điện thoại và Địa chỉ.");
                if (txtRole != null) txtRole.setTooltip(selfTooltip);
                if (txtEmpStatus != null) txtEmpStatus.setTooltip(selfTooltip);
                if (txtAccountStatus != null) txtAccountStatus.setTooltip(selfTooltip);
                if (txtDob != null) txtDob.setTooltip(selfTooltip);
                if (txtNationalId != null) txtNationalId.setTooltip(selfTooltip);
                if (txtGender != null) txtGender.setTooltip(selfTooltip);
            }
        } else {
            if (txtEmpId != null) txtEmpId.setTooltip(null);
            if (txtHiredDate != null) txtHiredDate.setTooltip(null);
            if (txtRole != null) txtRole.setTooltip(null);
            if (txtEmpStatus != null) txtEmpStatus.setTooltip(null);
            if (txtAccountStatus != null) txtAccountStatus.setTooltip(null);
            if (txtDob != null) txtDob.setTooltip(null);
            if (txtNationalId != null) txtNationalId.setTooltip(null);
            if (txtGender != null) txtGender.setTooltip(null);
        }

        if (canManageRoleAndStatus && cbEmpStatus != null) {
            applyAccountStatusConstraint(cbEmpStatus.getValue());
        }

        if (btnUpdateProfile != null) {
            btnUpdateProfile.setVisible(!editing);
            btnUpdateProfile.setManaged(!editing);
        }
        if (boxEditActions != null) {
            boxEditActions.setVisible(editing);
            boxEditActions.setManaged(editing);
        }
    }

    private void toggleField(TextField txt, ComboBox<String> cb, boolean editing) {
        if (txt != null && cb != null) {
            txt.setVisible(!editing);
            txt.setManaged(!editing);
            cb.setVisible(editing);
            cb.setManaged(editing);
            if (editing && txt.getText() != null && !txt.getText().isBlank()) {
                cb.setValue(txt.getText());
            }
        }
    }

    private void setEditableSafe(TextField field, boolean editable) {
        if (field == null) return;
        field.setEditable(editable);
        field.getStyleClass().removeAll("form-input-readonly", "form-input-editable");
        if (editable) {
            field.getStyleClass().add("form-input-editable");
        } else {
            field.getStyleClass().add("form-input-readonly");
        }
    }

    @FXML
    private void handleUpdateProfile() {
        setEditMode(true);
        if (txtFullName != null) {
            txtFullName.requestFocus();
        }
    }

    @FXML
    private void handleCancelEditing() {
        populateData();
        clearAllErrors();
        setEditMode(false);
    }

    @FXML
    private void handleSaveProfile() {
        if (employee == null) return;

        clearAllErrors();

        String fullName = (txtFullName != null && txtFullName.getText() != null) ? txtFullName.getText().trim() : "";
        String nationalId = (txtNationalId != null && txtNationalId.getText() != null) ? txtNationalId.getText().trim() : "";
        String phone = (txtPhone != null && txtPhone.getText() != null) ? txtPhone.getText().trim() : "";
        String address = (txtAddress != null && txtAddress.getText() != null) ? txtAddress.getText().trim() : "";
        String gender = (cbGender != null && cbGender.getValue() != null)
                ? cbGender.getValue()
                : ((txtGender != null && txtGender.getText() != null) ? txtGender.getText().trim() : "");
        if (isEditingSelf()) {
            nationalId = employee.getNationalId() != null ? employee.getNationalId() : "";
            gender = employee.getGender();
        }

        String empStatus;
        String accountStatus;
        String roleName;

        if (isEditingSelf()) {
            empStatus = (employee.getStatus() == Employee.Status.INACTIVE) ? "Đã nghỉ việc" : "Đang làm việc";
            Account acc = employee.getAccount();
            accountStatus = (acc != null && acc.getStatus() == Account.Status.DISABLED) ? "Đang bị khóa" : "Đang hoạt động";
            roleName = (acc != null && acc.getRole() != null && acc.getRole().getName() != null)
                    ? acc.getRole().getName()
                    : "Quản lý cửa hàng";
        } else {
            empStatus = (cbEmpStatus != null && cbEmpStatus.getValue() != null)
                    ? cbEmpStatus.getValue()
                    : ((txtEmpStatus != null && txtEmpStatus.getText() != null) ? txtEmpStatus.getText().trim() : "Đang làm việc");

            if ("Đã nghỉ việc".equalsIgnoreCase(empStatus)) {
                accountStatus = "Đang bị khóa";
            } else {
                accountStatus = (cbAccountStatus != null && cbAccountStatus.getValue() != null)
                        ? cbAccountStatus.getValue()
                        : ((txtAccountStatus != null && txtAccountStatus.getText() != null) ? txtAccountStatus.getText().trim() : "Đang hoạt động");
            }

            roleName = (cbRole != null && cbRole.getValue() != null)
                    ? cbRole.getValue()
                    : ((txtRole != null && txtRole.getText() != null) ? txtRole.getText().trim() : "Nhân viên bán thuốc");
        }

        boolean hasError = false;

        if (fullName.isEmpty()) {
            showFieldError(txtFullName, lblErrFullName, "Họ và tên không được để trống.");
            hasError = true;
        }

        LocalDate parsedDob = null;
        String dobStr = (txtDob != null && txtDob.getText() != null) ? txtDob.getText().trim() : "";
        if (isEditingSelf()) {
            parsedDob = employee.getDob();
        } else if (dobStr.isEmpty()) {
            showFieldError(txtDob, lblErrDob, "Ngày sinh không được để trống.");
            hasError = true;
        } else {
            try {
                parsedDob = LocalDate.parse(dobStr, FormatUtils.DATE_FORMATTER);
                if (parsedDob.isAfter(LocalDate.now().minusYears(18))) {
                    showFieldError(txtDob, lblErrDob, "Nhân viên phải từ đủ 18 tuổi trở lên.");
                    hasError = true;
                }
            } catch (Exception ex) {
                showFieldError(txtDob, lblErrDob, "Ngày sinh sai định dạng (dd/MM/yyyy).");
                hasError = true;
            }
        }

        if (nationalId.isEmpty()) {
            showFieldError(txtNationalId, lblErrNationalId, "Căn cước công dân không được để trống.");
            hasError = true;
        } else if (!FormValidator.isValidNationalId(nationalId)) {
            showFieldError(txtNationalId, lblErrNationalId, "Căn cước công dân phải đúng 12 chữ số.");
            hasError = true;
        }

        if (phone.isEmpty()) {
            showFieldError(txtPhone, lblErrPhone, "Số điện thoại không được để trống.");
            hasError = true;
        } else if (!FormValidator.isValidPhone(phone)) {
            showFieldError(txtPhone, lblErrPhone, "Số điện thoại không hợp lệ (10 số, đầu 03, 05, 07, 08, 09).");
            hasError = true;
        }

        if (address.isEmpty()) {
            showFieldError(txtAddress, lblErrAddress, "Địa chỉ không được để trống.");
            hasError = true;
        } else if (address.length() > 255) {
            showFieldError(txtAddress, lblErrAddress, "Địa chỉ không được quá 255 ký tự.");
            hasError = true;
        }

        if (hasError) return;

        final EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                employee.getId(),
                fullName,
                nationalId,
                phone,
                address,
                parsedDob,
                gender,
                empStatus,
                accountStatus,
                roleName
        );

        if (btnSave != null) btnSave.setDisable(true);
        if (btnCancel != null) btnCancel.setDisable(true);

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return employeeService.updateEmployeeByManager(request);
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (btnSave != null) btnSave.setDisable(false);
            if (btnCancel != null) btnCancel.setDisable(false);

            boolean success = Boolean.TRUE.equals(saveTask.getValue());
            if (success) {
                employee.setFullName(request.fullName());
                employee.setNationalId(request.nationalId());
                employee.setDob(request.dob());
                employee.setGender(request.gender());
                employee.setPhone(request.phone());
                employee.setAddress(request.address());

                if (!isEditingSelf()) {
                    employee.setStatus(
                            ("INACTIVE".equalsIgnoreCase(request.empStatus()) || "Đã nghỉ việc".equalsIgnoreCase(request.empStatus()))
                                    ? Employee.Status.INACTIVE
                                    : Employee.Status.ACTIVE
                    );

                    if (employee.getAccount() != null) {
                        employee.getAccount().setStatus(
                                ("DISABLED".equalsIgnoreCase(request.accountStatus()) || "Đang bị khóa".equalsIgnoreCase(request.accountStatus()))
                                        ? Account.Status.DISABLED
                                        : Account.Status.ACTIVE
                        );
                        if (employee.getAccount().getRole() != null) {
                            employee.getAccount().getRole().setName(request.roleName());
                        }
                    }
                } else if (UserSession.isLoggedIn() && UserSession.getInstance().getEmployee() != null) {
                    Employee sessionEmp = UserSession.getInstance().getEmployee();
                    sessionEmp.setFullName(request.fullName());
                    sessionEmp.setNationalId(request.nationalId());
                    sessionEmp.setDob(request.dob());
                    sessionEmp.setGender(request.gender());
                    sessionEmp.setPhone(request.phone());
                    sessionEmp.setAddress(request.address());
                }

                clearAllErrors();
                DialogUtils.showSuccess(getWindow(), "Thông báo", "Cập nhật hồ sơ thành công!");
                populateData();
                setEditMode(false);
            } else {
                DialogUtils.showWarning(getWindow(), "Cảnh báo", "Không thể cập nhật. Bản ghi không tồn tại hoặc không có thay đổi.");
            }
        });

        saveTask.setOnFailed(e -> {
            if (btnSave != null) btnSave.setDisable(false);
            if (btnCancel != null) btnCancel.setDisable(false);
            Throwable ex = saveTask.getException();
            log.error("Lỗi cập nhật hồ sơ nhân viên: ", ex);

            String msg = ex != null ? ex.getMessage() : "";
            if (msg != null && (msg.contains("Số điện thoại") || msg.toLowerCase().contains("phone"))) {
                showFieldError(txtPhone, lblErrPhone, "Số điện thoại này đã thuộc về nhân viên khác.");
            } else if (msg != null && (msg.contains("Căn cước") || msg.contains("CCCD") || msg.toLowerCase().contains("national"))) {
                showFieldError(txtNationalId, lblErrNationalId, "Số CCCD này đã thuộc về nhân viên khác.");
            } else if (msg != null && (msg.contains("Duplicate entry") || msg.contains("ConstraintViolationException"))) {
                showFieldError(txtPhone, lblErrPhone, "Số điện thoại hoặc CCCD đã bị trùng lặp trong hệ thống.");
            } else {
                DialogUtils.showError(getWindow(), "Lỗi", msg != null && !msg.isBlank() ? msg : "Có lỗi xảy ra khi lưu dữ liệu.");
            }
        });

        Thread th = new Thread(saveTask);
        th.setDaemon(true);
        th.start();
    }

    private void setupAutoClearError(TextField field, Label errLabel) {
        if (field == null) return;

        field.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                clearFieldError(field, errLabel);
            }
        });

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            clearFieldError(field, errLabel);
        });
    }

    private void showFieldError(TextField field, Label errLabel, String message) {
        if (errLabel != null) {
            errLabel.setText(message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        }
        if (field != null && !field.getStyleClass().contains("form-input-error")) {
            field.getStyleClass().add("form-input-error");
        }
    }

    private void clearAllErrors() {
        clearFieldError(txtFullName, lblErrFullName);
        clearFieldError(txtDob, lblErrDob);
        clearFieldError(txtNationalId, lblErrNationalId);
        clearFieldError(txtPhone, lblErrPhone);
        clearFieldError(txtAddress, lblErrAddress);
    }

    private void clearFieldError(TextField field, Label errLabel) {
        if (errLabel != null) {
            errLabel.setText("");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
        if (field != null) {
            field.getStyleClass().removeAll("form-input-error");
        }
    }

    private Window getWindow() {
        if (btnSave != null && btnSave.getScene() != null) {
            return btnSave.getScene().getWindow();
        }
        if (btnClose != null && btnClose.getScene() != null) {
            return btnClose.getScene().getWindow();
        }
        return DialogUtils.getActiveWindow();
    }

    @FXML
    private void handleClose() {
        if (onBackCallback != null) {
            onBackCallback.run();
        }
    }

    @FXML
    private void handleViewLoginHistory() {
        if (employee == null || employee.getAccount() == null) return;

        Integer accountId = employee.getAccount().getId();
        String empName = employee.getFullName();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử đăng nhập - " + (empName != null ? empName : ""));
        DialogUtils.configure(dialog, getWindow());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<Object[]> table = new TableView<>();
        table.setPrefWidth(560);
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Object[], String> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(cell -> {
            Object val = cell.getValue()[0];
            if (val instanceof java.sql.Timestamp ts) {
                return new SimpleStringProperty(ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
            }
            if (val instanceof java.time.LocalDateTime ldt) {
                return new SimpleStringProperty(ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
            }
            return new SimpleStringProperty(val != null ? val.toString().replace(".0", "") : "");
        });

        TableColumn<Object[], String> colResult = new TableColumn<>("Kết quả");
        colResult.setCellValueFactory(cell -> {
            Object val = cell.getValue()[1];
            String res = val != null ? val.toString() : "";
            return new SimpleStringProperty("SUCCESS".equalsIgnoreCase(res) ? "Thành công" : "Thất bại");
        });

        table.getColumns().add(colTime);
        table.getColumns().add(colResult);
        table.setPlaceholder(new Label("Chưa có lịch sử đăng nhập nào."));

        List<Object[]> historyList = employeeService.getLoginHistory(accountId);
        table.setItems(FXCollections.observableArrayList(historyList));

        dialog.getDialogPane().setContent(table);
        dialog.showAndWait();
    }
}
