package com.pharmacy.common;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

public final class TableSyncHelper {

    private TableSyncHelper() {}

    public static void enableAutoRowHeight(TableView<?> mainTable, TableView<?> stickyTable, int targetRowCount) {
        if (mainTable == null || targetRowCount <= 0) return;

        Runnable updateRowHeight = () -> {
            double tableHeight = mainTable.getHeight();
            if (tableHeight <= 0) return;

            Node header = mainTable.lookup(".column-header-background");
            double headerHeight = 44.0;
            if (header != null && header.getLayoutBounds().getHeight() > 0) {
                headerHeight = header.getLayoutBounds().getHeight();
            }

            double hBarHeight = 0.0;
            for (Node n : mainTable.lookupAll(".scroll-bar")) {
                if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL && sb.isVisible()) {
                    hBarHeight = sb.getLayoutBounds().getHeight();
                    break;
                }
            }

            double availableHeight = Math.max(0.0, tableHeight - headerHeight - hBarHeight - 2.0);
            double targetCellSize = Math.max(36.0, Math.floor(availableHeight / targetRowCount));

            if (!mainTable.fixedCellSizeProperty().isBound()) {
                mainTable.setFixedCellSize(targetCellSize);
            }
            if (stickyTable != null && !stickyTable.fixedCellSizeProperty().isBound()) {
                stickyTable.setFixedCellSize(targetCellSize);
            }
        };

        mainTable.heightProperty().addListener((obs, oldH, newH) -> Platform.runLater(updateRowHeight));
        mainTable.skinProperty().addListener((obs, oldS, newS) -> Platform.runLater(updateRowHeight));
        mainTable.sceneProperty().addListener((obs, oldSc, newSc) -> Platform.runLater(updateRowHeight));
        mainTable.visibleProperty().addListener((obs, oldV, newV) -> {
            if (Boolean.TRUE.equals(newV)) Platform.runLater(updateRowHeight);
        });

