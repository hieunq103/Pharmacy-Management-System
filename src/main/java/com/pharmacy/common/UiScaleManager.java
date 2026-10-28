package com.pharmacy.common;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public final class UiScaleManager {
    private static final Logger log = LoggerFactory.getLogger(UiScaleManager.class);
    private static final UiScaleManager INSTANCE = new UiScaleManager();

    private double currentScale = 1.0;
    private File dynamicCssFile = null;
    private final PauseTransition resizeDebounce = new PauseTransition(Duration.millis(50));

    private UiScaleManager() {
        try {
            dynamicCssFile = File.createTempFile("pharma_scale_", ".css");
            dynamicCssFile.deleteOnExit();
        } catch (IOException e) {
            log.error("Không thể khởi tạo file CSS động cho Scale Manager: ", e);
        }
    }

    public static UiScaleManager getInstance() {
        return INSTANCE;
    }

    public void install(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean isShortcut = event.isShortcutDown() || event.isControlDown() || event.isMetaDown();
            if (!isShortcut) return;

            KeyCode code = event.getCode();
            if (code == KeyCode.EQUALS || code == KeyCode.PLUS || code == KeyCode.ADD) {
                zoomIn(scene);
                event.consume();
            } else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
                zoomOut(scene);
                event.consume();
            } else if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
                resetZoom(scene);
                event.consume();
            }
        });
        scene.widthProperty().addListener((obs, oldW, newW) -> handleResize(scene));
        scene.heightProperty().addListener((obs, oldH, newH) -> handleResize(scene));
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin instanceof Stage stage) {
                setupStageListener(stage, scene);
            }
        });
        if (scene.getWindow() instanceof Stage stage) {
            setupStageListener(stage, scene);
        }

        Platform.runLater(() -> autoScaleScene(scene));
    }

    private void setupStageListener(Stage stage, Scene scene) {
        if (stage == null) return;

        stage.widthProperty().addListener((obs, oldW, newW) -> handleResize(scene));
        stage.heightProperty().addListener((obs, oldH, newH) -> handleResize(scene));
        stage.maximizedProperty().addListener((obs, oldM, newM) -> handleResize(scene));
        stage.fullScreenProperty().addListener((obs, oldF, newF) -> handleResize(scene));
        stage.showingProperty().addListener((obs, oldS, isShowing) -> {
            if (isShowing) {
                Platform.runLater(() -> autoScaleScene(scene));
            }
        });
    }

    private void handleResize(Scene scene) {
        resizeDebounce.setOnFinished(e -> autoScaleScene(scene));
        resizeDebounce.playFromStart();
    }

    public void autoScaleScene(Scene scene) {
        if (scene == null) return;

        double w = scene.getWidth();
        double h = scene.getHeight();

        if (w <= 0 || h <= 0) {
            if (scene.getWindow() instanceof Stage stage) {
                w = stage.getWidth();
                h = stage.getHeight();
            }
        }
        if (w <= 0 || h <= 0) return;
        double widthRatio = w / 1280.0;
        double heightRatio = h / 800.0;
        double dimRatio = Math.min(widthRatio, heightRatio);
        double calculatedScale = 1.0 + (dimRatio - 1.0) * 0.85;
        calculatedScale = Math.max(0.85, Math.min(2.2, calculatedScale));
        calculatedScale = Math.round(calculatedScale * 100.0) / 100.0;
        if (Math.abs(calculatedScale - currentScale) >= 0.02) {
            setScale(calculatedScale, scene);
        }
    }

    public void zoomIn(Scene scene) {
        double newScale = Math.min(2.2, currentScale + 0.15);
        setScale(newScale, scene);
    }

    public void zoomOut(Scene scene) {
        double newScale = Math.max(0.85, currentScale - 0.15);
        setScale(newScale, scene);
    }

    public void resetZoom(Scene scene) {
        autoScaleScene(scene);
    }

    public void setScale(double scale, Scene scene) {
        this.currentScale = Math.round(scale * 100.0) / 100.0;
        applyScale(this.currentScale, scene);
    }

    private void applyScale(double scale, Scene scene) {
        if (dynamicCssFile == null || scene == null) return;

        try {
            int baseFont = (int) Math.round(14.0 * scale);
            int statFont = (int) Math.round(18.0 * scale);
            int titleFont = (int) Math.round(19.0 * scale);
            int tableCellFont = (int) Math.round(15.0 * scale);
            int headerFont = (int) Math.round(15.0 * scale);
            int inputFont = (int) Math.round(13.5 * scale);
            int btnFont = (int) Math.round(14.0 * scale);
            int navFont = (int) Math.round(16.0 * scale);
            int paddingY = (int) Math.round(8.0 * scale);
            int paddingX = (int) Math.round(14.0 * scale);

            String css = String.format(Locale.US,
                ".root { -fx-font-size: %dpx; }\n" +
                ".stat-label-primary, .stat-label-secondary { -fx-font-size: %dpx; }\n" +
                "#lblHeaderTitle { -fx-font-size: %dpx; }\n" +
                ".custom-inventory-table .table-cell, .custom-employee-table .table-cell { -fx-font-size: %dpx; }\n" +
                ".custom-inventory-table .column-header .label, .custom-employee-table .column-header .label { -fx-font-size: %dpx; }\n" +
                ".search-input { -fx-font-size: %dpx; -fx-padding: %d %d; }\n" +
                ".btn-primary-import, .btn-secondary-export, .btn-primary-add { -fx-font-size: %dpx; -fx-padding: %d %d; }\n" +
                ".nav-button { -fx-font-size: %dpx; }\n",
                baseFont,
                statFont,
                titleFont,
                tableCellFont,
                headerFont,
                inputFont, paddingY, paddingX,
                btnFont, paddingY, paddingX,
                navFont
            );

            Files.writeString(dynamicCssFile.toPath(), css);

            String fileUri = dynamicCssFile.toURI().toURL().toExternalForm();
            scene.getStylesheets().remove(fileUri);
            scene.getStylesheets().add(fileUri);
        } catch (Exception e) {
            log.error("Lỗi khi áp dụng tỉ lệ UI: ", e);
        }
    }
}
