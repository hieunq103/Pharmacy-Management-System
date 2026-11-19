package com.pharmacy.common;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lớp phủ "Đang tải..." (vòng tròn quay) đặt đè lên vùng dữ liệu khi tìm kiếm/lọc/chuyển trang.
 * Chỉ hiện nếu tác vụ kéo dài hơn {@value #SHOW_DELAY_MS} ms để tránh nháy khi tải nhanh.
 */
public final class LoadingOverlay {

    private static final int SHOW_DELAY_MS = 150;

    private final VBox box = new VBox(10);
    private final PauseTransition delay = new PauseTransition(Duration.millis(SHOW_DELAY_MS));
    private int pending;

    private LoadingOverlay() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        spinner.setMaxSize(48, 48);
        spinner.setStyle("-fx-progress-color: #0066ff;");
        Label text = new Label("Đang tải...");
        text.setStyle("-fx-font-family: 'Inter 18pt Bold'; -fx-font-size: 14px; -fx-text-fill: #475569;");
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(248, 250, 252, 0.72);");
        box.getChildren().addAll(spinner, text);
        box.setVisible(false);
        box.setManaged(false);
        delay.setOnFinished(e -> {
            if (pending > 0) {
                box.setVisible(true);
                box.setManaged(true);
            }
        });
    }

    /**
     * Bọc vùng chứa dữ liệu quanh {@code target} bằng StackPane kèm lớp phủ. Nếu không bọc được
     * (cha không phải Pane thông thường) thì trả về đối tượng vô hại, không hiển thị gì.
     */
    public static LoadingOverlay over(Node target) {
        LoadingOverlay overlay = new LoadingOverlay();
        Node node = target;
        while (node != null && !(node.getParent() instanceof Pane)) {
            node = node.getParent();
        }
        if (
            node == null ||
            node.getParent() instanceof javafx.scene.layout.GridPane ||
            node.getParent() instanceof javafx.scene.layout.BorderPane
        ) {
            return overlay;
        }
        Pane parent = (Pane) node.getParent();
        int index = parent.getChildren().indexOf(node);
        if (index < 0) return overlay;

        StackPane stack = new StackPane();
        Insets margin;
        if (parent instanceof VBox) {
            VBox.setVgrow(stack, VBox.getVgrow(node));
            margin = VBox.getMargin(node);
            VBox.setMargin(stack, margin);
            VBox.setMargin(node, null);
            VBox.setVgrow(node, null);
        } else if (parent instanceof HBox) {
            HBox.setHgrow(stack, HBox.getHgrow(node));
            margin = HBox.getMargin(node);
            HBox.setMargin(stack, margin);
            HBox.setMargin(node, null);
            HBox.setHgrow(node, null);
        }
        stack.setMinSize(0, 0);
        parent.getChildren().set(index, stack);
        stack.getChildren().setAll(node, overlay.box);
        return overlay;
    }

    public void track(Task<?> task) {
        pending++;
        if (pending == 1 && !box.isVisible()) {
            delay.playFromStart();
        }
        task.stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED || state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                pending = Math.max(0, pending - 1);
                if (pending == 0) {
                    delay.stop();
                    box.setVisible(false);
                    box.setManaged(false);
                }
            }
        });
    }
}
