package com.dodgingcat.ui;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Styled, navigable holding screen for features scheduled for later milestones. */
public final class FeatureInfoView {

    private final String title;
    private final String message;
    private final Runnable onBack;

    public FeatureInfoView(String title, String message, Runnable onBack) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.onBack = Objects.requireNonNull(onBack, "onBack must not be null");
    }

    public Parent createView() {
        StackPane root = new StackPane(CozyArtwork.createMenuBackdrop());
        VBox panel = new VBox(16,
                UiKit.label("DODGING CAT", "screen-kicker"),
                UiKit.label(title, "screen-title"),
                UiKit.label(message, "body-text"),
                UiKit.compactButton("←  BACK TO MENU", "blue-button", onBack));
        panel.getStyleClass().add("placeholder-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(510);
        panel.setPadding(new Insets(30));
        root.getChildren().add(panel);
        return root;
    }
}
