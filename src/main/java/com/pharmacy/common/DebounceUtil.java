package com.pharmacy.common;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.function.Consumer;

public final class DebounceUtil {
    private DebounceUtil() {
    }

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
