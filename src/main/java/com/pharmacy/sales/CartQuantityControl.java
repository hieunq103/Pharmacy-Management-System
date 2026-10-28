package com.pharmacy.sales;

import com.pharmacy.common.DialogUtils;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.CartOrder;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

final class CartQuantityControl {
    private CartQuantityControl() {
    }

    static HBox create(CartItem item, CartOrder order, double inputWidth, Runnable onChanged) {
        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("btn-qty-control");
        btnMinus.setMinWidth(Region.USE_PREF_SIZE);
        btnMinus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            } else {
                order.removeItem(item);
            }
            onChanged.run();
        });

        TextField txtQty = new TextField(String.valueOf(item.getQuantity()));
        txtQty.getStyleClass().add("txt-qty-input");
        txtQty.setMinWidth(inputWidth - 4);
        txtQty.setPrefWidth(inputWidth);
        txtQty.setMaxWidth(inputWidth + 8);
        txtQty.setPrefHeight(26);
        txtQty.setAlignment(Pos.CENTER);

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
                    onChanged.run();
                } else {
                    txtQty.setText(String.valueOf(newQty));
                }
            } catch (NumberFormatException ex) {
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

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("btn-qty-control");
        btnPlus.setMinWidth(Region.USE_PREF_SIZE);
        btnPlus.setOnAction(e -> {
            if (item.getQuantity() + 1 > item.getMaxStock()) {
                DialogUtils.showWarning("Hết tồn kho", "Số lượng trong kho chỉ còn " + item.getMaxStock() + ".");
                return;
            }
            item.setQuantity(item.getQuantity() + 1);
            onChanged.run();
        });

        HBox qtyBox = new HBox(4, btnMinus, txtQty, btnPlus);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.setMinWidth(Region.USE_PREF_SIZE);
        return qtyBox;
    }
}
