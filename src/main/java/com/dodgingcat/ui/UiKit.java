package com.dodgingcat.ui;

import java.net.URL;
import java.util.Objects;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Shared construction and stylesheet helpers for the application screens. */
public final class UiKit {

    private static final String STYLESHEET = "/com/dodgingcat/ui/dodging-cat.css";

    private UiKit() {
    }

    public static void applyTheme(Scene scene) {
        URL stylesheet = UiKit.class.getResource(STYLESHEET);
        if (stylesheet == null) {
            return;
        }
        String stylesheetUrl = stylesheet.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheetUrl)) {
            scene.getStylesheets().add(stylesheetUrl);
        }
    }

    public static Button menuButton(String text, String colorClass, Runnable action) {
        Button button = button(text, colorClass, action);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    public static Button compactButton(String text, String colorClass, Runnable action) {
        Button button = button(text, colorClass, action);
        button.getStyleClass().add("compact-button");
        return button;
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static Button button(String text, String colorClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("action-button", colorClass);
        button.setOnAction(event -> Objects.requireNonNull(action, "action must not be null").run());
        return button;
    }
}
