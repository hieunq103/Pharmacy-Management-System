package com.pharmacy;

import com.pharmacy.infra.config.AppConfig;
import com.pharmacy.infra.db.DatabaseHealthChecker;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.MigrationRunner;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PharmacyApplication {

    private static final Logger log = LoggerFactory.getLogger(PharmacyApplication.class);

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    public static class App extends Application {

        private String startupError;

        @Override
        public void init() {
            Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
                log.error("Uncaught exception trên thread '{}': ", thread.getName(), ex)
            );

            if (AppConfig.autoMigrate()) {
                try {
                    MigrationRunner.migrate();
                } catch (Exception e) {
                    log.error("Khởi động thất bại do lỗi cấu trúc CSDL: ", e);
                    startupError = "Không thể cập nhật cấu trúc cơ sở dữ liệu.";
                    return;
                }
            }

            try {
                HibernateUtil.init();
                log.info("Hibernate SessionFactory đã được khởi tạo thành công.");
            } catch (Exception e) {
                log.error("Khởi động thất bại do không kết nối được Hibernate SessionFactory: ", e);
                startupError =
                    "Không thể kết nối tới máy chủ dữ liệu. Vui lòng kiểm tra kết nối mạng rồi mở lại ứng dụng.";
            }
        }

        @Override
        public void start(Stage primaryStage) {
            if (startupError != null) {
                com.pharmacy.common.DialogUtils.showError(
                    "Không thể khởi động PharmaOS",
                    startupError + "\n\nChi tiết lỗi được ghi tại thư mục .pharmaos/logs trong thư mục người dùng."
                );
                Platform.exit();
                return;
            }
            loadFont("/fonts/Inter_18pt-Medium.ttf");
            loadFont("/fonts/Inter_18pt-Bold.ttf");

            com.pharmacy.infra.MemoryMonitor.start();
            DatabaseHealthChecker.startMonitoring(
                Integer.parseInt(AppConfig.get("db.healthCheckSeconds", "30")),
                isAlive -> {
                    if (!isAlive) {
                        log.warn("Mất kết nối tới máy chủ cơ sở dữ liệu! Vui lòng kiểm tra lại dịch vụ MySQL.");
                    } else {
                        log.info("Kết nối cơ sở dữ liệu đã hoạt động bình thường.");
                    }
                }
            );

            try {
                String fxmlPath = "/fxml/auth/login.fxml";
                String title = "PharmaOS — Quản lý cửa hàng thuốc";

                URL fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null) {
                    throw new IOException("Không tìm thấy file giao diện " + fxmlPath);
                }

                Parent root = FXMLLoader.load(fxmlUrl);
                primaryStage.setTitle(title);

                Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
                double targetWidth = Math.min(1280, visualBounds.getWidth());
                double targetHeight = Math.min(760, visualBounds.getHeight() - 15);

                Scene scene = new Scene(root, targetWidth, targetHeight);
                URL cssResource = getClass().getResource("/css/app.css");
                if (cssResource != null) {
                    scene.getStylesheets().add(cssResource.toExternalForm());
                }

                primaryStage.setScene(scene);
                primaryStage.setWidth(targetWidth);
                primaryStage.setHeight(targetHeight);
                primaryStage.setMinWidth(1024);
                primaryStage.setMinHeight(580);
                primaryStage.setResizable(true);
                primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - targetWidth) / 2);
                primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - targetHeight) / 2);
                com.pharmacy.common.UiScaleManager.getInstance().install(scene);

                primaryStage.setOnCloseRequest(event -> {
                    log.info("Nhận tín hiệu đóng cửa sổ chính, đang dọn dẹp tài nguyên...");
                });

                primaryStage.show();
            } catch (Exception e) {
                log.error("Không thể mở màn hình ứng dụng: ", e);
                showStartupErrorAlert(e.getMessage());
            }
        }

        @Override
        public void stop() {
            log.info("Bắt đầu giải phóng tài nguyên hệ thống...");
            HibernateUtil.shutdown();
            log.info("PharmaOS đã tắt an toàn.");
        }

        private Font loadFont(String path) {
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in == null) {
                    log.warn("Không tìm thấy file font: {}", path);
                    return null;
                }
                return Font.loadFont(in, 14);
            } catch (IOException e) {
                log.warn("Lỗi khi nạp font {}: ", path, e);
                return null;
            }
        }

        private void showStartupErrorAlert(String detail) {
            com.pharmacy.common.DialogUtils.showError("Lỗi khởi động", "Không thể khởi động ứng dụng: " + detail);
        }
    }
}
