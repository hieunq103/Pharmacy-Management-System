package com.pharmacy.employee;

import com.pharmacy.common.DialogUtils;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

// Hiện tên đăng nhập + mật khẩu tạm của nhân viên vừa tạo để quản lý sao chép gửi cho nhân viên.
// Mật khẩu tạm không lưu ở đâu khác nên chỉ xem được một lần tại đây
public final class NewAccountDialog {
    private static final String FIELD_STYLE = "-fx-font-family: 'Inter 18pt SemiBold'; -fx-font-size: 15px; -fx-text-fill: #0F172A; "
            + "-fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 10;";
    private static final String COPY_STYLE = "-fx-background-color: #EFF6FF; -fx-text-fill: #0066FF; -fx-font-family: 'Inter 18pt SemiBold'; "
            + "-fx-border-color: #BFDBFE; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-cursor: hand;";

    private NewAccountDialog() {
    }

    public static void show(Window owner, String fullName, NewEmployeeAccount account) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.configure(dialog, owner);
        dialog.setTitle("Tạo tài khoản thành công");
        dialog.setHeaderText("Đã tạo tài khoản cho nhân viên " + fullName);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        addRow(grid, 0, "Tên đăng nhập", account.username());
        addRow(grid, 1, "Mật khẩu", account.temporaryPassword());

        Label note = new Label("Mật khẩu chỉ hiển thị một lần. Hãy sao chép và gửi cho nhân viên; "
                + "nhân viên phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #B45309; -fx-font-size: 12.5px;");

        VBox content = new VBox(14, grid, note);
        content.setPadding(new Insets(6, 4, 4, 4));
        content.setPrefWidth(460);
        dialog.getDialogPane().setContent(content);

        ButtonType copyAll = new ButtonType("Sao chép cả hai", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(copyAll, ButtonType.CLOSE);
        Button copyAllButton = (Button) dialog.getDialogPane().lookupButton(copyAll);
        // Giữ hộp thoại mở khi bấm sao chép
        copyAllButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            copy("Tên đăng nhập: " + account.username() + "\nMật khẩu: " + account.temporaryPassword());
            flash(copyAllButton, "Sao chép cả hai");
        });
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Đóng");
        dialog.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setMinWidth(Label.USE_PREF_SIZE);
        lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13.5px;");

        TextField field = new TextField(value);
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.setStyle(FIELD_STYLE);
        GridPane.setHgrow(field, Priority.ALWAYS);

        Button copyButton = new Button("Sao chép");
        copyButton.setMinWidth(Button.USE_PREF_SIZE);
        copyButton.setStyle(COPY_STYLE);
        copyButton.setOnAction(e -> {
            copy(value);
            flash(copyButton, "Sao chép");
        });

        GridPane.setValignment(lbl, javafx.geometry.VPos.CENTER);
        grid.addRow(row, lbl, field, copyButton);
        grid.setAlignment(Pos.CENTER_LEFT);
    }

    private static void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static void flash(Button button, String original) {
        button.setText("Đã sao chép ✓");
        PauseTransition back = new PauseTransition(Duration.seconds(1.5));
        back.setOnFinished(e -> button.setText(original));
        back.play();
    }
}
