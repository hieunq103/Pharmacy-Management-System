package com.pharmacy.common;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

import java.util.Map;

/**
 * Tiện ích tạo menu lọc (Filter dropdown) trên thanh tiêu đề của TableColumn.
 */
public final class TableFilterHelper {

    private TableFilterHelper() {
    }

    /**
     * Gắn dropdown menu lọc lên TableColumn.
     */
    public static <T> void setupColumnFilter(
            TableColumn<T, ?> column,
            String title,
            String[] options,
            String filterKey,
            Map<String, String> activeFilters,
            Runnable onFilterChanged) {
        if (column == null || options == null)
            return;

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("label");

        Label iconFilter = new Label("▾");
        iconFilter.getStyleClass().add("btn-header-filter");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-context-menu");

        java.util.function.Consumer<String> filterSetter = targetOpt -> {
            String selected = (targetOpt != null && !targetOpt.isBlank()) ? targetOpt : "Tất cả";
            if ("Tất cả".equals(selected)) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, selected);
            }

            if (activeFilters.containsKey(filterKey)) {
                if (!iconFilter.getStyleClass().contains("filter-active")) {
                    iconFilter.getStyleClass().add("filter-active");
                }
            } else {
                iconFilter.getStyleClass().remove("filter-active");
            }

            // Cập nhật trạng thái active trên các label trong menu
            for (MenuItem mi : menu.getItems()) {
                if (mi instanceof CustomMenuItem cmi && cmi.getContent() instanceof Label lbl) {
                    lbl.getStyleClass().remove("filter-item-active");
                    if (lbl.getText().equals(selected)) {
                        lbl.getStyleClass().add("filter-item-active");
                    }
                }
            }

