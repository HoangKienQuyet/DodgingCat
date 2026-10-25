package com.dodgingcat.ui;

import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Displays the final score and actions available after a run. */
public final class GameOverView {

    private final int finalScore;
    private final int highScore;
    private final boolean newRecord;
    private final Runnable onRestart;
    private final Runnable onMainMenu;

    public GameOverView(int finalScore, int highScore, boolean newRecord,
                        Runnable onRestart, Runnable onMainMenu) {
        this.finalScore = finalScore;
        this.highScore = highScore;
        this.newRecord = newRecord;
        this.onRestart = Objects.requireNonNull(onRestart);
        this.onMainMenu = Objects.requireNonNull(onMainMenu);
    }

    public Parent createView() {
        StackPane root = new StackPane(CozyArtwork.createMenuBackdrop());
        VBox panel = new VBox(13,
                UiKit.label("ONE LIFE • ONE MORE TRY", "screen-kicker"),
                UiKit.label("GAME OVER", "screen-title"),
                UiKit.label("FINAL SCORE   " + finalScore, "score-value"),
                UiKit.label("BEST SCORE   " + highScore, "body-text"),
                UiKit.label(newRecord ? "★ NEW PERSONAL BEST! ★" : "Dodge smarter. Jump higher.",
                        newRecord ? "record-text" : "body-text"),
                UiKit.menuButton("↻  RETRY", "primary-button", onRestart),
                UiKit.menuButton("⌂  MAIN MENU", "blue-button", onMainMenu));
        panel.getStyleClass().add("modal-panel");
        panel.setMaxWidth(380);
        panel.setAlignment(Pos.CENTER);
        root.getChildren().add(panel);
        return root;
    }
}
