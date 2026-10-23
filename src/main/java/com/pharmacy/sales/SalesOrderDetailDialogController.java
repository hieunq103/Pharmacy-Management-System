package com.pharmacy.sales;

import com.pharmacy.common.DebounceUtil;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class SalesOrderDetailDialogController {

    @FXML private Label lblOrderCode;
    @FXML private Label lblOrderTime;
    @FXML private Label lblEmployee;
    @FXML private Label lblCustomerPhone;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblItemCount;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblRowCount;

    @FXML private TextField txtSearchDetail;

    @FXML private TableView<SalesOrderDetailDTO> tableDetails;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colIndex;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colMedCode;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colMedName;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colCategory;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colLotNumber;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colExpiryDate;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colUnit;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colQuantity;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colUnitPrice;
    @FXML private TableColumn<SalesOrderDetailDTO, String> colTotalPrice;

    private final ObservableList<SalesOrderDetailDTO> masterItems = FXCollections.observableArrayList();
    private final ObservableList<SalesOrderDetailDTO> filteredItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchFilter();
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(cellData -> {
            int index = tableDetails.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(index));
        });

        colMedCode.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedicineCode()));

        colMedName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedicineName()));

        colCategory.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategoryName()));

        colLotNumber.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLotNumber()));

        colExpiryDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedExpiryDate()));

        colUnit.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUnit()));

        colQuantity.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));

        colUnitPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedUnitPrice()));

        colTotalPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedLineTotal()));

        tableDetails.setItems(filteredItems);
    }

    private void setupSearchFilter() {
        if (txtSearchDetail != null) {
            DebounceUtil.debounce(txtSearchDetail, Duration.millis(200), this::filterItems);
        }
    }

    public void setOrderData(SalesOrderDTO order, List<SalesOrderDetailDTO> items) {
        if (order != null) {
            lblOrderCode.setText(order.getCode());
            lblOrderTime.setText("Thời gian: " + order.getFormattedTime());
            lblEmployee.setText(order.getEmployeeName() + (order.getEmployeeCode() != null && !order.getEmployeeCode().isBlank() ? " (" + order.getEmployeeCode() + ")" : ""));
            lblCustomerPhone.setText(order.getCustomerPhone());
            lblPaymentMethod.setText(order.getFormattedPaymentMethod());
            lblItemCount.setText(order.getFormattedItemSummary());
            lblSubtotal.setText(order.getFormattedSubtotal());
            lblDiscount.setText(order.getFormattedDiscount());
            lblTotalAmount.setText(order.getFormattedTotal());
        }

        masterItems.clear();
        if (items != null) {
            masterItems.addAll(items);
        }
        filterItems(txtSearchDetail != null ? txtSearchDetail.getText() : "");
    }

    private void filterItems(String keyword) {
        filteredItems.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredItems.addAll(masterItems);
        } else {
            String lower = keyword.trim().toLowerCase();
            for (SalesOrderDetailDTO item : masterItems) {
                boolean matchCode = item.getMedicineCode() != null && item.getMedicineCode().toLowerCase().contains(lower);
                boolean matchName = item.getMedicineName() != null && item.getMedicineName().toLowerCase().contains(lower);
                boolean matchLot = item.getLotNumber() != null && item.getLotNumber().toLowerCase().contains(lower);
                boolean matchIngr = item.getActiveIngredient() != null && item.getActiveIngredient().toLowerCase().contains(lower);

                if (matchCode || matchName || matchLot || matchIngr) {
                    filteredItems.add(item);
                }
            }
        }

        if (lblRowCount != null) {
            lblRowCount.setText("Danh sách mặt hàng (" + filteredItems.size() + "/" + masterItems.size() + "):");
        }
    }

    @FXML
    private void handleClose() {
        if (lblOrderCode != null && lblOrderCode.getScene() != null) {
            Stage stage = (Stage) lblOrderCode.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}
