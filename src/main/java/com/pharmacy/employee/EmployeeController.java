package com.pharmacy.employee;

import com.pharmacy.account.Account;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import com.pharmacy.common.UserSession;

public class EmployeeController implements Initializable {

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

    private Region overlayRegion;
    private Parent drawerRoot;
    private AddEmployeeController addController;
    private StackPane appRootPane;

    @FXML private ComboBox<String> cbPageSize;
    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;

    private final ObservableList<Employee> masterData = FXCollections.observableArrayList();
    private final ObservableList<Employee> pageData = FXCollections.observableArrayList();

    private int pageSize = 7;
    private int currentPage = 1;
    private int totalPages = 1;

    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private FilteredList<Employee> filteredData;
    private SortedList<Employee> sortedData;
    private final Map<String, String> activeFilters = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        if (tableActionSticky != null) {
            tableActionSticky.addEventFilter(ScrollEvent.SCROLL, Event::consume);
            tableActionSticky.setFocusTraversable(false);
            tableActionSticky.setSelectionModel(null);

            tableActionSticky.itemsProperty().bind(tableEmployee.itemsProperty());
        }

// 2. Khóa giá trị của thanh cuộn dọc không cho trượt lệch (giữ nguyên)
        tableEmployee.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    ScrollBar vBarLeft = (ScrollBar) tableEmployee.lookup(".scroll-bar:vertical");
                    ScrollBar vBarRight = tableActionSticky != null ? (ScrollBar) tableActionSticky.lookup(".scroll-bar:vertical") : null;

