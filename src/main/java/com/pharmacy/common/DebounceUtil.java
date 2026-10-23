package com.pharmacy.common;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Tiện ích debounce cho ô tìm kiếm TextField trong JavaFX.
 * Giúp trì hoãn truy vấn cho đến khi người dùng ngừng gõ, tránh spam query
 * xuống DB.
 */
public final class DebounceUtil {

    private DebounceUtil() {
    }

    /**
     * Gắn debounce listener vào TextField.
     *
     * @param textField Ô nhập liệu cần bắt sự kiện
     * @param delay     Thời gian chờ (ví dụ: Duration.millis(350))
     * @param onAction  Callback thực hiện khi hết thời gian chờ hoặc nhấn Enter
     * @return PauseTransition instance đã được cấu hình
     */
    public static PauseTransition debounce(TextField textField, Duration delay, Consumer<String> onAction) {
        if (textField == null || onAction == null)
            return null;

        PauseTransition pause = new PauseTransition(delay != null ? delay : Duration.millis(350));
        pause.setOnFinished(e -> {
            String text = textField.getText() != null ? textField.getText().trim() : "";
            onAction.accept(text);
        });

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                pause.stop();
                onAction.accept("");
            } else {
                pause.playFromStart();
            }
        });

        textField.setOnAction(e -> {
            pause.stop();
            String text = textField.getText() != null ? textField.getText().trim() : "";
            onAction.accept(text);
        });

        return pause;
    }
}
