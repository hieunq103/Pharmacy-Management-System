package com.pharmacy.sales;

import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.CartOrder;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.payment.PaymentConfig;
import com.pharmacy.sales.payment.SepayService;
import com.pharmacy.sales.payment.VietQrService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PaymentDialogController {
    private static final Logger log = LoggerFactory.getLogger(PaymentDialogController.class);

    @FXML private Label lblDialogOrderCode;
    @FXML private Label lblItemCountBadge;
    @FXML private ScrollPane scrollDialogCart;
    @FXML private VBox boxDialogCartItems;

    @FXML private Label lblDialogSubtotal;
    @FXML private Label lblDialogDiscount;
    @FXML private Label lblTotalAmount;
    @FXML private HBox boxPrescriptionWarning;
    @FXML private CheckBox chkPrescriptionConfirmed;
    @FXML private Label lblPrescriptionHint;

    @FXML private Button btnMethodCash;
    @FXML private Button btnMethodTransfer;

    @FXML private VBox paneCash;
    @FXML private TextField txtCashGiven;
    @FXML private Button btnExactAmount;
    @FXML private Button btnQuick1;
    @FXML private Button btnQuick2;
    @FXML private Label lblChangeAmount;

    @FXML private VBox paneTransfer;
    @FXML private ImageView imgQrCode;
    @FXML private ProgressIndicator progressQr;
    @FXML private VBox boxSuccessOverlay;
    @FXML private Label lblBankName;
    @FXML private Label lblAccountNo;
    @FXML private Label lblAccountName;
    @FXML private Label lblTransferAmount;
    @FXML private Label lblTransferContent;
    @FXML private Label lblStatusPulse;
    @FXML private Label lblTransferStatus;

    @FXML private Button btnConfirmPayment;

    private final SalesService salesService = new SalesServiceImpl();
    private CartOrder currentOrder;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String selectedMethod = "CASH"; // "CASH" or "TRANSFER"
    private String orderCode;
    private Consumer<String> onSuccessCallback;

    private ScheduledExecutorService pollExecutor;
    private boolean isPaymentReceived = false;
    private Image cachedQrImage = null;

    public void initializeData(CartOrder order, Consumer<String> onSuccess) {
        PaymentConfig.loadProperties();
        this.currentOrder = order;
        this.onSuccessCallback = onSuccess;
        this.totalAmount = order != null ? order.getTotalAmount() : BigDecimal.ZERO;
        this.isPaymentReceived = false;

        // Sinh trước mã hóa đơn cho đơn này
        try {
            this.orderCode = salesService.generateNextOrderCode();
        } catch (Exception e) {
            this.orderCode = "HDB" + System.currentTimeMillis() % 1000000;
        }

        if (lblDialogOrderCode != null) {
            lblDialogOrderCode.setText("Mã hóa đơn: " + this.orderCode);
        }

        if (lblTransferContent != null) {
            lblTransferContent.setText(orderCode);
        }
        if (lblBankName != null) {
            lblBankName.setText(PaymentConfig.getBankId() + " Bank");
        }
        if (lblAccountNo != null) {
            lblAccountNo.setText(PaymentConfig.getAccountNo());
        }
        if (lblAccountName != null) {
            lblAccountName.setText(PaymentConfig.getAccountName());
        }

        btnConfirmPayment.setText("Xác nhận thanh toán");
        btnConfirmPayment.setStyle(null);

        chkPrescriptionConfirmed.selectedProperty().addListener((obs, oldV, isChecked) -> {
            if (currentOrder != null && currentOrder.hasPrescriptionItem()) {
                boolean confirmed = Boolean.TRUE.equals(isChecked);
                btnConfirmPayment.setDisable(!confirmed);
                if (lblPrescriptionHint != null) {
                    lblPrescriptionHint.setVisible(!confirmed);
                    lblPrescriptionHint.setManaged(!confirmed);
                }
            }
        });

        // Nạp danh sách mặt hàng và cập nhật tổng tiền
        renderDialogCartItems();
        updateTotalsAndStatus();

        setupQuickAmounts();
        setupCashInput();
        selectCash();

        // Gắn listener đóng cửa sổ để giải phóng thread và căn chỉnh kích thước
        Platform.runLater(() -> {
            adjustWindowSize();
            if (lblTotalAmount.getScene() != null && lblTotalAmount.getScene().getWindow() != null) {
                lblTotalAmount.getScene().getWindow().setOnHidden(e -> stopPolling());
            }
        });
    }

    /**
     * Render danh sách thuốc đang trong giỏ hàng tại dialog xác nhận thanh toán
     */
    private void renderDialogCartItems() {
        if (boxDialogCartItems == null) return;
        boxDialogCartItems.getChildren().clear();

        if (currentOrder == null || currentOrder.getItems().isEmpty()) {
            Label lblEmpty = new Label("Chưa có thuốc nào trong đơn hàng.");
            lblEmpty.setStyle("-fx-font-family: 'Inter 18pt Medium'; -fx-text-fill: #94A3B8; -fx-padding: 10;");
            boxDialogCartItems.getChildren().add(lblEmpty);
            return;
        }

        for (CartItem item : currentOrder.getItems()) {
            boxDialogCartItems.getChildren().add(createDialogCartItemRow(item));
        }
    }

    /**
     * Tạo từng dòng hiển thị thuốc trong dialog thanh toán với đầy đủ thao tác thêm bớt/nhập số lượng
     */
    private HBox createDialogCartItemRow(CartItem item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dialog-cart-row");

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        if (item.getMedicineCode() != null && !item.getMedicineCode().isBlank()) {
            Label lblCode = new Label(item.getMedicineCode());
            lblCode.getStyleClass().add("badge-medicine-code");
            lblCode.setMinWidth(Region.USE_PREF_SIZE);
            nameRow.getChildren().add(lblCode);
        }

        Label lblName = new Label(item.getMedicineName());
        lblName.getStyleClass().add("dialog-cart-name");
        lblName.setWrapText(true);
        HBox.setHgrow(lblName, Priority.ALWAYS);
        nameRow.getChildren().add(lblName);

        if (Boolean.TRUE.equals(item.isRequiresPrescription())) {
            Label lblReq = new Label("Kê toa");
            lblReq.getStyleClass().add("card-badge-prescription");
            lblReq.setMinWidth(Region.USE_PREF_SIZE);
            nameRow.getChildren().add(lblReq);
        }

        Label lblUnit = new Label(FormatUtils.formatCurrency(item.getSellPrice()) + " / " + item.getUnit());
        lblUnit.getStyleClass().add("dialog-cart-unit");
        infoBox.getChildren().addAll(nameRow, lblUnit);

        // Nút trừ [-]
        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("btn-qty-control");
        btnMinus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            } else {
                currentOrder.removeItem(item);
            }
            refreshDialogCartAndTotals();
        });

        // Ô nhập trực tiếp số lượng thuốc (số nguyên dương)
        TextField txtQty = new TextField(String.valueOf(item.getQuantity()));
        txtQty.getStyleClass().add("txt-qty-input");
        txtQty.setPrefWidth(42);
        txtQty.setMaxWidth(48);
        txtQty.setPrefHeight(26);
        txtQty.setAlignment(Pos.CENTER);

        // Lọc chỉ cho phép nhập số
        txtQty.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                txtQty.setText(newVal.replaceAll("[^0-9]", ""));
            }
        });

        Runnable commitQty = () -> {
            String text = txtQty.getText() != null ? txtQty.getText().trim() : "";
            if (text.isEmpty() || "0".equals(text)) {
                txtQty.setText(String.valueOf(item.getQuantity()));
                return;
            }
            try {
                int newQty = Integer.parseInt(text);
                if (newQty <= 0) newQty = 1;
                if (newQty > item.getMaxStock()) {
                    DialogUtils.showWarning("Vượt quá tồn kho", "Số lượng trong kho chỉ còn " + item.getMaxStock() + ".");
                    newQty = item.getMaxStock();
                }
                if (newQty != item.getQuantity()) {
                    item.setQuantity(newQty);
                    refreshDialogCartAndTotals();
                } else {
                    txtQty.setText(String.valueOf(newQty));
                }
            } catch (Exception ex) {
                txtQty.setText(String.valueOf(item.getQuantity()));
            }
        };

        txtQty.setOnAction(e -> commitQty.run());
        txtQty.setOnMouseClicked(e -> txtQty.selectAll());
        txtQty.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                Platform.runLater(txtQty::selectAll);
            } else {
                commitQty.run();
            }
        });

        // Nút cộng [+]
        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("btn-qty-control");
        btnPlus.setOnAction(e -> {
            if (item.getQuantity() + 1 > item.getMaxStock()) {
                DialogUtils.showWarning("Hết tồn kho", "Số lượng trong kho chỉ còn " + item.getMaxStock() + ".");
                return;
            }
            item.setQuantity(item.getQuantity() + 1);
            refreshDialogCartAndTotals();
        });

        HBox qtyBox = new HBox(4);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.getChildren().addAll(btnMinus, txtQty, btnPlus);

        // Thành tiền từng mặt hàng
        Label lblLineTotal = new Label(FormatUtils.formatCurrency(item.getLineTotal()) + "đ");
        lblLineTotal.getStyleClass().add("dialog-cart-total");
        lblLineTotal.setMinWidth(85);
        lblLineTotal.setAlignment(Pos.CENTER_RIGHT);

        // Nút xóa [✕]
        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("btn-remove-item");
        btnDelete.setOnAction(e -> {
            currentOrder.removeItem(item);
            refreshDialogCartAndTotals();
        });

        row.getChildren().addAll(infoBox, qtyBox, lblLineTotal, btnDelete);
        return row;
    }

    /**
     * Làm mới toàn bộ danh sách giỏ hàng, tổng tiền hàng, giảm giá và phương thức thanh toán
     */
    private void refreshDialogCartAndTotals() {
        renderDialogCartItems();
        updateTotalsAndStatus();

        setupQuickAmounts();

        if (txtCashGiven != null) {
            String digits = txtCashGiven.getText().replaceAll("[^0-9]", "");
            BigDecimal cashGiven = digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits);
            if (cashGiven.compareTo(totalAmount) < 0) {
                txtCashGiven.setText(FormatUtils.formatCurrency(totalAmount));
                updateChange(totalAmount);
            } else {
                updateChange(cashGiven);
            }
        }

        // Tái tạo lại mã QR nếu đang chọn chuyển khoản
        if ("TRANSFER".equals(selectedMethod)) {
            cachedQrImage = null;
            loadVietQrCode();
        }
    }

    private void updateTotalsAndStatus() {
        if (currentOrder == null || currentOrder.getItems().isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            if (lblDialogSubtotal != null) lblDialogSubtotal.setText("0đ");
            if (lblDialogDiscount != null) lblDialogDiscount.setText("0đ");
            if (lblTotalAmount != null) lblTotalAmount.setText("0đ");
            if (lblTransferAmount != null) lblTransferAmount.setText("0đ");
            if (lblItemCountBadge != null) lblItemCountBadge.setText("0 sản phẩm");
            if (boxPrescriptionWarning != null) {
                boxPrescriptionWarning.setVisible(false);
                boxPrescriptionWarning.setManaged(false);
            }
            if (lblPrescriptionHint != null) {
                lblPrescriptionHint.setVisible(false);
                lblPrescriptionHint.setManaged(false);
            }
            btnConfirmPayment.setDisable(true);
            return;
        }

        BigDecimal subtotal = currentOrder.getSubtotal();
        BigDecimal discount = currentOrder.getDiscount();
        this.totalAmount = currentOrder.getTotalAmount();

        if (lblDialogSubtotal != null) {
            lblDialogSubtotal.setText(FormatUtils.formatCurrency(subtotal) + "đ");
        }
        if (lblDialogDiscount != null) {
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                lblDialogDiscount.setText("-" + FormatUtils.formatCurrency(discount) + "đ");
            } else {
                lblDialogDiscount.setText("0đ");
            }
        }

        String formattedTotal = FormatUtils.formatCurrency(totalAmount) + "đ";
        if (lblTotalAmount != null) {
            lblTotalAmount.setText(formattedTotal);
        }
        if (lblTransferAmount != null) {
            lblTransferAmount.setText(formattedTotal);
        }
        if (lblItemCountBadge != null) {
            lblItemCountBadge.setText(currentOrder.getItemCount() + " sản phẩm (" + currentOrder.getItems().size() + " loại)");
        }

        boolean hasPrescription = currentOrder.hasPrescriptionItem();
        if (hasPrescription) {
            boxPrescriptionWarning.setVisible(true);
            boxPrescriptionWarning.setManaged(true);
            boolean confirmed = chkPrescriptionConfirmed.isSelected();
            btnConfirmPayment.setDisable(!confirmed);
            if (lblPrescriptionHint != null) {
                lblPrescriptionHint.setVisible(!confirmed);
                lblPrescriptionHint.setManaged(!confirmed);
                lblPrescriptionHint.setText("Cần xác nhận đã kiểm tra đơn thuốc");
            }
        } else {
            boxPrescriptionWarning.setVisible(false);
            boxPrescriptionWarning.setManaged(false);
            if (lblPrescriptionHint != null) {
                lblPrescriptionHint.setVisible(false);
                lblPrescriptionHint.setManaged(false);
            }
            btnConfirmPayment.setDisable(false);
        }
    }

    private void setupQuickAmounts() {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            btnExactAmount.setText("0đ");
            btnQuick1.setVisible(false);
            btnQuick2.setVisible(false);
            return;
        }

        btnExactAmount.setText("Vừa đủ");
        btnQuick1.setVisible(true);
        btnQuick2.setVisible(true);

        long totalVal = totalAmount.longValue();
        long step = 100_000;
        long next1 = ((totalVal / step) + 1) * step;
        long next2 = next1 + step;

        if (next1 == totalVal) {
            next1 = totalVal + 50_000;
            next2 = totalVal + 100_000;
        }

        btnQuick1.setText(FormatUtils.formatCurrency(next1));
        btnQuick2.setText(FormatUtils.formatCurrency(next2));
    }

    private void setupCashInput() {
        txtCashGiven.setText(FormatUtils.formatCurrency(totalAmount));
        updateChange(totalAmount);

        txtCashGiven.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                updateChange(BigDecimal.ZERO);
                return;
            }

            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                updateChange(BigDecimal.ZERO);
                return;
            }

            try {
                BigDecimal cash = new BigDecimal(digits);
                updateChange(cash);

                String formatted = FormatUtils.formatCurrency(cash);
                if (!formatted.equals(newVal)) {
                    Platform.runLater(() -> {
                        txtCashGiven.setText(formatted);
                        txtCashGiven.positionCaret(formatted.length());
                    });
                }
            } catch (Exception e) {
                updateChange(BigDecimal.ZERO);
            }
        });
    }

    private void updateChange(BigDecimal cashGiven) {
        BigDecimal change = cashGiven.subtract(totalAmount);
        if (change.compareTo(BigDecimal.ZERO) >= 0) {
            lblChangeAmount.setText(FormatUtils.formatCurrency(change) + "đ");
            lblChangeAmount.setStyle("-fx-text-fill: #D97706; -fx-font-family: 'Inter 18pt Bold'; -fx-font-size: 18px;");
        } else {
            lblChangeAmount.setText("Còn thiếu: " + FormatUtils.formatCurrency(change.abs()) + "đ");
            lblChangeAmount.setStyle("-fx-text-fill: #EF4444; -fx-font-family: 'Inter 18pt Bold'; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleSelectCash() {
        selectCash();
    }

    @FXML
    private void handleSelectTransfer() {
        selectTransfer();
    }

    private void selectCash() {
        selectedMethod = "CASH";
        stopPolling();

        while (btnMethodTransfer.getStyleClass().remove("active")) {}
        if (!btnMethodCash.getStyleClass().contains("active")) {
            btnMethodCash.getStyleClass().add("active");
        }
        paneCash.setVisible(true);
        paneCash.setManaged(true);
        paneTransfer.setVisible(false);
        paneTransfer.setManaged(false);

        adjustWindowSize();
    }

    private void selectTransfer() {
        selectedMethod = "TRANSFER";

        while (btnMethodCash.getStyleClass().remove("active")) {}
        if (!btnMethodTransfer.getStyleClass().contains("active")) {
            btnMethodTransfer.getStyleClass().add("active");
        }
        paneCash.setVisible(false);
        paneCash.setManaged(false);
        paneTransfer.setVisible(true);
        paneTransfer.setManaged(true);

        loadVietQrCode();
        startPolling();

        adjustWindowSize();
    }

    private void adjustWindowSize() {
        Platform.runLater(() -> {
            if (lblTotalAmount != null && lblTotalAmount.getScene() != null && lblTotalAmount.getScene().getWindow() instanceof Stage stage) {
                stage.sizeToScene();
                javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                if (stage.getHeight() > visualBounds.getHeight()) {
                    stage.setHeight(visualBounds.getHeight() - 40);
                }
                stage.centerOnScreen();
                if (stage.getY() + stage.getHeight() > visualBounds.getMaxY()) {
                    stage.setY(Math.max(visualBounds.getMinY() + 20, visualBounds.getMaxY() - stage.getHeight() - 20));
                }
            }
        });
    }

    private void loadVietQrCode() {
        if (cachedQrImage != null) {
            imgQrCode.setImage(cachedQrImage);
            progressQr.setVisible(false);
            return;
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            imgQrCode.setImage(null);
            progressQr.setVisible(false);
            return;
        }

        progressQr.setVisible(true);
        CompletableFuture.supplyAsync(() -> VietQrService.generateQrImage(
                PaymentConfig.getBankId(),
                PaymentConfig.getAccountNo(),
                PaymentConfig.getAccountName(),
                totalAmount,
                orderCode
        )).thenAccept(img -> Platform.runLater(() -> {
            progressQr.setVisible(false);
            if (img != null) {
                cachedQrImage = img;
                imgQrCode.setImage(img);
            }
        }));
    }

    private void startPolling() {
        stopPolling();
        if (isPaymentReceived) return;

        if (lblTransferStatus != null) {
            lblTransferStatus.setText("Đang chờ chuyển khoản...");
            lblTransferStatus.setStyle("-fx-text-fill: #475569;");
        }
        if (lblStatusPulse != null) {
            lblStatusPulse.setStyle("-fx-text-fill: #0066FF;");
        }

        pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Payment-Polling-Thread");
            t.setDaemon(true);
            return t;
        });

        long interval = PaymentConfig.getPollIntervalMs();
        pollExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (isPaymentReceived) {
                    stopPolling();
                    return;
                }

                // Toggle animation pulse nhẹ trên giao diện
                Platform.runLater(() -> {
                    if (lblStatusPulse != null) {
                        lblStatusPulse.setOpacity(lblStatusPulse.getOpacity() > 0.5 ? 0.3 : 1.0);
                    }
                });

                if (PaymentConfig.isSepayConfigured()) {
                    SepayService.PaymentResult result = SepayService.checkOrderPayment(orderCode, totalAmount);
                    if (result.matched()) {
                        Platform.runLater(() -> onPaymentSuccess(result.content(), result.amount()));
                    }
                }
            } catch (Exception e) {
                log.error("Error in payment polling loop: {}", e.getMessage());
            }
        }, 1500, interval, TimeUnit.MILLISECONDS);
    }

    private synchronized void onPaymentSuccess(String note, BigDecimal paidAmount) {
        if (isPaymentReceived) return;
        isPaymentReceived = true;
        stopPolling();

        log.info("Payment received for order {}: {}đ", orderCode, paidAmount);

        if (boxSuccessOverlay != null) {
            boxSuccessOverlay.setVisible(true);
        }
        if (lblTransferStatus != null) {
            lblTransferStatus.setText("✓ Đã nhận " + FormatUtils.formatCurrency(paidAmount) + "đ thành công!");
            lblTransferStatus.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
        }
        if (lblStatusPulse != null) {
            lblStatusPulse.setOpacity(1.0);
            lblStatusPulse.setStyle("-fx-text-fill: #16A34A;");
        }

        btnConfirmPayment.setDisable(false);
        btnConfirmPayment.setText("Xác nhận hoàn tất đơn");
        btnConfirmPayment.setStyle("-fx-background-color: #16A34A; -fx-text-fill: #FFFFFF;");
        btnConfirmPayment.requestFocus();

        if (lblPrescriptionHint != null) {
            lblPrescriptionHint.setText("✓ Tiền đã vào tài khoản, vui lòng bấm Xác nhận hoàn tất");
            lblPrescriptionHint.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
            lblPrescriptionHint.setVisible(true);
            lblPrescriptionHint.setManaged(true);
        }
    }

    private void stopPolling() {
        if (pollExecutor != null && !pollExecutor.isShutdown()) {
            pollExecutor.shutdownNow();
            pollExecutor = null;
        }
    }

    @FXML
    private void handleExactCash() {
        txtCashGiven.setText(FormatUtils.formatCurrency(totalAmount));
    }

    @FXML
    private void handleQuickAmount1() {
        try {
            String txt = btnQuick1.getText().replace(".", "");
            txtCashGiven.setText(FormatUtils.formatCurrency(new BigDecimal(txt)));
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleQuickAmount2() {
        try {
            String txt = btnQuick2.getText().replace(".", "");
            txtCashGiven.setText(FormatUtils.formatCurrency(new BigDecimal(txt)));
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleCancel() {
        stopPolling();
        closeStage();
    }

    @FXML
    private void handleConfirmPayment() {
        stopPolling();

        if (currentOrder == null || currentOrder.getItems().isEmpty()) {
            DialogUtils.showError("Lỗi", "Đơn hàng rỗng không thể thanh toán.");
            return;
        }

        if (currentOrder.hasPrescriptionItem() && !chkPrescriptionConfirmed.isSelected()) {
            DialogUtils.showWarning("Chưa xác nhận đơn thuốc", "Vui lòng tích xác nhận đã kiểm tra đơn thuốc của khách hàng trước khi bán.");
            return;
        }

        // Kiểm tra tiền đưa nếu chọn Tiền mặt
        if ("CASH".equals(selectedMethod)) {
            String cashText = txtCashGiven.getText().replaceAll("[^0-9]", "");
            BigDecimal cash = cashText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cashText);
            if (cash.compareTo(totalAmount) < 0) {
                DialogUtils.showWarning("Số tiền không đủ", "Số tiền khách đưa chưa đủ để hoàn tất đơn hàng.");
                return;
            }
        }

        Integer employeeId = UserSession.getInstance() != null ? UserSession.getInstance().getEmployeeId() : null;

        SalesOrderRequest req = new SalesOrderRequest(
                employeeId,
                currentOrder.getCustomerPhone(),
                currentOrder.getDiscount(),
                selectedMethod,
                chkPrescriptionConfirmed.isSelected(),
                new ArrayList<>(currentOrder.getItems())
        );
        req.setOrderCode(this.orderCode);

        try {
            String createdOrderCode = salesService.checkout(req);
            closeStage();
            if (onSuccessCallback != null) {
                onSuccessCallback.accept(createdOrderCode != null ? createdOrderCode : this.orderCode);
            }
        } catch (Exception ex) {
            DialogUtils.showError("Thanh toán thất bại", ex.getMessage());
        }
    }

    private void closeStage() {
        stopPolling();
        Stage stage = (Stage) lblTotalAmount.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
