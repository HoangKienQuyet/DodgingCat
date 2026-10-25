package com.dodgingcat.ui;

import java.util.Objects;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A short-lived pause screen that keeps the current game model untouched. */
public final class PauseView {

    private final int score;
    private final Runnable onResume;
    private final Runnable onMainMenu;
    private final EventHandler<KeyEvent> keyPressedHandler = this::onKeyPressed;
    private Scene scene;

    public PauseView(int score, Runnable onResume, Runnable onMainMenu) {
        this.score = score;
        this.onResume = Objects.requireNonNull(onResume, "onResume must not be null");
        this.onMainMenu = Objects.requireNonNull(onMainMenu, "onMainMenu must not be null");
    }

    public Parent createView() {
        StackPane root = new StackPane(CozyArtwork.createMenuBackdrop());
        VBox panel = new VBox(15,
                UiKit.label("GAME PAUSED", "screen-title"),
                UiKit.label("Your run is safely frozen.", "body-text"),
                UiKit.label("CURRENT SCORE   " + score, "score-value"),
                UiKit.menuButton("▶  RESUME", "primary-button", onResume),
                UiKit.menuButton("⌂  MAIN MENU", "blue-button", onMainMenu),
                UiKit.label("Press Esc to resume", "screen-kicker"));
        panel.getStyleClass().add("modal-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(360);
        root.getChildren().add(panel);
        root.setFocusTraversable(true);
        return root;
    }

    public void attachInput(Scene scene) {
        detachInput();
        this.scene = Objects.requireNonNull(scene, "scene must not be null");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
    }

    public void detachInput() {
        if (scene != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            scene = null;
        }
    }

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            event.consume();
            onResume.run();
        }
    }
}
