package com.pharmacy.common;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

public final class DialogUtils {
    private DialogUtils() {}

    public static Window getActiveWindow() {
        List<Window> windows = Window.getWindows();
        for (Window w : windows) {
            if (w.isShowing() && w.isFocused()) {
                return w;
            }
        }
        for (Window w : windows) {
            if (w.isShowing() && w instanceof Stage) {
                return w;
            }
        }
        return null;
    }

    public static void configure(Dialog<?> dialog, Window explicitOwner) {
        if (dialog == null) return;
        Window owner = (explicitOwner != null) ? explicitOwner : getActiveWindow();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        try {
            var css = DialogUtils.class.getResource("/css/app.css");
            if (css != null && dialog.getDialogPane() != null) {
                dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    public static void showInfo(String title, String message) {
        showInfo(null, title, message);
    }

    public static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        configure(alert, owner);
        alert.setTitle(title != null ? title : "Thông tin");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showSuccess(Window owner, String title, String message) {
        showInfo(owner, title != null ? title : "Thành công", message);
    }

    public static void showWarning(String title, String message) {
        showWarning(null, title, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        configure(alert, owner);
        alert.setTitle(title != null ? title : "Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        configure(alert, owner);
        alert.setTitle(title != null ? title : "Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(b -> b == javafx.scene.control.ButtonType.OK).isPresent();
    }

    public static void showError(String title, String message) {
        showError(null, title, message);
    }

    public static void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        configure(alert, owner);
        alert.setTitle(title != null ? title : "Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
