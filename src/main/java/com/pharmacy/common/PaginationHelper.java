package com.pharmacy.common;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public final class PaginationHelper {
    private PaginationHelper() {}

    public static void render(
            HBox paginationBox,
            Label lblRecordRange,
            int currentPage,
            int totalPages,
            long totalRecords,
            int pageSize,
            Consumer<Integer> onPageChange
    ) {
        if (paginationBox == null) return;
        paginationBox.getChildren().clear();

        int effectiveTotalPages = totalPages <= 0 ? 1 : totalPages;
        int effectiveCurrentPage = Math.max(1, Math.min(currentPage, effectiveTotalPages));

        if (lblRecordRange != null) {
            if (totalRecords <= 0) {
                lblRecordRange.setText("0 - 0");
            } else {
                int fromIndex = (effectiveCurrentPage - 1) * pageSize + 1;
                int toIndex = (int) Math.min((long) effectiveCurrentPage * pageSize, totalRecords);
                lblRecordRange.setText(String.format("%d - %d", fromIndex, toIndex));
            }
        }

        Button btnPrev = new Button("<");
        btnPrev.getStyleClass().add("page-btn");
        btnPrev.setDisable(effectiveCurrentPage <= 1);
        btnPrev.setOnAction(e -> {
            if (effectiveCurrentPage > 1 && onPageChange != null) {
                onPageChange.accept(effectiveCurrentPage - 1);
            }
        });
        paginationBox.getChildren().add(btnPrev);

        if (effectiveTotalPages <= 7) {
            for (int i = 1; i <= effectiveTotalPages; i++) {
                addPageButton(paginationBox, i, effectiveCurrentPage, onPageChange);
            }
        } else {
            if (effectiveCurrentPage <= 4) {
                for (int i = 1; i <= 5; i++) {
                    addPageButton(paginationBox, i, effectiveCurrentPage, onPageChange);
                }
                addEllipsis(paginationBox);
                addPageButton(paginationBox, effectiveTotalPages, effectiveCurrentPage, onPageChange);
            } else if (effectiveCurrentPage >= effectiveTotalPages - 3) {
                addPageButton(paginationBox, 1, effectiveCurrentPage, onPageChange);
                addEllipsis(paginationBox);
                for (int i = effectiveTotalPages - 4; i <= effectiveTotalPages; i++) {
                    addPageButton(paginationBox, i, effectiveCurrentPage, onPageChange);
                }
            } else {
                addPageButton(paginationBox, 1, effectiveCurrentPage, onPageChange);
                addEllipsis(paginationBox);
                addPageButton(paginationBox, effectiveCurrentPage - 1, effectiveCurrentPage, onPageChange);
                addPageButton(paginationBox, effectiveCurrentPage, effectiveCurrentPage, onPageChange);
                addPageButton(paginationBox, effectiveCurrentPage + 1, effectiveCurrentPage, onPageChange);
                addEllipsis(paginationBox);
                addPageButton(paginationBox, effectiveTotalPages, effectiveCurrentPage, onPageChange);
            }
        }

        Button btnNext = new Button(">");
        btnNext.getStyleClass().add("page-btn");
        btnNext.setDisable(effectiveCurrentPage >= effectiveTotalPages);
        btnNext.setOnAction(e -> {
            if (effectiveCurrentPage < effectiveTotalPages && onPageChange != null) {
                onPageChange.accept(effectiveCurrentPage + 1);
            }
        });
        paginationBox.getChildren().add(btnNext);
    }

    private static void addPageButton(HBox box, int pageIndex, int currentPage, Consumer<Integer> onPageChange) {
        Button btn = new Button(String.valueOf(pageIndex));
        btn.getStyleClass().add("page-btn");
        if (pageIndex == currentPage) {
            btn.getStyleClass().add("page-btn-active");
        }
        btn.setOnAction(e -> {
            if (currentPage != pageIndex && onPageChange != null) {
                onPageChange.accept(pageIndex);
            }
        });
        box.getChildren().add(btn);
    }

    private static void addEllipsis(HBox box) {
        Label lbl = new Label("...");
        lbl.getStyleClass().add("page-dots");
        box.getChildren().add(lbl);
    }
}
