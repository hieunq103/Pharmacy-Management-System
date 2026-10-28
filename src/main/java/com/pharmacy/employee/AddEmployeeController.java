package com.pharmacy.employee;

import com.pharmacy.common.BackgroundTasks;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AddEmployeeController implements Initializable {
    @FXML private TextField txtEmployeeCode;
    @FXML private TextField txtFullName;
    @FXML private DatePicker dpBirthDate;
    @FXML private ComboBox<String> cbGender;
    @FXML private ComboBox<String> cbRole;
    @FXML private TextField txtCitizenId;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;

    @FXML private Label lblErrorName;
    @FXML private Label lblErrorBirthDate;
    @FXML private Label lblErrorCitizenId;
    @FXML private Label lblErrorPhone;
    @FXML private Label lblErrorAddress;

    private Runnable onCloseCallback;
    private Consumer<Employee> onSaveSuccessCallback;
    private final EmployeeService employeeService = new EmployeeServiceImpl();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNextEmployeeCode();

        cbGender.setItems(FXCollections.observableArrayList(
                Employee.GENDER_MALE,
                Employee.GENDER_FEMALE,
                Employee.GENDER_OTHER
        ));
        cbGender.setValue(Employee.GENDER_MALE);

        cbRole.setItems(FXCollections.observableArrayList("Nhân viên", "Quản lý"));
        cbRole.setValue("Nhân viên");

        com.pharmacy.common.FormatUtils.setupDatePicker(dpBirthDate);

        txtFullName.textProperty().addListener((obs, o, n) -> clearError(txtFullName, lblErrorName));
        dpBirthDate.valueProperty().addListener((obs, o, n) -> clearError(dpBirthDate, lblErrorBirthDate));
        txtCitizenId.textProperty().addListener((obs, o, n) -> clearError(txtCitizenId, lblErrorCitizenId));
        txtPhone.textProperty().addListener((obs, o, n) -> clearError(txtPhone, lblErrorPhone));
        txtAddress.textProperty().addListener((obs, o, n) -> clearError(txtAddress, lblErrorAddress));
    }

    public void loadNextEmployeeCode() {
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return employeeService.generateNextEmployeeCode();
            }
        };
        task.setOnSucceeded(e -> {
            if (txtEmployeeCode != null) {
                txtEmployeeCode.setText(task.getValue());
            }
        });
        BackgroundTasks.run(task);
    }

    public void setOnCloseCallback(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    public void setOnSaveSuccessCallback(Consumer<Employee> onSaveSuccessCallback) {
        this.onSaveSuccessCallback = onSaveSuccessCallback;
    }

    @FXML
    private void handleCancel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    @FXML
    private void handleSave() {
        String name = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        LocalDate birthDate = dpBirthDate.getValue();
        String gender = cbGender.getValue();
        String role = cbRole.getValue();
        String citizenId = txtCitizenId.getText() != null ? txtCitizenId.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String address = txtAddress.getText() != null ? txtAddress.getText().trim() : "";

        boolean isValid = true;

        if (name.isEmpty()) {
            showError(txtFullName, lblErrorName, "Họ và tên không được để trống");
            isValid = false;
        } else if (name.length() < 2) {
            showError(txtFullName, lblErrorName, "Họ và tên tối thiểu 2 ký tự");
            isValid = false;
        }

        if (birthDate == null) {
            showError(dpBirthDate, lblErrorBirthDate, "Vui lòng chọn ngày sinh");
            isValid = false;
        } else if (birthDate.isAfter(LocalDate.now().minusYears(18))) {
            showError(dpBirthDate, lblErrorBirthDate, "Nhân viên phải từ đủ 18 tuổi trở lên");
            isValid = false;
        }

        if (citizenId.isEmpty()) {
            showError(txtCitizenId, lblErrorCitizenId, "Vui lòng nhập căn cước công dân");
            isValid = false;
        } else if (!com.pharmacy.common.FormValidator.isValidNationalId(citizenId)) {
            showError(txtCitizenId, lblErrorCitizenId, "CCCD phải đúng 12 chữ số");
            isValid = false;
        }

        if (phone.isEmpty()) {
            showError(txtPhone, lblErrorPhone, "Số điện thoại không được để trống");
            isValid = false;
        } else if (!com.pharmacy.common.FormValidator.isValidPhone(phone)) {
            showError(txtPhone, lblErrorPhone, "SĐT không hợp lệ (10 số, ví dụ: 0912345678)");
            isValid = false;
        }

        if (address.isEmpty()) {
            showError(txtAddress, lblErrorAddress, "Địa chỉ không được để trống");
            isValid = false;
        }

        if (!isValid) return;

        Employee newEmp = new Employee();
        // Không gửi mã đang hiển thị: mã thật được cấp khi lưu để nhiều quản lý thêm cùng lúc không trùng nhau
        newEmp.setFullName(name);
        newEmp.setDob(birthDate);
        newEmp.setGender(gender);
        newEmp.setNationalId(citizenId);
        newEmp.setPhone(phone);
        newEmp.setAddress(address);

        newEmp.setHiredDate(LocalDate.now());
        newEmp.setStatus(Employee.Status.ACTIVE);

        try {
            var account = employeeService.createEmployeeWithAccount(newEmp, role);

            if (account.isPresent()) {
                NewAccountDialog.showCreated(txtFullName.getScene() != null ? txtFullName.getScene().getWindow() : null,
                        newEmp.getFullName(), account.get());
                if (onSaveSuccessCallback != null) {
                    onSaveSuccessCallback.accept(newEmp);
                }
            } else {
                showError(txtPhone, lblErrorPhone, "Lưu thất bại! Đã có lỗi xảy ra từ hệ thống.");
            }
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();

            if (message.contains("Số điện thoại")) {
                showError(txtPhone, lblErrorPhone, message);
            } else if (message.contains("Căn cước") || message.contains("CCCD")) {
                showError(txtCitizenId, lblErrorCitizenId, message);
            } else {
                showError(txtFullName, lblErrorName, message);
            }
        }
    }

    private void showError(Control input, Label errLabel, String msg) {
        if (!input.getStyleClass().contains("form-input-error")) {
            input.getStyleClass().add("form-input-error");
        }
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearError(Control input, Label errLabel) {
        input.getStyleClass().remove("form-input-error");
        errLabel.setText("");
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    public void clearForm() {
        loadNextEmployeeCode();
        txtFullName.clear();
        dpBirthDate.setValue(null);
        cbGender.setValue(Employee.GENDER_MALE);
        cbRole.setValue("Nhân viên");
        txtCitizenId.clear();
        txtPhone.clear();
        txtAddress.clear();

        clearError(txtFullName, lblErrorName);
        clearError(dpBirthDate, lblErrorBirthDate);
        clearError(txtCitizenId, lblErrorCitizenId);
        clearError(txtPhone, lblErrorPhone);
        clearError(txtAddress, lblErrorAddress);
    }
}
