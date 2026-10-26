package com.pharmacy;

import com.pharmacy.auth.AuthService;
import com.pharmacy.auth.AuthServiceImpl;
import com.pharmacy.auth.LoginRequest;
import com.pharmacy.common.UserSession;
import com.pharmacy.infra.db.DatabaseHealthChecker;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.MigrationRunner;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Điểm khởi động ứng dụng desktop PharmaOS.
 * <p>
 * Lớp chứa {@code main} cố ý KHÔNG kế thừa {@link Application}: từ Java 11, nếu lớp main kế thừa
 * Application mà JavaFX không nằm trên module path (chạy thẳng từ IntelliJ/classpath) thì JVM báo
 * "JavaFX runtime components are missing". Vòng đời JavaFX nằm trong lớp lồng {@link App}.
 */
public class PharmacyApplication {

    private static final Logger log = LoggerFactory.getLogger(PharmacyApplication.class);

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    /** Vòng đời JavaFX (init / start / stop) của ứng dụng. */
    public static class App extends Application {

        @Override
        public void init() {
            // 1. Cấu hình bắt ngoại lệ chưa được xử lý trên toàn bộ Thread (R2)
            Thread.setDefaultUncaughtExceptionHandler(
                    (thread, ex) -> log.error("Uncaught exception trên thread '{}': ", thread.getName(), ex));

            // 2. Chạy Flyway migration trước để đồng bộ schema (R72)
            try {
                MigrationRunner.migrate();
            } catch (Exception e) {
                log.error("Khởi động thất bại do lỗi cấu trúc CSDL: ", e);
                Platform.exit();
                System.exit(1);
            }

            // 3. Khởi tạo Hibernate SessionFactory và HikariCP Pool
            try {
                HibernateUtil.init();
                log.info("Hibernate SessionFactory đã được khởi tạo thành công.");
            } catch (Exception e) {
                log.error("Khởi động thất bại do không kết nối được Hibernate SessionFactory: ", e);
                Platform.exit();
                System.exit(1);
            }
        }

        @Override
        public void start(Stage primaryStage) {
            loadFont("/fonts/Inter_18pt-Regular.ttf");
            loadFont("/fonts/Inter_18pt-SemiBold.ttf");

            DatabaseHealthChecker.startMonitoring(5, isAlive -> {
                if (!isAlive) {
                    log.warn("Mất kết nối tới máy chủ cơ sở dữ liệu! Vui lòng kiểm tra lại dịch vụ MySQL.");
                } else {
                    log.info("Kết nối cơ sở dữ liệu đã hoạt động bình thường.");
                }
            });

            try {

                boolean autoLoginDevMode = false;
                String fxmlPath;
                String title;

                if (autoLoginDevMode) {
                    AuthService authService = new AuthServiceImpl();
                    authService.login(new LoginRequest("PMS0001", "12345678"));
                    log.info("Đăng nhập Admin tự động thành công: {}", UserSession.getInstance().getEmployeeName());
                    fxmlPath = "/fxml/layout/main_layout.fxml";
                    title = "PharmaOS — Quản lý cửa hàng thuốc (Admin Mode)";
                } else {
                    fxmlPath = "/fxml/auth/login.fxml";
                    title = "PharmaOS — Quản lý cửa hàng thuốc";
                }

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

                // Graceful shutdown khi người dùng bấm nút đóng cửa sổ (R71)
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
