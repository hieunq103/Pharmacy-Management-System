package com.pharmacy.common;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class TogglePasswordField extends HBox {

    private static final String SVG_EYE_SLASH =
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7z" +
                    "M2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27z" +
                    "M7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2z";

    private static final String SVG_EYE_OPEN =
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5z" +
                    "M12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";

    private final PasswordField passwordField;
    private final TextField textField;
    private final SVGPath iconEye;

    // Bổ sung: Constructor rỗng cho FXML loader
    public TogglePasswordField() {
        this("");
    }

    public TogglePasswordField(String promptText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 12; -fx-pref-height: 44.0;");

        passwordField = new PasswordField();
        passwordField.setPromptText(promptText);

        textField = new TextField();
        textField.setPromptText(promptText);
        textField.setVisible(false);
        textField.setManaged(false);

        // Ràng buộc dữ liệu 2 chiều
        passwordField.textProperty().bindBidirectional(textField.textProperty());

        String inputStyle = "-fx-font-family: 'Inter 18pt'; -fx-background-color: transparent; -fx-text-fill: #0f172a; -fx-prompt-text-fill: #94a3b8; -fx-font-size: 15px;";
        passwordField.setStyle(inputStyle);
        textField.setStyle(inputStyle);

        StackPane stackInput = new StackPane(passwordField, textField);
        stackInput.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(passwordField, Pos.CENTER_LEFT);
        StackPane.setAlignment(textField, Pos.CENTER_LEFT);
        HBox.setHgrow(stackInput, Priority.ALWAYS);

        // Mặc định mắt gạch chéo vì mật khẩu đang che
        iconEye = new SVGPath();
        iconEye.setContent(SVG_EYE_SLASH);
        iconEye.setFill(Color.valueOf("#94a3b8"));
        iconEye.setScaleX(0.8);
        iconEye.setScaleY(0.8);

        Button btnToggle = new Button();
        btnToggle.setFocusTraversable(false);
        btnToggle.setGraphic(iconEye);
        btnToggle.setStyle("-fx-background-color: transparent; -fx-padding: 4; -fx-cursor: hand;");
        btnToggle.setOnAction(e -> toggleVisibility());

        getChildren().addAll(stackInput, btnToggle);
    }

    private void toggleVisibility() {
        boolean isMasked = passwordField.isVisible();
        if (isMasked) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            textField.setVisible(true);
            textField.setManaged(true);
            iconEye.setContent(SVG_EYE_OPEN);
            textField.requestFocus();
            textField.positionCaret(textField.getText() != null ? textField.getText().length() : 0);
        } else {
            textField.setVisible(false);
            textField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            iconEye.setContent(SVG_EYE_SLASH);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText() != null ? passwordField.getText().length() : 0);
        }
    }

    public String getText() {
        return passwordField.getText() != null ? passwordField.getText().trim() : "";
    }

    public void setText(String text) {
        passwordField.setText(text);
    }

    public void clear() {
        passwordField.clear();
    }

    public void setPromptText(String promptText) {
        passwordField.setPromptText(promptText);
        textField.setPromptText(promptText);
    }

    public String getPromptText() {
        return passwordField.getPromptText();
    }

    public javafx.beans.property.StringProperty promptTextProperty() {
        return passwordField.promptTextProperty();
    }

    public StringProperty textProperty() {
        return passwordField.textProperty();
    }

    @Override
    public void requestFocus() {
        if (textField.isVisible()) {
            textField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }
}