package com.pharmacy.sales;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.DebounceUtil;
import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.PaginationHelper;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.CartOrder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);

    // Màn hình chính và Lịch sử bán hàng
    @FXML private HBox viewSalesMain;
    private static final double LINE_TOTAL_WIDTH = 86;
    @FXML private StackPane viewSalesHistory;
    /** Hành động của nút back trong Lịch sử bán hàng; null = quay lại trang Bán hàng. */
    private Runnable historyBackAction;

    private Parent historyRootNode;
    private SalesHistoryController historyController;

    // Cột trái: Tìm kiếm, nhóm thuốc, 6 thẻ thuốc và phân trang
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cboCategory;
    @FXML private GridPane gridMedicineCards;
    @FXML private VBox paneEmptyMedicines;
    @FXML private HBox paginationBox;
    @FXML private Label lblRecordRange;

    // Cột phải: Giỏ hàng
    @FXML private VBox paneEmptyCart;
    @FXML private ScrollPane scrollCartItems;
    @FXML private VBox boxCartItems;
    @FXML private HBox bannerPrescriptionNotice;

    @FXML private Label lblExpandIcon;
    @FXML private Label lblExpandTitle;
    @FXML private VBox paneExtraFields;
    @FXML private TextField txtCustomerPhone;
    @FXML private TextField txtDiscount;
    @FXML private Button btnDiscountVnd;
    @FXML private Button btnDiscountPercent;
    private boolean isDiscountPercent = false;
    private boolean isAdjustingDiscount = false;

    @FXML private Label lblCartSubtotal;
    @FXML private Label lblCartDiscount;
    @FXML private Label lblCartTotal;
    @FXML private Button btnCheckout;

    private final SalesService salesService = new SalesServiceImpl();
    private CartOrder activeOrder = new CartOrder(1, "Hóa đơn");

    // Cấu hình phân trang: 6 thuốc mỗi trang (2 cột x 3 hàng)
    private static final int PAGE_SIZE = 6;
    private int currentPage = 1;
    private int totalPages = 1;
    private long totalRecords = 0;
    private String currentKeyword = "";
    private String currentCategory = "Tất cả nhóm thuốc";

    private List<MedicineInventoryDTO> allLoadedMedicines = new ArrayList<>();

    private final ExecutorService salesExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pos-sales-worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (btnDiscountPercent != null) {
            btnDiscountPercent.setText("%");
        }
        activeOrder = new CartOrder(1, "Hóa đơn");
        setupSearchAndShortcuts();
        setupExtraFieldsListeners();
        setupCategoryComboBox();
        loadMedicinesData();
    }

    private void setupSearchAndShortcuts() {
        if (txtSearch != null) {
            DebounceUtil.debounce(txtSearch, AppConstants.SEARCH_DEBOUNCE, text -> {
                currentKeyword = text != null ? text.trim() : "";
                currentPage = 1;
                loadMedicinesData();
            });

            txtSearch.setOnAction(e -> {
                if (!allLoadedMedicines.isEmpty()) {
                    // Nếu ấn Enter mà có thuốc đầu tiên còn hàng -> thêm ngay vào giỏ
                    for (MedicineInventoryDTO m : allLoadedMedicines) {
                        if (m.getTotalQuantity() > 0) {
                            addMedicineToCart(m);
                            txtSearch.clear();
                            currentKeyword = "";
                            break;
                        }
                    }
                }
            });
        }

        // Bắt phím tắt F2 (focus tìm kiếm) và F9 (thanh toán)
        Platform.runLater(() -> {
            if (txtSearch != null && txtSearch.getScene() != null) {
                txtSearch.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F2) {
                        txtSearch.requestFocus();
                        txtSearch.selectAll();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F9) {
                        if (btnCheckout != null && !btnCheckout.isDisable()) {
                            handleOpenPaymentDialog();
                            event.consume();
                        }
                    }
                });
            }
        });
    }

    private void setupExtraFieldsListeners() {
        if (txtCustomerPhone != null) {
            txtCustomerPhone.textProperty().addListener((obs, oldV, newV) -> {
                if (activeOrder != null) {
                    activeOrder.setCustomerPhone(newV != null ? newV.trim() : "");
                }
            });
        }

        if (txtDiscount != null) {
            txtDiscount.textProperty().addListener((obs, oldV, newV) -> {
                recalculateDiscount();
            });
        }
    }

    @FXML
    private void handleDiscountVndMode() {
        if (!isDiscountPercent) return;
        isDiscountPercent = false;
        if (btnDiscountVnd != null && !btnDiscountVnd.getStyleClass().contains("active")) {
            btnDiscountVnd.getStyleClass().add("active");
        }
        if (btnDiscountPercent != null) {
            while (btnDiscountPercent.getStyleClass().remove("active")) {}
        }
        recalculateDiscount();
    }

    @FXML
    private void handleDiscountPercentMode() {
        if (isDiscountPercent) return;
        isDiscountPercent = true;
        if (btnDiscountPercent != null && !btnDiscountPercent.getStyleClass().contains("active")) {
            btnDiscountPercent.getStyleClass().add("active");
        }
        if (btnDiscountVnd != null) {
            while (btnDiscountVnd.getStyleClass().remove("active")) {}
        }
        recalculateDiscount();
    }

    private void recalculateDiscount() {
        if (activeOrder == null || isAdjustingDiscount) return;
        BigDecimal subtotal = activeOrder.getSubtotal();
        String val = (txtDiscount != null && txtDiscount.getText() != null) ? txtDiscount.getText().trim() : "";
        if (val.isEmpty()) {
            activeOrder.setDiscount(BigDecimal.ZERO);
        } else {
            String digits = val.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                activeOrder.setDiscount(BigDecimal.ZERO);
            } else {
                try {
                    BigDecimal num = new BigDecimal(digits);
                    if (isDiscountPercent) {
                        // Nếu là %, tối đa 100%
                        if (num.compareTo(BigDecimal.valueOf(100)) > 0) {
                            num = BigDecimal.valueOf(100);
                            final String corrected = "100";
                            isAdjustingDiscount = true;
                            if (txtDiscount != null) {
                                txtDiscount.setText(corrected);
                                txtDiscount.positionCaret(corrected.length());
                            }
                            isAdjustingDiscount = false;
                        }
                        BigDecimal calculatedDisc = subtotal.multiply(num)
                                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                        if (calculatedDisc.compareTo(subtotal) > 0) {
                            calculatedDisc = subtotal;
                        }
                        activeOrder.setDiscount(calculatedDisc);
                    } else {
                        // Nếu là tiền VND:
                        // QUY TẮC BẮT BUỘC: Không được giảm giá nhiều hơn số tiền phải thanh toán (subtotal)
                        if (num.compareTo(subtotal) > 0) {
                            num = subtotal;
                            final String corrected = subtotal.compareTo(BigDecimal.ZERO) > 0 ? subtotal.toPlainString() : "0";
                            isAdjustingDiscount = true;
                            if (txtDiscount != null) {
                                txtDiscount.setText(corrected);
                                txtDiscount.positionCaret(corrected.length());
                            }
                            isAdjustingDiscount = false;
                        }
                        activeOrder.setDiscount(num);
                    }
                } catch (Exception ex) {
                    activeOrder.setDiscount(BigDecimal.ZERO);
                }
            }
        }
        updateCartSummary();
    }

    /**
     * Nạp danh mục thuốc vào ComboBox chọn nhóm thuốc
     */
    private void setupCategoryComboBox() {
        if (cboCategory == null) return;
        cboCategory.getItems().clear();
        cboCategory.getItems().add("Tất cả nhóm thuốc");
        cboCategory.setValue("Tất cả nhóm thuốc");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return salesService.getCategories();
            }
        };

        task.setOnSucceeded(e -> {
            List<String> categories = task.getValue();
            if (categories != null) {
                cboCategory.getItems().addAll(categories);
            }
        });

        salesExecutor.submit(task);

        cboCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentCategory = newVal != null ? newVal : "Tất cả nhóm thuốc";
            currentPage = 1;
            loadMedicinesData();
        });
    }

    /**
     * Nạp trang thuốc (mỗi trang đúng 6 thuốc) theo từ khóa và nhóm thuốc
     */
    public void loadMedicinesData() {
        int pageToLoad = currentPage;
        String kw = currentKeyword;
        String cat = currentCategory;

        Task<PageResult<MedicineInventoryDTO>> task = new Task<>() {
            @Override
            protected PageResult<MedicineInventoryDTO> call() {
                return salesService.getMedicinesPage(pageToLoad, PAGE_SIZE, kw, cat);
            }
        };

        task.setOnSucceeded(e -> {
            PageResult<MedicineInventoryDTO> result = task.getValue();
            if (result != null) {
                allLoadedMedicines = result.getItems();
                currentPage = result.getCurrentPage();
                totalPages = result.getTotalPages();
                totalRecords = result.getTotalElements();
            } else {
                allLoadedMedicines = Collections.emptyList();
                currentPage = 1;
                totalPages = 1;
                totalRecords = 0;
            }

            renderMedicineCards();
            renderPagination();
        });

        task.setOnFailed(e -> {
            log.error("Lỗi khi tải trang thuốc bán hàng: ", task.getException());
        });

        salesExecutor.submit(task);
    }

    private void renderPagination() {
        PaginationHelper.render(
                paginationBox,
                lblRecordRange,
                currentPage,
                totalPages,
                totalRecords,
                PAGE_SIZE,
                page -> {
                    currentPage = page;
                    loadMedicinesData();
                }
        );
    }

    /**
     * Render 6 thẻ thuốc theo bố cục 2 cột x 3 hàng, không sử dụng thanh cuộn
     */
    private void renderMedicineCards() {
        if (gridMedicineCards == null) return;
        gridMedicineCards.getChildren().clear();

        if (allLoadedMedicines.isEmpty()) {
            if (paneEmptyMedicines != null) {
                paneEmptyMedicines.setVisible(true);
                paneEmptyMedicines.setManaged(true);
            }
            gridMedicineCards.setVisible(false);
            gridMedicineCards.setManaged(false);
            return;
        }

        if (paneEmptyMedicines != null) {
            paneEmptyMedicines.setVisible(false);
            paneEmptyMedicines.setManaged(false);
        }
        gridMedicineCards.setVisible(true);
        gridMedicineCards.setManaged(true);

        for (int i = 0; i < PAGE_SIZE; i++) {
            int col = i % 2;
            int row = i / 2;

            if (i < allLoadedMedicines.size()) {
                MedicineInventoryDTO med = allLoadedMedicines.get(i);
                VBox card = createMedicineCard(med);
                gridMedicineCards.add(card, col, row);
                GridPane.setFillWidth(card, true);
                GridPane.setFillHeight(card, true);
            } else {
                // Ô trống giữ chỗ giúp kích thước 6 ô chứa luôn đồng nhất tuyệt đối giữa các trang
                Region emptySlot = new Region();
                emptySlot.setMaxWidth(Double.MAX_VALUE);
                emptySlot.setMaxHeight(Double.MAX_VALUE);
                GridPane.setFillWidth(emptySlot, true);
                GridPane.setFillHeight(emptySlot, true);
                gridMedicineCards.add(emptySlot, col, row);
            }
        }
    }

    private VBox createMedicineCard(MedicineInventoryDTO med) {
        VBox card = new VBox(5);
        card.getStyleClass().add("medicine-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.ALWAYS);

        boolean outOfStock = med.getTotalQuantity() <= 0;
        if (outOfStock) {
            card.getStyleClass().add("out-of-stock");
        }

        // Hàng 1: Mã thuốc (bên trái) + Badge Kê toa (bên phải)
        HBox rowHeader = new HBox(6);
        rowHeader.setAlignment(Pos.CENTER_LEFT);

        if (med.getMedicineCode() != null && !med.getMedicineCode().isBlank()) {
            Label lblCode = new Label(med.getMedicineCode());
            lblCode.getStyleClass().add("badge-medicine-code");
            lblCode.setMinWidth(Region.USE_PREF_SIZE);
            rowHeader.getChildren().add(lblCode);
        }

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        rowHeader.getChildren().add(headerSpacer);

        if (Boolean.TRUE.equals(med.getRequiresPrescription())) {
            Label lblPrescription = new Label("Kê toa");
            lblPrescription.getStyleClass().add("card-badge-prescription");
            lblPrescription.setMinWidth(Region.USE_PREF_SIZE);
            rowHeader.getChildren().add(lblPrescription);
        }

        // Hàng 2: Tên thuốc (chiếm toàn bộ chiều ngang, viết 2 dòng nếu dài, chiều cao chuẩn 36px cố định cho mọi thẻ)
        Label lblName = new Label(med.getMedicineName());
        lblName.getStyleClass().add("card-title");
        lblName.setWrapText(true);
        lblName.setMinHeight(36.0);
        lblName.setPrefHeight(36.0);
        lblName.setMaxHeight(36.0);
        lblName.setAlignment(Pos.TOP_LEFT);

        // Hàng 3: Đơn vị tính + Cảnh báo HSD gần hoặc Sắp hết (nếu có)
        HBox row3 = new HBox(6);
        row3.setAlignment(Pos.CENTER_LEFT);
        row3.setMinHeight(18.0);
        row3.setPrefHeight(18.0);

        Label lblUnit = new Label(med.getUnit() != null ? med.getUnit() : "hộp");
        lblUnit.getStyleClass().add("card-unit");
        row3.getChildren().add(lblUnit);

        if (med.getStatus() == MedicineInventoryDTO.InventoryStatus.HAS_NEAR_EXPIRY_LOT) {
            Label lblWarning = new Label("HSD gần");
            lblWarning.getStyleClass().add("card-warning-tag");
            lblWarning.setMinWidth(Region.USE_PREF_SIZE);
            row3.getChildren().add(lblWarning);
        }
        if (med.getTotalQuantity() > 0 && med.getTotalQuantity() <= 10) {
            Label lblLowStock = new Label("Sắp hết · " + med.getTotalQuantity());
            lblLowStock.getStyleClass().add("card-low-stock-tag");
            lblLowStock.setMinWidth(Region.USE_PREF_SIZE);
            row3.getChildren().add(lblLowStock);
        } else if (outOfStock) {
            Label lblOut = new Label("Hết hàng");
            lblOut.getStyleClass().add("card-low-stock-tag");
            lblOut.setMinWidth(Region.USE_PREF_SIZE);
            row3.getChildren().add(lblOut);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Hàng 4: Giá tiền (bên trái) + Số lượng còn lại (bên phải)
        HBox row4 = new HBox();
        row4.setAlignment(Pos.BOTTOM_LEFT);
        row4.setMinHeight(22.0);

        Label lblPrice = new Label(FormatUtils.formatCurrency(med.getSellPrice()));
        lblPrice.getStyleClass().add("card-price");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        Label lblStock = new Label("Còn " + med.getTotalQuantity());
        lblStock.getStyleClass().add("card-stock");

        row4.getChildren().addAll(lblPrice, spacerBottom, lblStock);

        card.getChildren().addAll(rowHeader, lblName, row3, spacer, row4);

        boolean inCart = getCartQuantityForMedicine(med.getMedicineId()) > 0;
        if (inCart) {
            card.getStyleClass().add("in-cart");
        }

        // Click để thêm vào giỏ hàng
        card.setOnMouseClicked(e -> {
            if (outOfStock) {
                DialogUtils.showWarning("Hết hàng", "Thuốc '" + med.getMedicineName() + "' hiện đã hết hàng trong kho.");
                return;
            }
            addMedicineToCart(med);
        });

        return card;
    }

    private int getCartQuantityForMedicine(Integer medicineId) {
        if (activeOrder == null || medicineId == null) return 0;
        for (CartItem item : activeOrder.getItems()) {
            if (medicineId.equals(item.getMedicineId())) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    /**
     * Thêm thuốc vào đơn hàng hiện tại
     */
    private void addMedicineToCart(MedicineInventoryDTO med) {
        if (activeOrder == null) return;

        int currentQty = getCartQuantityForMedicine(med.getMedicineId());
        if (currentQty + 1 > med.getTotalQuantity()) {
            DialogUtils.showWarning("Quá tồn kho", "Không thể bán quá số lượng tồn kho khả dụng (" + med.getTotalQuantity() + ").");
            return;
        }

        CartItem item = new CartItem(
                med.getMedicineId(),
                med.getMedicineCode(),
                med.getMedicineName(),
                med.getUnit(),
                med.getRequiresPrescription(),
                med.getSellPrice(),
                1,
                med.getTotalQuantity()
        );

        activeOrder.addItem(item);
        recalculateDiscount();
        renderCartItems();
        renderMedicineCards();
    }

    @FXML
    private void handleClearCurrentCart() {
        if (activeOrder == null || activeOrder.getItems().isEmpty()) return;

        activeOrder.clear();
        if (txtCustomerPhone != null) txtCustomerPhone.clear();
        if (txtDiscount != null) txtDiscount.clear();
        renderCartItems();
        renderMedicineCards();
    }

    /**
     * Render các dòng sản phẩm trong giỏ hàng
     */
    private void renderCartItems() {
        if (boxCartItems == null || paneEmptyCart == null || scrollCartItems == null) return;

        boolean isEmpty = activeOrder == null || activeOrder.getItems().isEmpty();

        paneEmptyCart.setVisible(isEmpty);
        paneEmptyCart.setManaged(isEmpty);
        scrollCartItems.setVisible(!isEmpty);
        scrollCartItems.setManaged(!isEmpty);

        boxCartItems.getChildren().clear();

        if (!isEmpty) {
            for (CartItem item : activeOrder.getItems()) {
                VBox row = createCartItemRow(item);
                boxCartItems.getChildren().add(row);
            }
        }

        updateCartSummary();
    }

    /**
     * Một dòng sản phẩm trong giỏ, chia 2 hàng để không bị cắt chữ khi tên thuốc dài:
     * - Hàng 1: [Mã] Tên thuốc (xuống dòng nếu dài) ............ [Kê toa] [✕]
     * - Hàng 2: Đơn giá / Đơn vị ............ [-] [SL] [+]   Thành tiền
     */
    private VBox createCartItemRow(CartItem item) {
        VBox row = new VBox(8);
        row.getStyleClass().add("cart-item-row");

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        if (item.getMedicineCode() != null && !item.getMedicineCode().isBlank()) {
            Label lblCode = new Label(item.getMedicineCode());
            lblCode.getStyleClass().add("badge-medicine-code");
            lblCode.setMinWidth(Region.USE_PREF_SIZE);
            nameRow.getChildren().add(lblCode);
        }

        Label lblName = new Label(item.getMedicineName());
        lblName.getStyleClass().add("cart-item-name");
        lblName.setWrapText(true);
        lblName.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblName, Priority.ALWAYS);
        nameRow.getChildren().add(lblName);

        if (Boolean.TRUE.equals(item.isRequiresPrescription())) {
            Label lblReq = new Label("Kê toa");
            lblReq.getStyleClass().add("card-badge-prescription");
            lblReq.setMinWidth(Region.USE_PREF_SIZE);
            nameRow.getChildren().add(lblReq);
        }

        Label lblUnitPrice = new Label(FormatUtils.formatCurrency(item.getSellPrice()) + " / " + item.getUnit());
        lblUnitPrice.getStyleClass().add("cart-item-unit-price");
        lblUnitPrice.setMinWidth(Region.USE_PREF_SIZE);

        // Thành tiền của món (không bao giờ bị cắt thành "145.0...")
        Label lblTotal = new Label(FormatUtils.formatCurrency(item.getLineTotal()));
        lblTotal.getStyleClass().add("cart-item-total");
        // Cột thành tiền rộng cố định (đủ tới hàng chục triệu) để ô số lượng các dòng luôn thẳng hàng
        lblTotal.setMinWidth(LINE_TOTAL_WIDTH);
        lblTotal.setPrefWidth(LINE_TOTAL_WIDTH);
        lblTotal.setMaxWidth(LINE_TOTAL_WIDTH);
        lblTotal.setAlignment(Pos.CENTER_RIGHT);

        // Bộ nút tăng/giảm [-] [txtQty] [+]
        HBox qtyBox = CartQuantityControl.create(item, activeOrder, 52, () -> {
            recalculateDiscount();
            renderCartItems();
            renderMedicineCards();
        });

        // Nút xóa dòng
        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("btn-remove-item");
        btnDelete.setOnAction(e -> {
            activeOrder.removeItem(item);
            recalculateDiscount();
            renderCartItems();
            renderMedicineCards();
        });

        btnDelete.setMinWidth(Region.USE_PREF_SIZE);

        // Tên thuốc đã giãn hết chiều ngang (Hgrow) nên nút ✕ luôn nằm sát mép phải
        nameRow.getChildren().add(btnDelete);

        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        HBox priceRow = new HBox(8, lblUnitPrice, priceSpacer, qtyBox, lblTotal);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(nameRow, priceRow);
        return row;
    }

    private void updateCartSummary() {
        if (activeOrder == null) return;

        boolean hasItems = !activeOrder.getItems().isEmpty();
        boolean hasPrescription = activeOrder.hasPrescriptionItem();

        // Banner thuốc kê toa
        if (bannerPrescriptionNotice != null) {
            bannerPrescriptionNotice.setVisible(hasPrescription);
            bannerPrescriptionNotice.setManaged(hasPrescription);
        }

        // Biểu thức tính tiền: Tổng tiền hàng, Giảm giá, Tổng thanh toán
        BigDecimal subtotal = activeOrder.getSubtotal();
        BigDecimal discount = activeOrder.getDiscount();
        BigDecimal total = activeOrder.getTotalAmount();

        if (lblCartSubtotal != null) {
            lblCartSubtotal.setText(FormatUtils.formatCurrency(subtotal) + "đ");
        }
        if (lblCartDiscount != null) {
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                lblCartDiscount.setText("-" + FormatUtils.formatCurrency(discount) + "đ");
            } else {
                lblCartDiscount.setText("0đ");
            }
        }
        if (lblCartTotal != null) {
            lblCartTotal.setText(FormatUtils.formatCurrency(total) + "đ");
        }

        // Nút thanh toán mờ nếu không có sản phẩm
        if (btnCheckout != null) {
            btnCheckout.setDisable(!hasItems);
        }
    }

    @FXML
    private void toggleCustomerDiscountExpand() {
        if (paneExtraFields == null || lblExpandIcon == null) return;
        boolean isExpanded = paneExtraFields.isVisible();
        paneExtraFields.setVisible(!isExpanded);
        paneExtraFields.setManaged(!isExpanded);
        lblExpandIcon.setText(!isExpanded ? "−" : "+");
        if (lblExpandTitle != null) {
            lblExpandTitle.setText(!isExpanded ? "Ẩn giảm giá · SĐT khách" : "Giảm giá · SĐT khách");
        }
    }

    /**
     * Mở Dialog thanh toán theo đúng form bố cục yêu cầu
     */
    @FXML
    private void handleOpenPaymentDialog() {
        if (activeOrder == null || activeOrder.getItems().isEmpty()) {
            DialogUtils.showWarning("Giỏ hàng rỗng", "Vui lòng chọn thuốc trước khi thanh toán.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sales/payment_dialog.fxml"));
            Parent root = loader.load();

            PaymentDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (btnCheckout != null && btnCheckout.getScene() != null) {
                stage.initOwner(btnCheckout.getScene().getWindow());
            }
            stage.setTitle("Thanh toán đơn hàng - PharmaOS");
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            controller.initializeData(activeOrder, orderCode -> {
                // Thanh toán thành công!
                DialogUtils.showInfo("Thanh toán thành công", "Hóa đơn " + orderCode + " đã được thanh toán và trừ kho thành công!");
                activeOrder.clear();
                if (txtCustomerPhone != null) txtCustomerPhone.clear();
                if (txtDiscount != null) txtDiscount.clear();
                renderCartItems();
                renderMedicineCards();
                loadMedicinesData(); // Nạp lại số lượng tồn kho mới nhất
            });

            stage.showAndWait();

            // Đồng bộ lại giỏ hàng vì có thể đã thêm bớt số lượng hoặc xóa thuốc trong dialog thanh toán
            recalculateDiscount();
            renderCartItems();
            renderMedicineCards();
        } catch (IOException e) {
            log.error("Lỗi khi mở hộp thoại thanh toán: ", e);
            DialogUtils.showError("Lỗi hệ thống", "Không thể mở hộp thoại thanh toán: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenSalesHistory() {
        // Mở từ nút lịch sử của trang Bán hàng -> nút back quay lại trang Bán hàng
        historyBackAction = null;
        showSalesHistoryView();
    }

    private void showSalesHistoryView() {
        ensureHistoryViewLoaded();
        if (viewSalesMain != null) {
            viewSalesMain.setVisible(false);
            viewSalesMain.setManaged(false);
        }
        if (viewSalesHistory != null) {
            viewSalesHistory.setVisible(true);
            viewSalesHistory.setManaged(true);
        }
        if (historyController != null) {
            historyController.loadSalesHistoryData();
        }
    }

    /** Mở Lịch sử bán hàng với bộ lọc Theo ngày của ngày chỉ định. */
    public void openSalesHistoryForDate(LocalDate date, Runnable onBack) {
        historyBackAction = onBack;
        showSalesHistoryView();
        if (historyController != null) {
            historyController.showDate(date);
        }
    }

    /** Mở Lịch sử bán hàng với bộ lọc Theo tháng của tháng/năm chỉ định. */
    public void openSalesHistoryForMonth(int year, int month, Runnable onBack) {
        historyBackAction = onBack;
        showSalesHistoryView();
        if (historyController != null) {
            historyController.showMonth(year, month);
        }
    }

    public void handleCloseSalesHistory() {
        if (historyBackAction != null) {
            Runnable back = historyBackAction;
            historyBackAction = null;
            back.run();
            return;
        }
        if (viewSalesHistory != null) {
            viewSalesHistory.setVisible(false);
            viewSalesHistory.setManaged(false);
        }
        if (viewSalesMain != null) {
            viewSalesMain.setVisible(true);
            viewSalesMain.setManaged(true);
        }
        loadMedicinesData();
    }

    private void ensureHistoryViewLoaded() {
        if (historyRootNode != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sales/sales_history.fxml"));
            historyRootNode = loader.load();
            historyController = loader.getController();
            historyController.setOnCloseHistory(this::handleCloseSalesHistory);
            viewSalesHistory.getChildren().setAll(historyRootNode);
        } catch (Exception e) {
            log.error("Không thể tải giao diện lịch sử bán hàng: ", e);
        }
    }
}

