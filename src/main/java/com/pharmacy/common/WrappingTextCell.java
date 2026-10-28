package com.pharmacy.common;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class WrappingTextCell<S> extends TableCell<S, String> {
    private final Label label = new Label();

    public WrappingTextCell(TableColumn<S, String> column) {
        label.setWrapText(true);
        label.maxWidthProperty().bind(column.widthProperty().subtract(20));
        label.getStyleClass().add("wrapping-cell-text");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER_LEFT);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forColumn() {
        return WrappingTextCell::new;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isBlank()) {
            setGraphic(null);
            setTooltip(null);
        } else {
            label.setText(item);
            setGraphic(label);
            setTooltip(new Tooltip(item));
        }
    }
}
