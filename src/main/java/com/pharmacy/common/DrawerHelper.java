package com.pharmacy.common;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Tiện ích quản lý hiệu ứng mở/đóng Drawer trượt từ cạnh phải và màn che mờ (Overlay).
 * Dùng chung giữa Thêm nhân viên và Nhập/Xuất kho.
 */
public class DrawerHelper {

    private StackPane appRootPane;
    private Region overlayRegion;
    private Parent activeDrawerRoot;
    private Runnable onCloseCallback;

    /**
     * Khởi tạo hạ tầng overlay trên Scene hiện tại.
     * Tự động bọc Root thành StackPane nếu chưa có.
     */
    public void init(Scene scene, Runnable onClose) {
        if (scene == null || appRootPane != null) return;
        this.onCloseCallback = onClose;

        Parent currentRoot = scene.getRoot();
        if (currentRoot instanceof StackPane stack) {
            appRootPane = stack;
        } else {
            appRootPane = new StackPane();
            scene.setRoot(appRootPane);
            appRootPane.getChildren().add(currentRoot);
        }

        overlayRegion = new Region();
        overlayRegion.getStyleClass().add("drawer-overlay");
        overlayRegion.prefWidthProperty().bind(scene.widthProperty());
        overlayRegion.prefHeightProperty().bind(scene.heightProperty());
        overlayRegion.setVisible(false);
        overlayRegion.setOpacity(0.0);
        overlayRegion.setOnMouseClicked(e -> close());

        appRootPane.getChildren().add(overlayRegion);
    }

    /**
     * Đăng ký một Drawer vào StackPane.
     */
    public void registerDrawer(Parent drawerRoot) {
        if (appRootPane == null || drawerRoot == null) return;
        if (!appRootPane.getChildren().contains(drawerRoot)) {
            drawerRoot.setVisible(false);
            StackPane.setAlignment(drawerRoot, Pos.CENTER_RIGHT);
            appRootPane.getChildren().add(drawerRoot);
        }
    }

    /**
     * Mở Drawer với hiệu ứng trượt từ phải sang và mờ dần màn che.
     */
    public void open(Parent drawerRoot) {
        if (drawerRoot == null || overlayRegion == null) return;
        this.activeDrawerRoot = drawerRoot;

        overlayRegion.toFront();
        drawerRoot.toFront();

        overlayRegion.setVisible(true);
        drawerRoot.setVisible(true);

        double width = (drawerRoot instanceof Region r && r.getPrefWidth() > 0) ? r.getPrefWidth() : AppConstants.DRAWER_WIDTH;
        TranslateTransition slideIn = new TranslateTransition(AppConstants.DRAWER_ANIMATION_DURATION, drawerRoot);
        slideIn.setFromX(width);
        slideIn.setToX(0.0);

        FadeTransition fadeIn = new FadeTransition(AppConstants.DRAWER_ANIMATION_DURATION, overlayRegion);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        new ParallelTransition(slideIn, fadeIn).play();
    }

    /**
     * Đóng Drawer với hiệu ứng trượt sang phải và ẩn màn che.
     */
    public void close() {
        if (activeDrawerRoot == null || overlayRegion == null) return;

        final Parent drawer = activeDrawerRoot;
        double width = (drawer instanceof Region r && r.getPrefWidth() > 0) ? r.getPrefWidth() : AppConstants.DRAWER_WIDTH;

        TranslateTransition slideOut = new TranslateTransition(AppConstants.DRAWER_CLOSE_DURATION, drawer);
        slideOut.setFromX(0.0);
        slideOut.setToX(width);

        FadeTransition fadeOut = new FadeTransition(AppConstants.DRAWER_CLOSE_DURATION, overlayRegion);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition parallel = new ParallelTransition(slideOut, fadeOut);
        parallel.setOnFinished(e -> {
            drawer.setVisible(false);
            overlayRegion.setVisible(false);
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        parallel.play();
    }

}
