package com.dodgingcat.ui;

import java.net.URL;
import java.util.Objects;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/** Shared construction and stylesheet helpers for the application screens. */
public final class UiKit {

    private static final String STYLESHEET = "/com/dodgingcat/ui/dodging-cat.css";
    private static final ClasspathImageLoader PIXEL_ASSETS = new ClasspathImageLoader();

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

    /** Creates a compact text button that gains an optional standalone pixel-art icon. */
    public static Button compactButtonWithOptionalIcon(String text, String colorClass,
                                                        String iconPath, Runnable action) {
        Button button = compactButton(text, colorClass, action);
        PIXEL_ASSETS.loadPng(iconPath).ifPresent(image -> {
            ImageView icon = new ImageView(image);
            icon.setFitWidth(14);
            icon.setFitHeight(14);
            icon.setPreserveRatio(true);
            button.setGraphic(icon);
        });
        return button;
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Creates a text label that gains an optional standalone pixel-art icon when it is available. */
    public static Label labelWithOptionalIcon(String text, String styleClass, String iconPath) {
        Label label = label(text, styleClass);
        PIXEL_ASSETS.loadPng(iconPath).ifPresent(image -> {
            ImageView icon = new ImageView(image);
            icon.setFitWidth(16);
            icon.setFitHeight(16);
            icon.setPreserveRatio(true);
            label.setGraphic(icon);
        });
        return label;
    }

    private static Button button(String text, String colorClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("action-button", colorClass);
        PixelAssetPaths.buttonForStyle(colorClass)
                .flatMap(PIXEL_ASSETS::loadPng)
                .ifPresent(image -> button.setBackground(new Background(new BackgroundImage(image,
                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(100, 100, true, true, false, false)))));
        button.setOnAction(event -> Objects.requireNonNull(action, "action must not be null").run());
        return button;
    }
}