        Platform.runLater(updateRowHeight);
    }

    public static <T> void setupDualTableSync(TableView<T> mainTable, TableView<T> stickyTable) {
        if (mainTable == null || stickyTable == null) return;

        stickyTable.setFocusTraversable(false);
        stickyTable.setSelectionModel(null);
        stickyTable.setPlaceholder(new Region());
        stickyTable.itemsProperty().bind(mainTable.itemsProperty());
        stickyTable.fixedCellSizeProperty().bind(mainTable.fixedCellSizeProperty());

        mainTable.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems != null && !newItems.isEmpty()) {
                resetScrollToTop(mainTable, stickyTable);
            }
            if (newItems != null) {
                newItems.addListener((ListChangeListener<? super T>) c -> {
                    if (mainTable.getItems() != null && !mainTable.getItems().isEmpty()) {
                        resetScrollToTop(mainTable, stickyTable);
                    }
                });
            }
        });
        if (mainTable.getItems() != null) {
            mainTable.getItems().addListener((ListChangeListener<? super T>) c -> {
                if (mainTable.getItems() != null && !mainTable.getItems().isEmpty()) {
                    resetScrollToTop(mainTable, stickyTable);
                }
            });
        }

        final boolean[] attached = new boolean[]{false};
        final boolean[] isSyncing = new boolean[]{false};

        Runnable tryAttachSync = () -> {
            if (attached[0]) return;

            ScrollBar vBarMain = findVerticalScrollBar(mainTable);
            ScrollBar vBarSticky = findVerticalScrollBar(stickyTable);

            if (vBarMain != null && vBarSticky != null) {
                attached[0] = true;

                vBarMain.valueProperty().addListener((obs, oldV, newV) -> {
                    if (mainTable.getItems() == null || mainTable.getItems().isEmpty()) return;
                    if (!isSyncing[0] && newV != null) {
                        isSyncing[0] = true;
                        try {
                            double maxAllowed = calculateMaxAllowedScroll(mainTable, vBarMain);
                            double effectiveVal = newV.doubleValue();
                            if (effectiveVal > maxAllowed) {
                                effectiveVal = maxAllowed;
                                vBarMain.setValue(maxAllowed);
                            }

                            double rangeMain = vBarMain.getMax() - vBarMain.getMin();
                            double rangeSticky = vBarSticky.getMax() - vBarSticky.getMin();
                            double target;
                            if (rangeMain > 0 && rangeSticky > 0 && Math.abs(rangeMain - rangeSticky) > 1e-3) {
                                double ratio = (effectiveVal - vBarMain.getMin()) / rangeMain;
                                target = vBarSticky.getMin() + ratio * rangeSticky;
                            } else {
                                target = effectiveVal;
                            }
                            if (Math.abs(vBarSticky.getValue() - target) > 1e-4) {
                                vBarSticky.setValue(target);
                            }
                        } finally {
                            isSyncing[0] = false;
                        }
                    }
                });

                vBarSticky.valueProperty().addListener((obs, oldV, newV) -> {
                    if (stickyTable.getItems() == null || stickyTable.getItems().isEmpty()) return;
                    if (!isSyncing[0] && newV != null) {
                        isSyncing[0] = true;
                        try {
                            double maxAllowed = calculateMaxAllowedScroll(mainTable, vBarMain);
                            double effectiveVal = newV.doubleValue();
                            if (effectiveVal > maxAllowed) {
                                effectiveVal = maxAllowed;
                                vBarSticky.setValue(maxAllowed);
                            }

                            double rangeMain = vBarMain.getMax() - vBarMain.getMin();
                            double rangeSticky = vBarSticky.getMax() - vBarSticky.getMin();
                            double target;
                            if (rangeMain > 0 && rangeSticky > 0 && Math.abs(rangeMain - rangeSticky) > 1e-3) {
                                double ratio = (effectiveVal - vBarSticky.getMin()) / rangeSticky;
                                target = vBarMain.getMin() + ratio * rangeMain;
                            } else {
                                target = effectiveVal;
                            }
                            if (Math.abs(vBarMain.getValue() - target) > 1e-4) {
                                vBarMain.setValue(target);
                            }
                        } finally {
                            isSyncing[0] = false;
                        }
                    }
                });
            }
        };

        Platform.runLater(tryAttachSync);
        mainTable.skinProperty().addListener((obs, oldS, newS) -> {
            if (!attached[0] && newS != null) Platform.runLater(tryAttachSync);
        });
        stickyTable.skinProperty().addListener((obs, oldS, newS) -> {
            if (!attached[0] && newS != null) Platform.runLater(tryAttachSync);
        });
        mainTable.sceneProperty().addListener((obs, oldSc, newSc) -> {
            if (!attached[0] && newSc != null) Platform.runLater(tryAttachSync);
        });
        mainTable.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (!attached[0]) tryAttachSync.run();
        });
        stickyTable.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (!attached[0]) tryAttachSync.run();
        });

        attachVirtualFlowProtection(mainTable);
        attachVirtualFlowProtection(stickyTable);

        stickyTable.addEventFilter(ScrollEvent.SCROLL, event -> {
            double dx = event.getDeltaX();
            double dy = event.getDeltaY();

            if (Math.abs(dx) == 0 && event.isShiftDown() && Math.abs(dy) > 0) {
                dx = dy;
                dy = 0;
            }

            if (Math.abs(dx) > 0) {
                scrollTableHorizontally(mainTable, dx);
            }

            event.consume();
        });

        mainTable.addEventFilter(ScrollEvent.SCROLL, event -> {
            double dx = event.getDeltaX();
            double dy = event.getDeltaY();

            if (Math.abs(dx) == 0 && event.isShiftDown() && Math.abs(dy) > 0) {
                dx = dy;
                dy = 0;
            }

            if (Math.abs(dx) > 0) {
                scrollTableHorizontally(mainTable, dx);
            }

            event.consume();
        });
    }

    public static <T> void setupTableScrollProtection(TableView<T> table, int targetRowCount) {
        if (table == null) return;

        table.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems != null && !newItems.isEmpty()) {
                resetScrollToTop(table, null);
            }
            if (newItems != null) {
                newItems.addListener((ListChangeListener<? super T>) c -> {
                    if (table.getItems() != null && !table.getItems().isEmpty()) {
                        resetScrollToTop(table, null);
                    }
                });
            }
        });
        if (table.getItems() != null) {
            table.getItems().addListener((ListChangeListener<? super T>) c -> {
                if (table.getItems() != null && !table.getItems().isEmpty()) {
                    resetScrollToTop(table, null);
                }
            });
        }

        attachVirtualFlowProtection(table);

        table.addEventFilter(ScrollEvent.SCROLL, event -> {
            double dx = event.getDeltaX();
            double dy = event.getDeltaY();

            if (Math.abs(dx) == 0 && event.isShiftDown() && Math.abs(dy) > 0) {
                dx = dy;
                dy = 0;
            }

            if (Math.abs(dx) > 0) {
                scrollTableHorizontally(table, dx);
            }

            event.consume();
        });
    }

    public static void attachVirtualFlowProtection(TableView<?> table) {
        if (table == null) return;

        Runnable attach = () -> {
            Node flowNode = table.lookup(".virtual-flow");
            if (flowNode instanceof VirtualFlow<?> flow) {
                flow.positionProperty().addListener((obs, oldPos, newPos) -> {
                    if (table.getItems() == null || table.getItems().isEmpty()) {
                        return;
                    }
                    if (newPos != null && newPos.doubleValue() > 0.0001) {
                        Platform.runLater(() -> {
                            if (table.getItems() != null && !table.getItems().isEmpty()) {
                                flow.setPosition(0.0);
                                flow.scrollToTop(0);
                            }
                        });
                    }
                });
            }
        };

        Platform.runLater(attach);
        table.skinProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(attach);
        });
        table.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(attach);
        });
    }

    public static void resetScrollToTop(TableView<?> mainTable, TableView<?> stickyTable) {
        boolean mainEmpty = mainTable == null || mainTable.getItems() == null || mainTable.getItems().isEmpty();
        boolean stickyEmpty = stickyTable == null || stickyTable.getItems() == null || stickyTable.getItems().isEmpty();
        if (mainEmpty && stickyEmpty) {
            return;
        }

        Runnable doReset = () -> {
            resetSingleTable(mainTable);
            resetSingleTable(stickyTable);
        };

        doReset.run();
        Platform.runLater(doReset);
    }

    public static void resetSingleTable(TableView<?> table) {
        if (table == null || table.getItems() == null || table.getItems().isEmpty()) return;
        table.scrollTo(0);
        ScrollBar vBar = findVerticalScrollBar(table);
        if (vBar != null && vBar.isVisible()) {
            vBar.setValue(vBar.getMin());
        }
        Node flowNode = table.lookup(".virtual-flow");
        if (flowNode instanceof VirtualFlow<?> flow) {
            flow.setPosition(0.0);
            flow.scrollToTop(0);
        }
    }

    public static double getAvailableTableHeight(TableView<?> table) {
        if (table == null) return 0.0;
        double tableHeight = table.getHeight();
        if (tableHeight <= 0) return 0.0;

        Node header = table.lookup(".column-header-background");
        double headerHeight = 44.0;
        if (header != null && header.getLayoutBounds().getHeight() > 0) {
            headerHeight = header.getLayoutBounds().getHeight();
        }

        double hBarHeight = 0.0;
        ScrollBar hBar = findHorizontalScrollBar(table);
        if (hBar != null && hBar.isVisible()) {
            hBarHeight = hBar.getLayoutBounds().getHeight();
        }

        return Math.max(0.0, tableHeight - headerHeight - hBarHeight - 2.0);
    }

    public static double calculateMaxAllowedScroll(TableView<?> table) {
        return calculateMaxAllowedScroll(table, findVerticalScrollBar(table));
    }

    public static double calculateMaxAllowedScroll(TableView<?> table, ScrollBar vBar) {
        if (table == null || table.getItems() == null || table.getItems().isEmpty()) return 0.0;
        if (vBar == null || vBar.getMax() <= vBar.getMin()) return 0.0;

        double availableHeight = getAvailableTableHeight(table);
        int itemCount = table.getItems().size();
        double cellSize = table.getFixedCellSize() > 0 ? table.getFixedCellSize() : 50.0;
        double totalContentHeight = itemCount * cellSize;

        if (totalContentHeight <= availableHeight + 2.0) {
            return vBar.getMin();
        }

        double visibleRowCount = availableHeight / cellSize;
        double overflowRows = itemCount - visibleRowCount;
        if (overflowRows <= 0) return vBar.getMin();

        double range = vBar.getMax() - vBar.getMin();
        double maxAllowed;
        if (range > 1.5) {
            maxAllowed = vBar.getMin() + overflowRows;
        } else {
            maxAllowed = vBar.getMin() + (overflowRows / itemCount) * range;
        }
        return Math.max(vBar.getMin(), Math.min(vBar.getMax(), maxAllowed));
    }

    public static ScrollBar findVerticalScrollBar(TableView<?> table) {
        if (table == null) return null;
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                return sb;
            }
        }
        return null;
    }

    public static ScrollBar findHorizontalScrollBar(TableView<?> table) {
        if (table == null) return null;
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL) {
                return sb;
            }
        }
        return null;
    }

    public static void scrollTableHorizontally(TableView<?> table, double delta) {
        if (table == null) return;
        ScrollBar hBar = findHorizontalScrollBar(table);
        if (hBar != null && hBar.getMax() > hBar.getMin()) {
            double newV = hBar.getValue() - delta;
            hBar.setValue(Math.min(hBar.getMax(), Math.max(hBar.getMin(), newV)));
        } else if (table.getItems().isEmpty()) {
            Node nestedHeader = table.lookup(".nested-column-header");
            if (nestedHeader != null) {
                double totalColsWidth = 0;
                for (TableColumn<?, ?> col : table.getVisibleLeafColumns()) {
                    totalColsWidth += col.getWidth();
                }
                double viewWidth = table.getWidth();
                if (totalColsWidth > viewWidth) {
                    double minTranslateX = viewWidth - totalColsWidth;
                    double currentTranslateX = nestedHeader.getTranslateX();
                    double newTranslateX = Math.max(minTranslateX, Math.min(0.0, currentTranslateX + delta));
                    nestedHeader.setTranslateX(newTranslateX);
                }
            }
        }
    }
}
