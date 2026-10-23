package com.pharmacy.sales;

import com.pharmacy.common.DialogUtils;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.sales.dto.CartOrder;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.payment.PaymentConfig;
import com.pharmacy.sales.payment.SepayService;
import com.pharmacy.sales.payment.VietQrService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

    @FXML private Label lblDialogSubtotal;
    @FXML private Label lblDialogDiscount;
    @FXML private Label lblTotalAmount;
    @FXML private VBox boxPrescriptionWarning;
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

        BigDecimal subtotal = order != null ? order.getSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = order != null ? order.getDiscount() : BigDecimal.ZERO;

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
        lblTotalAmount.setText(formattedTotal);
        if (lblTransferAmount != null) {
            lblTransferAmount.setText(formattedTotal);
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

        boolean hasPrescription = order != null && order.hasPrescriptionItem();

        if (hasPrescription) {
            boxPrescriptionWarning.setVisible(true);
            boxPrescriptionWarning.setManaged(true);
            chkPrescriptionConfirmed.setSelected(false);

            lblPrescriptionHint.setVisible(true);
            lblPrescriptionHint.setManaged(true);
            lblPrescriptionHint.setText("Cần xác nhận đã kiểm tra đơn thuốc");

            btnConfirmPayment.setDisable(true);
            chkPrescriptionConfirmed.selectedProperty().addListener((obs, oldV, isChecked) -> {
                boolean confirmed = Boolean.TRUE.equals(isChecked);
                btnConfirmPayment.setDisable(!confirmed);
                lblPrescriptionHint.setVisible(!confirmed);
                lblPrescriptionHint.setManaged(!confirmed);
            });
        } else {
            boxPrescriptionWarning.setVisible(false);
            boxPrescriptionWarning.setManaged(false);
            lblPrescriptionHint.setVisible(false);
            lblPrescriptionHint.setManaged(false);
            btnConfirmPayment.setDisable(false);
        }

        setupQuickAmounts();
        setupCashInput();
        selectCash();

        // Gắn listener đóng cửa sổ để giải phóng thread
        Platform.runLater(() -> {
            if (lblTotalAmount.getScene() != null && lblTotalAmount.getScene().getWindow() != null) {
                lblTotalAmount.getScene().getWindow().setOnHidden(e -> stopPolling());
            }
        });
    }

    private void setupQuickAmounts() {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            btnExactAmount.setText("0đ");
            btnQuick1.setVisible(false);
            btnQuick2.setVisible(false);
            return;
        }

        btnExactAmount.setText("Vừa đủ");

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
            lblChangeAmount.setStyle("-fx-text-fill: #D97706; -fx-font-family: 'Inter 18pt Bold'; -fx-font-size: 28px;");
        } else {
            lblChangeAmount.setText("Còn thiếu: " + FormatUtils.formatCurrency(change.abs()) + "đ");
            lblChangeAmount.setStyle("-fx-text-fill: #EF4444; -fx-font-family: 'Inter 18pt Bold'; -fx-font-size: 20px;");
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
    }

    private void loadVietQrCode() {
        if (cachedQrImage != null) {
            imgQrCode.setImage(cachedQrImage);
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

        lblTransferStatus.setText("Đang chờ chuyển khoản...");
        lblTransferStatus.setStyle("-fx-text-fill: #475569;");
        lblStatusPulse.setStyle("-fx-text-fill: #0066FF;");

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
