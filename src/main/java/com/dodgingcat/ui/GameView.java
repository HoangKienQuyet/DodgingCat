package com.dodgingcat.ui;

import com.dodgingcat.game.GameEngine;
import com.dodgingcat.game.InputController;
import com.dodgingcat.util.Constants;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX HUD and drawing surface for an active game session. */
public final class GameView {

    private final Runnable onMainMenu;
    private final Runnable onPause;
    private final int bestScore;
    private final InputController input = new InputController();
    private final GameCanvasRenderer renderer = new GameCanvasRenderer();
    private final BorderPane root = new BorderPane();
    private final Canvas canvas = new Canvas(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
    private final Label scoreLabel = UiKit.label("0", "hud-value");
    private final Label bestScoreLabel = UiKit.label("0", "hud-value");
    private final Label timeLabel = UiKit.label("00:00", "hud-value");
    private final Label levelLabel = UiKit.label("1", "hud-value");

    public GameView(int bestScore, Runnable onMainMenu, Runnable onPause) {
        this.bestScore = bestScore;
        this.onMainMenu = Objects.requireNonNull(onMainMenu, "onMainMenu must not be null");
        this.onPause = Objects.requireNonNull(onPause, "onPause must not be null");
        root.getStyleClass().add("game-screen");
        root.setTop(createHud());
        root.setCenter(canvas);
        root.setFocusTraversable(true);
    }

    public Parent createView() {
        return root;
    }

    public void attachInput(Scene scene) {
        input.attach(scene);
        root.requestFocus();
    }

    public void detachInput() {
        input.detach();
    }

    public InputController getInputController() {
        return input;
    }

    public void render(GameEngine engine) {
        renderer.render(canvas.getGraphicsContext2D(), engine);
        scoreLabel.setText("Score: " + engine.getScore());
        bestScoreLabel.setText("Best: " + Math.max(bestScore, engine.getScore()));
        timeLabel.setText(formatTime(engine.getElapsedSeconds()));
        levelLabel.setText("Difficulty: " + engine.getDifficultyLevel());
    }

    private HBox createHud() {
        VBox lifeCard = hudCard("♥  ONE LIFE", "NO REVIVES", "life-value",
                PixelAssetPaths.UI_ICON_LIFE);
        VBox scoreCard = hudCard("SCORE", "", null, null);
        scoreCard.getChildren().set(1, scoreLabel);
        VBox bestCard = hudCard("BEST", "", null, null);
        bestCard.getChildren().set(1, bestScoreLabel);
        VBox timeCard = hudCard("SURVIVAL", "", null, null);
        timeCard.getChildren().set(1, timeLabel);
        VBox levelCard = hudCard("LEVEL", "", null, null);
        levelCard.getChildren().set(1, levelLabel);
        Label controls = UiKit.label("A / D  MOVE     SPACE  JUMP     ESC  PAUSE", "hud-controls");
        HBox.setHgrow(controls, Priority.ALWAYS);

        HBox hud = new HBox(9, lifeCard, scoreCard, bestCard, timeCard, levelCard, controls,
                UiKit.compactButtonWithOptionalIcon("PAUSE", "blue-button",
                        PixelAssetPaths.UI_ICON_PAUSE, onPause),
                UiKit.compactButtonWithOptionalIcon("MENU", "navy-button",
                        PixelAssetPaths.UI_ICON_MENU, onMainMenu));
        hud.getStyleClass().add("game-hud");
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(0));
        return hud;
    }

    private VBox hudCard(String labelText, String valueText, String valueClass, String iconPath) {
        Label label = iconPath == null
                ? UiKit.label(labelText, "hud-label")
                : UiKit.labelWithOptionalIcon(labelText, "hud-label", iconPath);
        Label value = UiKit.label(valueText, "hud-value");
        if (valueClass != null) {
            value.getStyleClass().add(valueClass);
        }
        VBox card = new VBox(0, label, value);
        card.getStyleClass().add("hud-card");
        return card;
    }

    private String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