                    if (vBarLeft != null) {
                        vBarLeft.valueProperty().addListener((o, oldVal, newVal) -> {
                            if (newVal.doubleValue() != 0.0) vBarLeft.setValue(0.0);
                        });
                    }
                    if (vBarRight != null) {
                        vBarRight.valueProperty().addListener((o, oldVal, newVal) -> {
                            if (newVal.doubleValue() != 0.0) vBarRight.setValue(0.0);
                        });
                    }
                });
            }
        });

        Platform.runLater(() -> {
            Node nestedHeader = tableEmployee.lookup(".nested-column-header");

            if (nestedHeader != null) {
                tableEmployee.addEventFilter(ScrollEvent.SCROLL, event -> {
                    // Kích hoạt trượt ngang khi bảng RỖNG và có lực vuốt ngang
                    if (tableEmployee.getItems().isEmpty() && Math.abs(event.getDeltaX()) > 0) {

                        double totalColsWidth = 0;
                        for (TableColumn<?, ?> col : tableEmployee.getVisibleLeafColumns()) {
                            totalColsWidth += col.getWidth();
                        }

                        double viewWidth = tableEmployee.getWidth();

                        if (totalColsWidth > viewWidth) {
                            double minTranslateX = viewWidth - totalColsWidth; // Giới hạn mép trái
                            double maxTranslateX = 0.0;                       // Giới hạn mép phải (gốc)

                            double currentTranslateX = nestedHeader.getTranslateX();
                            double newTranslateX = currentTranslateX + event.getDeltaX(); // deltaX trên Mac chạy rất chuẩn

                            // Ép tọa độ không bao giờ vượt quá 2 mép
                            newTranslateX = Math.max(minTranslateX, Math.min(maxTranslateX, newTranslateX));

                            nestedHeader.setTranslateX(newTranslateX);
                        }
                        event.consume(); // Bắt buộc tiêu thụ sự kiện để trượt mượt tuyệt đối
                    }
                });

                tableEmployee.getItems().addListener((javafx.collections.ListChangeListener<Employee>) c -> {
                    nestedHeader.setTranslateX(0.0);
                });
            }
        });

        // 1. Pipeline: masterData -> filteredData -> sortedData
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableEmployee.comparatorProperty());

        colName.setSortable(true);

        tableEmployee.comparatorProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            renderPagination();
        });

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                reapplyFilters();
            });
        }

        setupColumnFilter(colGender, "Giới tính", new String[]{"Tất cả", "Nam", "Nữ", "Khác"}, "gender");
        setupColumnFilter(colRole, "Vai trò", new String[]{"Tất cả", "Nhân viên", "Quản lý"}, "role");
        setupColumnFilter(colStatus, "Nhân sự", new String[]{"Tất cả", "Đang làm", "Đã nghỉ"}, "status");
        setupColumnFilter(colAccountStatus, "Tài khoản", new String[]{"Tất cả", "Active", "Disabled"}, "statusAcount");

        tableEmployee.setItems(pageData);
        updateSummaryCounts();
        loadEmployeeData();
    }

    private void reapplyFilters() {
        String keyword = (txtSearch != null && txtSearch.getText() != null)
                ? txtSearch.getText().trim().toLowerCase()
                : "";

        filteredData.setPredicate(emp -> {
            if (emp == null) return false;

            // 1. Điều kiện ô tìm kiếm
            boolean matchSearch = true;
            if (!keyword.isEmpty()) {
                boolean matchName = emp.getFullName() != null && emp.getFullName().toLowerCase().contains(keyword);
                boolean matchPhone = emp.getPhone() != null && emp.getPhone().contains(keyword);
                boolean matchCccd = emp.getNationalId() != null && emp.getNationalId().contains(keyword);
                matchSearch = matchName || matchPhone || matchCccd;
            }

            if (!matchSearch) return false;

            // 2. Bộ lọc cột Giới tính
            boolean matchGender = true;
            if (activeFilters.containsKey("gender")) {
                String filterVal = activeFilters.get("gender");
                matchGender = emp.getGender() != null && emp.getGender().equalsIgnoreCase(filterVal);
            }

            // 3. Bộ lọc cột Vai trò
            boolean matchRole = true;
            if (activeFilters.containsKey("role")) {
                String filterVal = activeFilters.get("role");
                Account acc = emp.getAccount();
                boolean isManager = (acc != null && acc.isManager());
                String roleStr = isManager ? "Quản lý" : "Nhân viên";
                matchRole = roleStr.equalsIgnoreCase(filterVal);
            }

            // 4. Bộ lọc cột Trạng thái nhân sự
            boolean matchStatus = true;
            if (activeFilters.containsKey("status")) {
                String filterVal = activeFilters.get("status");
                if ("Đang làm".equals(filterVal)) {
                    matchStatus = (emp.getStatus() == Employee.Status.ACTIVE);
                } else if ("Đã nghỉ".equals(filterVal)) {
                    matchStatus = (emp.getStatus() == Employee.Status.INACTIVE);
                }
            }

            // 5. Bộ lọc cột Trạng thái tài khoản
            boolean matchAccount = true;
            if (activeFilters.containsKey("statusAcount")) {
                String filterVal = activeFilters.get("statusAcount");
                Account acc = emp.getAccount();
                if (acc == null || acc.getStatus() == null) {
                    matchAccount = false;
                } else {
                    String accStatusStr = acc.getStatus().name();
                    matchAccount = accStatusStr.equalsIgnoreCase(filterVal);
                }
            }

            return matchGender && matchRole && matchStatus && matchAccount;
        });

        updatePaginationAfterFilter();
    }

    private void setupColumnFilter(TableColumn<Employee, ?> column, String title, String[] options, String filterKey) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("label");

        Label iconFilter = new Label("▾");
        iconFilter.getStyleClass().add("btn-header-filter");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-context-menu");

        for (String opt : options) {
            Label lblOpt = new Label(opt);
            lblOpt.setMaxWidth(Double.MAX_VALUE);
            lblOpt.getStyleClass().add("filter-item-label");

            CustomMenuItem item = new CustomMenuItem(lblOpt, true);
            item.getStyleClass().add("custom-filter-menu-item");

            item.setOnAction(e -> {
                if ("Tất cả".equals(opt)) {
                    activeFilters.remove(filterKey);
                } else {
                    activeFilters.put(filterKey, opt);
                }

                if (activeFilters.containsKey(filterKey)) {
                    if (!iconFilter.getStyleClass().contains("filter-active")) {
                        iconFilter.getStyleClass().add("filter-active");
                    }
                } else {
                    iconFilter.getStyleClass().remove("filter-active");
                }

                reapplyFilters();
            });

            menu.getItems().add(item);
        }

        menu.setOnShowing(e -> {
            String currentSelected = activeFilters.getOrDefault(filterKey, "Tất cả");
            for (MenuItem mi : menu.getItems()) {
                if (mi instanceof CustomMenuItem cmi && cmi.getContent() instanceof Label lbl) {
                    lbl.getStyleClass().remove("filter-item-active");
                    if (lbl.getText().equalsIgnoreCase(currentSelected)) {
                        lbl.getStyleClass().add("filter-item-active");
                    }
                }
            }
        });

        iconFilter.setOnMouseClicked(e -> {
            if (!menu.isShowing()) {
                menu.show(iconFilter, Side.BOTTOM, 0, 0);
            } else {
                menu.hide();
            }
        });

        HBox headerBox = new HBox(8, lblTitle, iconFilter);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        column.setText("");
        column.setGraphic(headerBox);
        column.setSortable(false);
    }

    private void updatePaginationAfterFilter() {
        currentPage = 1;
        renderPagination();
    }

    public void loadEmployeeData() {
        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() {
                return employeeService.getAllEmployees();
            }
        };

        task.setOnSucceeded(e -> {
            List<Employee> list = task.getValue();
            setEmployeeData(list);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Lỗi nạp dữ liệu: " + ex.getMessage());
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public void setEmployeeData(List<Employee> list) {
        if (list == null) return;
        masterData.setAll(list);
        updateSummaryCounts();
        currentPage = 1;
        renderPagination();
    }

    private void updateTablePage() {
        int total = sortedData.size();
        if (total == 0) {
            pageData.clear();
            if (lblRecordRange != null) lblRecordRange.setText("0 - 0");
            return;
        }

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        pageData.setAll(sortedData.subList(fromIndex, toIndex));

        if (lblRecordRange != null) {
            lblRecordRange.setText(String.format("%d - %d", (fromIndex + 1), toIndex));
        }
    }

    private void renderPagination() {
        if (paginationBox == null) return;
        paginationBox.getChildren().clear();

        int totalRecords = sortedData != null ? sortedData.size() : masterData.size();
        totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        updateTablePage();

        Button btnPrev = new Button("<");
        btnPrev.getStyleClass().add("page-btn");
        btnPrev.setDisable(currentPage == 1);
        btnPrev.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderPagination();
            }
        });
        paginationBox.getChildren().add(btnPrev);

        if (totalPages <= 7) {
            for (int i = 1; i <= totalPages; i++) addPageButton(i);
        } else {
            if (currentPage <= 4) {
                for (int i = 1; i <= 5; i++) addPageButton(i);
                addEllipsis();
                addPageButton(totalPages);
            } else if (currentPage >= totalPages - 3) {
                addPageButton(1);
                addEllipsis();
                for (int i = totalPages - 4; i <= totalPages; i++) addPageButton(i);
            } else {
                addPageButton(1);
                addEllipsis();
                addPageButton(currentPage - 1);
                addPageButton(currentPage);
                addPageButton(currentPage + 1);
                addEllipsis();
                addPageButton(totalPages);
            }
        }

        Button btnNext = new Button(">");
        btnNext.getStyleClass().add("page-btn");
        btnNext.setDisable(currentPage == totalPages);
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderPagination();
            }
        });
        paginationBox.getChildren().add(btnNext);
    }

    private void addPageButton(int pageIndex) {
        Button btn = new Button(String.valueOf(pageIndex));
        btn.getStyleClass().add("page-btn");
        if (pageIndex == currentPage) {
            btn.getStyleClass().add("page-btn-active");
        }
        btn.setOnAction(e -> {
            currentPage = pageIndex;
            renderPagination();
        });
        paginationBox.getChildren().add(btn);
    }

    private void addEllipsis() {
        Label lbl = new Label("...");
        lbl.getStyleClass().add("page-dots");
        paginationBox.getChildren().add(lbl);
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
                    setText(null);
                    setGraphic(lblNoAccount);
                    return;
                }

                boolean isActive = acc.getStatus() == Account.Status.ACTIVE;

                HBox badge = new HBox(6);
                badge.setAlignment(Pos.CENTER_LEFT);
                badge.getStyleClass().add("status-badge");

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
        long total = masterData.size();
        long active = masterData.stream().filter(e -> e.getStatus() == Employee.Status.ACTIVE).count();
        if (lblTotalEmployees != null) lblTotalEmployees.setText(String.valueOf(total));
        if (lblActiveEmployees != null) lblActiveEmployees.setText(String.valueOf(active));
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
        itemEditDetail.setOnAction(e -> {
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

                Window window = ((MenuItem) e.getSource()).getParentPopup().getOwnerWindow();
                if (window == null && window.getScene() == null) return;

                StackPane contentArea = (StackPane) window.getScene().lookup("#contentArea");

                if (contentArea != null) {
                    Node currentListView = contentArea.getChildren().getFirst();

                    detailController.setEmployeeData(fullEmployee, () -> {
                        contentArea.getChildren().setAll(currentListView);
                        // Ở màn hình chi tiết quay lại thì có thể gọi loadEmployeeData()
                        // hoặc lưu trữ context để không bị mất trang.
                        loadEmployeeData();
                    });

                    contentArea.getChildren().setAll(detailView);
                }

            } catch (Exception ex) {
                System.err.println("Lỗi khi mở màn hình chi tiết nhân viên: " + ex.getMessage());
                ex.printStackTrace();
                showSimpleAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang chi tiết: " + ex.getMessage());
            }
        });

        contextMenu.getItems().addAll(
                itemToggleAcc,
                itemMarkInactive,
                new SeparatorMenuItem(),
                itemEditDetail
        );

        contextMenu.show(btnTarget, Side.BOTTOM, 0, 0);
    }

    private void showSimpleAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showGlobalDrawer() {
        if (tableEmployee.getScene() == null) return;

        javafx.scene.Scene scene = tableEmployee.getScene();

        if (appRootPane == null) {
            Parent currentRoot = scene.getRoot();

            if (currentRoot instanceof StackPane) {
                appRootPane = (StackPane) currentRoot;
            } else {
                appRootPane = new StackPane();
                scene.setRoot(appRootPane);
                appRootPane.getChildren().add(currentRoot);
            }

            overlayRegion = new Region();
            overlayRegion.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45); -fx-cursor: hand;");
            overlayRegion.prefWidthProperty().bind(scene.widthProperty());
            overlayRegion.prefHeightProperty().bind(scene.heightProperty());
            overlayRegion.setVisible(false);
            overlayRegion.setOpacity(0.0);
            overlayRegion.setOnMouseClicked(e -> closeGlobalDrawer());

            try {
                URL fxmlUrl = getClass().getResource("/fxml/employee/add_employee.fxml");
                if (fxmlUrl == null) {
                    fxmlUrl = getClass().getResource("add_employee.fxml");
                }
                if (fxmlUrl == null) {
                    System.err.println("Không tìm thấy file add_employee.fxml");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                drawerRoot = loader.load();
                addController = loader.getController();

                addController.setOnCloseCallback(this::closeGlobalDrawer);
                addController.setOnSaveSuccessCallback(newEmp -> {
                    closeGlobalDrawer();
                    loadEmployeeData();
                });

                drawerRoot.setVisible(false);
                StackPane.setAlignment(drawerRoot, Pos.CENTER_RIGHT);

                appRootPane.getChildren().addAll(overlayRegion, drawerRoot);

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (drawerRoot == null) return;

        if (addController != null) {
            addController.clearForm();
        }

        overlayRegion.setVisible(true);
        drawerRoot.setVisible(true);

        double drawerWidth = 460;
        drawerRoot.setTranslateX(drawerWidth);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), overlayRegion);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), drawerRoot);
        slideIn.setFromX(drawerWidth);
        slideIn.setToX(0);

        new ParallelTransition(fadeIn, slideIn).play();
    }

    private void closeGlobalDrawer() {
        if (overlayRegion == null || !overlayRegion.isVisible()) return;

        double drawerWidth = 460;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlayRegion);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), drawerRoot);
        slideOut.setFromX(0);
        slideOut.setToX(drawerWidth);

        ParallelTransition anim = new ParallelTransition(fadeOut, slideOut);
        anim.setOnFinished(e -> {
            overlayRegion.setVisible(false);
            drawerRoot.setVisible(false);
        });
        anim.play();
    }
}