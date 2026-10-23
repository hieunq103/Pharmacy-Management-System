package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.common.PageResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pharmacy.common.UserSession;
import com.pharmacy.common.AppConstants;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.DrawerHelper;
import com.pharmacy.common.TableFilterHelper;
import com.pharmacy.common.TableSyncHelper;

public class EmployeeController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    @FXML private Label lblTotalEmployees;
    @FXML private Label lblActiveEmployees;
    @FXML private TextField txtSearch;

    @FXML private TableView<Employee> tableEmployee;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colGender;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colStatus;
    @FXML private TableColumn<Employee, String> colAccountStatus;
    @FXML private TableColumn<Employee, Void> colAction;
    @FXML private TableColumn<Employee, String> colCccd;
    @FXML private TableView<Employee> tableActionSticky;

    private final DrawerHelper drawerHelper = new DrawerHelper();
    private Parent drawerRoot;
    private AddEmployeeController addController;

    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;

    private final ObservableList<Employee> pageData = FXCollections.observableArrayList();

    private int pageSize = AppConstants.DEFAULT_PAGE_SIZE;
    private int currentPage = 1;
    private int totalPages = 1;
    private long totalRecords = 0;
    private Task<PageResult<Employee>> currentLoadTask;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "emp-load-worker");
        t.setDaemon(true);
        return t;
    });

    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private final Map<String, String> activeFilters = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        // Tự động điều chỉnh chiều cao 7 dòng lấp đầy 100% bảng khi phóng to màn hình (không còn khoảng trắng)
        TableSyncHelper.enableAutoRowHeight(tableEmployee, tableActionSticky, AppConstants.DEFAULT_PAGE_SIZE);

        // Thiết lập đồng bộ cuộn 2 chiều tuyệt đối: cuộn dọc mượt mà, không bao giờ xê dịch lệch hàng
        TableSyncHelper.setupDualTableSync(tableEmployee, tableActionSticky);

        Platform.runLater(() -> {
            Node nestedHeader = tableEmployee.lookup(".nested-column-header");
            if (nestedHeader != null) {
                tableEmployee.getItems().addListener((javafx.collections.ListChangeListener<Employee>) c -> {
                    nestedHeader.setTranslateX(0.0);
                });
            }
        });

        if (txtSearch != null) {
            com.pharmacy.common.DebounceUtil.debounce(txtSearch, AppConstants.SEARCH_DEBOUNCE, text -> {
                currentPage = 1;
                loadEmployeeData();
            });
        }

        TableFilterHelper.setupColumnFilter(colGender, "Giới tính", new String[]{"Tất cả", "Nam", "Nữ", "Khác"}, "gender", activeFilters, () -> {
            currentPage = 1;
            loadEmployeeData();
        });
        TableFilterHelper.setupColumnFilter(colRole, "Vai trò", new String[]{"Tất cả", "Nhân viên", "Quản lý"}, "role", activeFilters, () -> {
            currentPage = 1;
            loadEmployeeData();
        });
        TableFilterHelper.setupColumnFilter(colStatus, "Nhân sự", new String[]{"Tất cả", "Đang làm", "Đã nghỉ"}, "status", activeFilters, () -> {
            currentPage = 1;
            loadEmployeeData();
        });
        TableFilterHelper.setupColumnFilter(colAccountStatus, "Tài khoản", new String[]{"Tất cả", "Active", "Disabled"}, "statusAcount", activeFilters, () -> {
            currentPage = 1;
            loadEmployeeData();
        });

        tableEmployee.setItems(pageData);

        // Nhấp đúp chuột (double-click) vào hàng để mở nhanh trang Thông tin chi tiết
        tableEmployee.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEmployeeDetail(row.getItem());
                }
            });
            return row;
        });

        if (tableActionSticky != null) {
            tableActionSticky.setRowFactory(tv -> {
                TableRow<Employee> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        showEmployeeDetail(row.getItem());
                    }
                });
                return row;
            });
        }

        updateSummaryCounts();
        loadEmployeeData();
    }

    public void loadEmployeeData() {
        String keyword = (txtSearch != null && txtSearch.getText() != null)
                ? txtSearch.getText().trim()
                : "";
        String gender = activeFilters.get("gender");
        String role = activeFilters.get("role");
        String status = activeFilters.get("status");
        String accountStatus = activeFilters.get("statusAcount");

        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        currentLoadTask = new Task<>() {
            @Override
            protected PageResult<Employee> call() {
                return employeeService.getEmployeesPage(currentPage, pageSize, keyword, gender, role, status, accountStatus);
            }
        };

        currentLoadTask.setOnSucceeded(e -> {
            PageResult<Employee> result = currentLoadTask.getValue();
            if (result != null) {
                totalRecords = result.totalElements();
                totalPages = result.totalPages();
                currentPage = result.currentPage();
                pageData.setAll(result.content());

                // Cập nhật thông tin phạm vi bản ghi
                if (lblRecordRange != null) {
                    if (totalRecords == 0) {
                        lblRecordRange.setText("0 - 0");
                    } else {
                        int fromIndex = (currentPage - 1) * pageSize + 1;
                        int toIndex = (int) Math.min(fromIndex + pageSize - 1, totalRecords);
                        lblRecordRange.setText(String.format("%d - %d", fromIndex, toIndex));
                    }
                }

                renderPagination();
            }
        });

        currentLoadTask.setOnFailed(e -> {
            Throwable ex = currentLoadTask.getException();
            log.error("Lỗi nạp dữ liệu nhân viên: ", ex);
        });

        loadExecutor.submit(currentLoadTask);
    }

    private void renderPagination() {
        com.pharmacy.common.PaginationHelper.render(
                paginationBox,
                null,
                currentPage,
                totalPages,
                totalRecords,
                pageSize,
                page -> {
                    currentPage = page;
                    TableSyncHelper.resetScrollToTop(tableEmployee, tableActionSticky);
                    loadEmployeeData();
                }
        );
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName() != null ? c.getValue().getFullName() : ""));
        colGender.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGender() != null ? c.getValue().getGender() : ""));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone() != null ? c.getValue().getPhone() : ""));
        colCccd.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNationalId() != null ? c.getValue().getNationalId() : "")
        );

        colRole.setCellFactory(col -> new TableCell<Employee, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Employee emp = getTableRow().getItem();
                Account acc = emp.getAccount();
                boolean isManager = (acc != null && acc.isManager());

                HBox badge = new HBox(8);
                badge.setAlignment(Pos.CENTER_LEFT);
                badge.getStyleClass().add("pill-badge");
                badge.setMouseTransparent(true);

                StackPane haloDot;
                Label lbl = new Label(isManager ? "Quản lý" : "Nhân viên");
                lbl.getStyleClass().add("pill-text");
                lbl.setStyle("-fx-text-fill: #1E293B;");

                if (isManager) {
                    haloDot = createStatusDot(Color.web("#EF4444"), Color.web("#FEE2E2"));
                    badge.getStyleClass().add("pill-role-manager");
                } else {
                    haloDot = createStatusDot(Color.web("#6366F1"), Color.web("#E0E7FF"));
                    badge.getStyleClass().add("pill-role-staff");
                }

                badge.getChildren().addAll(haloDot, lbl);
                setText(null);
                setGraphic(badge);
            }
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Employee emp = getTableRow().getItem();
                boolean isActive = emp.getStatus() == Employee.Status.ACTIVE;

                if (isActive) {
                    setText("Đang làm");
                } else {
                    setText("Đã nghỉ");
                }

                setGraphic(null);
            }
        });

        colAccountStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Employee emp = getTableRow().getItem();
                Account acc = emp.getAccount();

                if (acc == null || acc.getStatus() == null) {
                    Label lblNoAccount = new Label("Chưa có TK");
                    lblNoAccount.getStyleClass().add("status-badge-none");
                    lblNoAccount.setMouseTransparent(true);
                    setText(null);
                    setGraphic(lblNoAccount);
                    return;
                }

                boolean isActive = acc.getStatus() == Account.Status.ACTIVE;

                HBox badge = new HBox(6);
                badge.setAlignment(Pos.CENTER_LEFT);
                badge.getStyleClass().add("status-badge");
                badge.setMouseTransparent(true);

                Circle dot = new Circle(4);
                Label lbl = new Label(isActive ? "Active" : "Disabled");
                lbl.getStyleClass().add("pill-text");

                if (isActive) {
                    badge.getStyleClass().add("status-badge-active");
                    dot.setFill(Color.web("#16A34A"));
                } else {
                    badge.getStyleClass().add("status-badge-inactive");
                    dot.setFill(Color.web("#D9483B"));
                }

                badge.getChildren().addAll(dot, lbl);
                setText(null);
                setGraphic(badge);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnMore = new Button("⋮");

            {
                btnMore.getStyleClass().add("btn-action-more");
                btnMore.setOnAction(e -> {
                    Employee emp = getTableRow() != null ? getTableRow().getItem() : null;
                    if (emp != null) {
                        handleActionClick(emp, btnMore);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Employee emp = getTableRow().getItem();
                Integer currentLoggedInId = null;
                if (UserSession.isLoggedIn()) {
                    currentLoggedInId = UserSession.getInstance().getEmployeeId();
                }
                if (currentLoggedInId != null && currentLoggedInId.equals(emp.getId())) {
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setGraphic(btnMore);
                }
            }
        });
    }

    public void updateSummaryCounts() {
        Task<EmployeeRepository.EmployeeSummary> task = new Task<>() {
            @Override
            protected EmployeeRepository.EmployeeSummary call() {
                return employeeService.getEmployeeSummary();
            }
        };

        task.setOnSucceeded(e -> {
            EmployeeRepository.EmployeeSummary summary = task.getValue();
            if (summary != null) {
                if (lblTotalEmployees != null) lblTotalEmployees.setText(String.valueOf(summary.total()));
                if (lblActiveEmployees != null) lblActiveEmployees.setText(String.valueOf(summary.active()));
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("Lỗi nạp thống kê nhân viên: ", ex);
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private StackPane createStatusDot(Color centerColor, Color haloColor) {
        Circle outerRing = new Circle(7.5);
        outerRing.setFill(haloColor);

        Circle innerDot = new Circle(3.5);
        innerDot.setFill(centerColor);

        StackPane dotContainer = new StackPane(outerRing, innerDot);
        dotContainer.setAlignment(Pos.CENTER);
        dotContainer.setMinSize(15, 15);
        dotContainer.setPrefSize(15, 15);
        dotContainer.setMaxSize(15, 15);
        return dotContainer;
    }

    @FXML
    private void handleOpenAddDialog() {
        showGlobalDrawer();
    }

    private ScrollBar cachedEmployeeHBar = null;

    private ScrollBar getEmployeeHorizontalScrollBar() {
        if (cachedEmployeeHBar == null || cachedEmployeeHBar.getScene() == null) {
            for (Node n : tableEmployee.lookupAll(".scroll-bar")) {
                if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL) {
                    cachedEmployeeHBar = sb;
                    break;
                }
            }
        }
        return cachedEmployeeHBar;
    }


    private void handleActionClick(Employee emp, Button btnTarget) {
        if (emp == null) return;

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("filter-context-menu");

        Account acc = emp.getAccount();
        boolean isAccountDisabled = (acc != null && acc.getStatus() == Account.Status.DISABLED);
        boolean isEmpInactive = (emp.getStatus() == Employee.Status.INACTIVE);

        String toggleLabel = isAccountDisabled ? "Mở khóa tài khoản" : "Khóa tài khoản";
        MenuItem itemToggleAcc = new MenuItem(toggleLabel);

        itemToggleAcc.setDisable(isEmpInactive);

        // =========================================================================
        // 1. CHỨC NĂNG KHÓA / MỞ KHÓA TÀI KHOẢN
        // =========================================================================
        itemToggleAcc.setOnAction(e -> {
            if (acc == null || acc.getId() == null) {
                showSimpleAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin tài khoản.");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            DialogUtils.configure(confirmAlert, tableEmployee != null && tableEmployee.getScene() != null ? tableEmployee.getScene().getWindow() : null);
            confirmAlert.setTitle("Xác nhận");
            confirmAlert.setHeaderText("Bạn có chắc chắn muốn " + toggleLabel.toLowerCase() + "?");
            confirmAlert.setContentText(isAccountDisabled
                    ? "Tài khoản sẽ được kích hoạt lại và có thể đăng nhập bình thường."
                    : "Tài khoản sẽ bị vô hiệu hóa và không thể đăng nhập vào hệ thống.");

            ButtonType btnOk = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(btnOk, btnCancel);

            confirmAlert.showAndWait().ifPresent(res -> {
                if (res == btnOk) {
                    final Integer targetAccountId = acc.getId();
                    Task<Boolean> task = new Task<>() {
                        @Override
                        protected Boolean call() {
                            if (isAccountDisabled) {
                                return employeeService.unlockAccount(targetAccountId);
                            } else {
                                return employeeService.lockAccount(targetAccountId);
                            }
                        }
                    };

                    task.setOnSucceeded(ev -> {
                        boolean success = task.getValue();
                        if (success) {
                            showSimpleAlert(Alert.AlertType.INFORMATION, "Thông báo", toggleLabel + " thành công!");

                            // ----- CÁCH 1: CẬP NHẬT TRỰC TIẾP TRÊN RAM VÀ REFRESH BẢNG -----
                            if (isAccountDisabled) {
                                acc.setStatus(Account.Status.ACTIVE);
                            } else {
                                acc.setStatus(Account.Status.DISABLED);
                            }

                            tableEmployee.refresh();
                            if (tableActionSticky != null) tableActionSticky.refresh();
                            // ---------------------------------------------------------------

                        } else {
                            showSimpleAlert(Alert.AlertType.ERROR, "Thông báo", toggleLabel + " thất bại.");
                        }
                    });

                    task.setOnFailed(ev -> {
                        Throwable ex = task.getException();
                        showSimpleAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra.");
                    });

                    new Thread(task).start();
                }
            });
        });

        // =========================================================================
        // 2. CHỨC NĂNG ĐÁNH DẤU ĐÃ NGHỈ
        // =========================================================================
        MenuItem itemMarkInactive = new MenuItem("Đánh dấu đã nghỉ");
        itemMarkInactive.setDisable(isEmpInactive);
        itemMarkInactive.setOnAction(e -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Nhân viên " + emp.getFullName() + " sẽ chuyển sang trạng thái ĐÃ NGHỈ và tài khoản sẽ bị KHÓA ngay lập tức.",
                    ButtonType.YES,
                    ButtonType.NO
            );
            DialogUtils.configure(confirm, tableEmployee != null && tableEmployee.getScene() != null ? tableEmployee.getScene().getWindow() : null);
            confirm.setTitle("Xác nhận đánh dấu nghỉ");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    boolean success = employeeService.markAsInactive(emp.getId());
                    if (success) {

                        // ----- CÁCH 1: CẬP NHẬT TRỰC TIẾP TRÊN RAM VÀ REFRESH BẢNG -----
                        emp.setStatus(Employee.Status.INACTIVE);
                        if (emp.getAccount() != null) {
                            emp.getAccount().setStatus(Account.Status.DISABLED);
                        }

                        tableEmployee.refresh();
                        if (tableActionSticky != null) tableActionSticky.refresh();

                        // Cập nhật lại luôn số đếm tổng/đang làm trên Header
                        updateSummaryCounts();
                        // ---------------------------------------------------------------

                    } else {
                        showSimpleAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái nhân viên.");
                    }
                }
            });
        });

        // =========================================================================
        // 3. CHỨC NĂNG XEM CHI TIẾT
        // =========================================================================
        MenuItem itemEditDetail = new MenuItem("Thông tin chi tiết");
        itemEditDetail.setOnAction(e -> showEmployeeDetail(emp));

        contextMenu.getItems().addAll(
                itemToggleAcc,
                itemMarkInactive,
                new SeparatorMenuItem(),
                itemEditDetail
        );

        contextMenu.show(btnTarget, Side.BOTTOM, 0, 0);
    }

    private void showEmployeeDetail(Employee emp) {
        if (emp == null || emp.getId() == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn nhân viên cần xem chi tiết.");
            return;
        }

        Optional<Employee> empDetailOpt = employeeService.findByIdWithDetails(emp.getId());
        if (empDetailOpt.isEmpty()) {
            showSimpleAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy dữ liệu nhân viên.");
            return;
        }
        Employee fullEmployee = empDetailOpt.get();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/employee/detail_employee.fxml"));
            Parent detailView = loader.load();

            EmployeeDetailController detailController = loader.getController();

            Scene scene = tableEmployee != null ? tableEmployee.getScene() : null;
            if (scene == null) return;

            StackPane contentArea = (StackPane) scene.lookup("#contentArea");

            if (contentArea != null) {
                Node currentListView = contentArea.getChildren().getFirst();

                detailController.setEmployeeData(fullEmployee, () -> {
                    contentArea.getChildren().setAll(currentListView);
                    updateSummaryCounts();
                    loadEmployeeData();
                });

                contentArea.getChildren().setAll(detailView);
            }

        } catch (Exception ex) {
            log.error("Lỗi khi mở màn hình chi tiết nhân viên: ", ex);
            showSimpleAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang chi tiết: " + ex.getMessage());
        }
    }

    private void showSimpleAlert(Alert.AlertType type, String title, String content) {
        javafx.stage.Window owner = tableEmployee != null && tableEmployee.getScene() != null
                ? tableEmployee.getScene().getWindow()
                : com.pharmacy.common.DialogUtils.getActiveWindow();
        if (type == Alert.AlertType.ERROR) {
            com.pharmacy.common.DialogUtils.showError(owner, title, content);
        } else if (type == Alert.AlertType.WARNING) {
            com.pharmacy.common.DialogUtils.showWarning(owner, title, content);
        } else {
            com.pharmacy.common.DialogUtils.showInfo(owner, title, content);
        }
    }

    private void showGlobalDrawer() {
        if (tableEmployee.getScene() == null) return;
        drawerHelper.init(tableEmployee.getScene(), null);

        if (drawerRoot == null) {
            try {
                URL fxmlUrl = getClass().getResource("/fxml/employee/add_employee.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                drawerRoot = loader.load();
                addController = loader.getController();

                addController.setOnCloseCallback(drawerHelper::close);
                addController.setOnSaveSuccessCallback(newEmp -> {
                    drawerHelper.close();
                    updateSummaryCounts();
                    loadEmployeeData();
                });

                drawerHelper.registerDrawer(drawerRoot);
            } catch (IOException e) {
                log.error("Lỗi khi mở drawer thêm nhân viên: ", e);
                return;
            }
        }

        if (addController != null) {
            addController.clearForm();
        }
        drawerHelper.open(drawerRoot);
    }
}