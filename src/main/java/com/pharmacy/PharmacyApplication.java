package com.pharmacy;

import com.pharmacy.infra.db.DatabaseHealthChecker;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.MigrationRunner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Điểm khởi động ứng dụng desktop PharmaOS.
 *
 * Trách nhiệm:
 *  1. Cấu hình uncaught-exception handler toàn cục (mục 13.1, R2)
 *  2. Chạy Flyway migration + khởi tạo Hibernate SessionFactory (mục 13.8, R72)
 *  3. Kích hoạt giám sát kết nối cơ sở dữ liệu ngầm (DatabaseHealthChecker)
 *  4. Mở màn hình đăng nhập (fxml/auth/login.fxml)
 *  5. Thực hiện đóng an toàn tài nguyên (Graceful Shutdown - R71)
 */
public class PharmacyApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(PharmacyApplication.class);

    @Override
    public void init() {
        // 1. Cấu hình bắt ngoại lệ chưa được xử lý trên toàn bộ Thread (R2)
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
                log.error("Uncaught exception trên thread '{}': ", thread.getName(), ex));

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
        // Nạp font TRƯỚC khi load FXML
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
            URL loginFxml = getClass().getResource("/fxml/auth/login.fxml");
            if (loginFxml == null) {
                throw new IOException("Không tìm thấy file giao diện /fxml/auth/login.fxml");
            }

            Parent root = FXMLLoader.load(loginFxml);
            primaryStage.setTitle("PharmaOS — Quản lý cửa hàng thuốc");

            // Giới hạn kích thước sàn khi người dùng kéo co nhỏ lại
            primaryStage.setMinWidth(880);
            primaryStage.setMinHeight(540);

            // Tạo Scene với kích thước chuẩn 1160x720
            Scene scene = new Scene(root, 1160, 720);

            // Nạp file app.css toàn cục
            URL cssResource = getClass().getResource("/css/app.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                log.warn("Không tìm thấy file CSS: /css/app.css");
            }

            // Gán đúng biến 'scene' đã có CSS vào Stage
            primaryStage.setScene(scene);
            primaryStage.setWidth(1160);
            primaryStage.setHeight(720);
            primaryStage.centerOnScreen();

            // Graceful shutdown khi người dùng bấm nút đóng cửa sổ (R71)
            primaryStage.setOnCloseRequest(event -> {
                log.info("Nhận tín hiệu đóng cửa sổ chính, đang dọn dẹp tài nguyên...");
            });

            primaryStage.show();
        } catch (Exception e) {
            log.error("Không thể mở màn hình đăng nhập: ", e);
            showStartupErrorAlert(e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Đóng toàn bộ Connection Pool và Hibernate khi ứng dụng kết thúc
        log.info("Bắt đầu giải phóng tài nguyên hệ thống...");
        HibernateUtil.shutdown();
        log.info("PharmaOS đã tắt an toàn.");
    }

    /** Nạp font từ classpath, trả về null (và log cảnh báo) nếu không tìm thấy. */
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi khởi động");
        alert.setHeaderText("Không thể nạp giao diện đăng nhập");
        alert.setContentText("Chi tiết: " + detail);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}