            if (onFilterChanged != null) {
                onFilterChanged.run();
            }
            menu.hide();
        };

        column.getProperties().put("tableFilterHelper_setter", filterSetter);

        for (String opt : options) {
            Label lblOpt = new Label(opt);
            lblOpt.setMaxWidth(Double.MAX_VALUE);
            lblOpt.getStyleClass().add("filter-item-label");
            if ("Tất cả".equals(opt)) {
                lblOpt.getStyleClass().add("filter-item-active");
            }

            CustomMenuItem item = new CustomMenuItem(lblOpt, true);
            item.getStyleClass().add("custom-filter-menu-item");

            item.setOnAction(e -> filterSetter.accept(opt));
            lblOpt.setOnMouseClicked(e -> {
                filterSetter.accept(opt);
                e.consume();
            });

            menu.getItems().add(item);
        }

        iconFilter.setOnMouseClicked(e -> {
            if (!menu.isShowing()) {
                menu.show(iconFilter, Side.BOTTOM, -12, 6);
            } else {
                menu.hide();
            }
            e.consume();
        });

        HBox headerBox = new HBox(6, lblTitle, iconFilter);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        column.setText(null);
        column.setGraphic(headerBox);
    }

    /**
     * Kích hoạt bộ lọc trên cột theo lập trình
     */
    public static void setFilter(TableColumn<?, ?> column, String value) {
        if (column == null)
            return;
        Object setter = column.getProperties().get("tableFilterHelper_setter");
        if (setter instanceof java.util.function.Consumer<?> consumer) {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> stringConsumer = (java.util.function.Consumer<String>) consumer;
            stringConsumer.accept(value);
        }
    }

    /**
     * Gắn dropdown menu lọc dạng nhiều lựa chọn lên TableColumn.
     */
    public static <T> void setupMultiSelectColumnFilter(
            TableColumn<T, ?> column,
            String title,
            String[] options,
            String filterKey,
            Map<String, String> activeFilters,
            Runnable onFilterChanged) {
        if (column == null || options == null)
            return;

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("label");

        Label iconFilter = new Label("▾");
        iconFilter.getStyleClass().add("btn-header-filter");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("filter-context-menu");

        Map<String, javafx.scene.control.CheckBox> checkBoxes = new java.util.LinkedHashMap<>();
        javafx.scene.control.CheckBox chkAll = new javafx.scene.control.CheckBox("Tất cả");
        chkAll.getStyleClass().add("filter-checkbox");
        chkAll.setSelected(true);

        java.util.function.Consumer<java.util.Collection<String>> multiSetter = selectedValues -> {
            boolean isAll = selectedValues == null || selectedValues.isEmpty() || selectedValues.contains("Tất cả")
                    || selectedValues.size() == options.length;

            if (isAll) {
                activeFilters.remove(filterKey);
                chkAll.setSelected(true);
                for (javafx.scene.control.CheckBox cb : checkBoxes.values()) {
                    cb.setSelected(false);
                }
                iconFilter.getStyleClass().remove("filter-active");
            } else {
                activeFilters.put(filterKey, String.join(",", selectedValues));
                chkAll.setSelected(false);
                for (Map.Entry<String, javafx.scene.control.CheckBox> entry : checkBoxes.entrySet()) {
                    entry.getValue().setSelected(selectedValues.contains(entry.getKey()));
                }
                if (!iconFilter.getStyleClass().contains("filter-active")) {
                    iconFilter.getStyleClass().add("filter-active");
                }
            }

            if (onFilterChanged != null) {
                onFilterChanged.run();
            }
        };

        column.getProperties().put("tableFilterHelper_multiSetter", multiSetter);

        // Hàng "Tất cả"
        HBox boxAll = new HBox(8, chkAll);
        boxAll.setAlignment(Pos.CENTER_LEFT);
        boxAll.getStyleClass().add("filter-item-label");
        CustomMenuItem itemAll = new CustomMenuItem(boxAll, false);
        itemAll.getStyleClass().add("custom-filter-menu-item");

        chkAll.setOnAction(e -> {
            if (chkAll.isSelected()) {
                multiSetter.accept(java.util.Collections.emptyList());
            } else {
                chkAll.setSelected(true);
            }
        });
        boxAll.setOnMouseClicked(e -> {
            if (e.getTarget() != chkAll) {
                chkAll.setSelected(true);
                multiSetter.accept(java.util.Collections.emptyList());
            }
            e.consume();
        });
        menu.getItems().add(itemAll);

        // Các hàng tùy chọn thực tế
        for (String opt : options) {
            javafx.scene.control.CheckBox chk = new javafx.scene.control.CheckBox(opt);
            chk.getStyleClass().add("filter-checkbox");
            checkBoxes.put(opt, chk);

            HBox boxOpt = new HBox(8, chk);
            boxOpt.setAlignment(Pos.CENTER_LEFT);
            boxOpt.getStyleClass().add("filter-item-label");
            CustomMenuItem itemOpt = new CustomMenuItem(boxOpt, false);
            itemOpt.getStyleClass().add("custom-filter-menu-item");

            Runnable handleCheck = () -> {
                java.util.List<String> selected = new java.util.ArrayList<>();
                for (Map.Entry<String, javafx.scene.control.CheckBox> entry : checkBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selected.add(entry.getKey());
                    }
                }
                multiSetter.accept(selected);
            };

            chk.setOnAction(e -> handleCheck.run());
            boxOpt.setOnMouseClicked(e -> {
                if (e.getTarget() != chk) {
                    chk.setSelected(!chk.isSelected());
                    handleCheck.run();
                }
                e.consume();
            });

            menu.getItems().add(itemOpt);
        }

        iconFilter.setOnMouseClicked(e -> {
            if (!menu.isShowing()) {
                menu.show(iconFilter, Side.BOTTOM, -12, 6);
            } else {
                menu.hide();
            }
            e.consume();
        });

        HBox headerBox = new HBox(6, lblTitle, iconFilter);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        column.setText(null);
        column.setGraphic(headerBox);
    }

    /**
     * Kích hoạt nhiều giá trị lọc dạng multi-select checkbox theo lập trình
     */
    public static void setMultiFilter(TableColumn<?, ?> column, String... values) {
        if (column == null)
            return;
        Object setter = column.getProperties().get("tableFilterHelper_multiSetter");
        if (setter instanceof java.util.function.Consumer<?> consumer) {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<java.util.Collection<String>> collConsumer = (java.util.function.Consumer<java.util.Collection<String>>) consumer;
            collConsumer.accept(values != null ? java.util.Arrays.asList(values) : java.util.Collections.emptyList());
        }
    }
}
