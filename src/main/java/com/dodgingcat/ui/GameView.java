package com.dodgingcat.ui;

import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.Obstacle;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.game.GameEngine;
import com.dodgingcat.game.InputController;
import com.dodgingcat.util.Constants;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.ArcType;

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
        VBox lifeCard = hudCard("♥  ONE LIFE", "NO REVIVES", "life-value");
        VBox scoreCard = hudCard("SCORE", "", null);
        scoreCard.getChildren().set(1, scoreLabel);
        VBox bestCard = hudCard("BEST", "", null);
        bestCard.getChildren().set(1, bestScoreLabel);
        VBox timeCard = hudCard("SURVIVAL", "", null);
        timeCard.getChildren().set(1, timeLabel);
        VBox levelCard = hudCard("LEVEL", "", null);
        levelCard.getChildren().set(1, levelLabel);
        Label controls = UiKit.label("A / D  MOVE     SPACE  JUMP     ESC  PAUSE", "hud-controls");
        HBox.setHgrow(controls, Priority.ALWAYS);

        HBox hud = new HBox(9, lifeCard, scoreCard, bestCard, timeCard, levelCard, controls,
                UiKit.compactButton("PAUSE", "blue-button", onPause),
                UiKit.compactButton("MENU", "navy-button", onMainMenu));
        hud.getStyleClass().add("game-hud");
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(0));
        return hud;
    }

    private VBox hudCard(String labelText, String valueText, String valueClass) {
        Label label = UiKit.label(labelText, "hud-label");
        Label value = UiKit.label(valueText, "hud-value");
        if (valueClass != null) {
            value.getStyleClass().add(valueClass);
        }
        VBox card = new VBox(0, label, value);
        card.getStyleClass().add("hud-card");
        return card;
    }

    private void drawRoom(GraphicsContext g, double groundY) {
        g.setFill(Color.web("#f4ead7"));
        g.fillRect(0, 0, Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
        // Programmatic room details keep the project runnable without external art assets.
        g.setFill(Color.web("#b8dce0"));
        g.fillRoundRect(58, 44, 168, 104, 8, 8);
        g.setStroke(Color.web("#fffdf4"));
        g.setLineWidth(7);
        g.strokeRoundRect(58, 44, 168, 104, 8, 8);
        g.strokeLine(142, 48, 142, 145);
        g.strokeLine(62, 96, 222, 96);
        g.setFill(Color.web("#d9c8ad"));
        g.fillRect(0, groundY - 5, Constants.GAME_WIDTH, 5);
        g.setFill(Color.web("#94b883"));
        g.fillRect(0, groundY, Constants.GAME_WIDTH, Constants.FLOOR_HEIGHT);
        g.setFill(Color.web("#7d9f70"));
        g.fillRect(0, groundY, Constants.GAME_WIDTH, 7);
    }

    private void drawPlatform(GraphicsContext g, Platform platform) {
        double x = platform.x();
        double y = platform.y();
        double width = platform.width();
        double height = platform.height();
        g.setFill(Color.web("#ad774d"));
        g.fillRoundRect(x, y, width, height, 6, 6);
        g.setFill(Color.web("#d4a06f"));
        g.fillRoundRect(x, y, width, 6, 6, 6);
        g.setFill(Color.web("#8f5e42"));
        g.fillRect(x + 12, y + height, 9, 20);
        g.fillRect(x + width - 21, y + height, 9, 20);
    }

    private void drawFood(GraphicsContext g, Food food) {
        if (food instanceof GoldenFood) {
            g.setFill(Color.web("#f4c542"));
            g.fillOval(food.getX(), food.getY(), food.getWidth(), food.getHeight());
            g.setFill(Color.web("#fff4b2"));
            double cx = food.getX() + food.getWidth() / 2;
            double cy = food.getY() + food.getHeight() / 2;
            double[] xs = new double[10];
            double[] ys = new double[10];
            for (int i = 0; i < xs.length; i++) {
                double radius = i % 2 == 0 ? 7 : 3.5;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                xs[i] = cx + Math.cos(angle) * radius;
                ys[i] = cy + Math.sin(angle) * radius;
            }
            g.fillPolygon(xs, ys, xs.length);
        } else {
            g.setFill(Color.web("#e68c4b"));
            g.fillOval(food.getX(), food.getY(), food.getWidth(), food.getHeight());
            g.setFill(Color.web("#ffe3a4"));
            g.fillOval(food.getX() + 5, food.getY() + 4, 6, 6);
        }
    }

    private void drawObstacle(GraphicsContext g, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        switch (obstacle.getKind()) {
            case FLOWER_POT -> {
                g.setFill(Color.web("#5d9664"));
                g.fillOval(x + 4, y - 8, 14, 16);
                g.fillOval(x + 17, y - 12, 15, 20);
                g.setFill(Color.web("#b96b46"));
                g.fillPolygon(new double[]{x + 3, x + 35, x + 30, x + 8},
                        new double[]{y + 9, y + 9, y + 39, y + 39}, 4);
            }
            case PAPER_PLANE -> {
                g.setFill(Color.web("#f5f5f0"));
                g.fillPolygon(new double[]{x, x + obstacle.getWidth(), x + 16},
                        new double[]{y + obstacle.getHeight() / 2, y, y + obstacle.getHeight()}, 3);
                g.setStroke(Color.web("#6485a3"));
                g.setLineWidth(2);
                g.strokeLine(x + 16, y + obstacle.getHeight(), x + 23, y + obstacle.getHeight() / 2);
                g.strokeLine(x + 23, y + obstacle.getHeight() / 2, x + obstacle.getWidth(), y);
            }
            case YOYO -> {
                double centerX = x + obstacle.getWidth() / 2;
                g.setStroke(Color.web("#d8c8af"));
                g.setLineWidth(3);
                g.strokeLine(centerX, y - 52, centerX, y + 8);
                g.setFill(Color.web("#d54c62"));
                g.fillOval(x, y, obstacle.getWidth(), obstacle.getHeight());
                g.setFill(Color.web("#f7a7a8"));
                g.fillOval(x + 11, y + 11, obstacle.getWidth() - 22, obstacle.getHeight() - 22);
                g.setFill(Color.web("#ffd85c"));
                g.fillOval(centerX - 6, y + obstacle.getHeight() / 2 - 6, 12, 12);
            }
            case ROBOT_VACUUM -> {
                g.setFill(Color.web("#555e65"));
                g.fillRoundRect(x, y + 2, obstacle.getWidth(), obstacle.getHeight() - 2, 15, 15);
                g.setFill(Color.web("#9da8ac"));
                g.fillOval(x + 21, y + 8, 12, 12);
                g.setFill(Color.web("#f2c94c"));
                g.fillOval(x + 7, y + 18, 5, 4);
            }
            case ROLLING_BALL -> {
                g.setFill(Color.web("#d97755"));
                g.fillOval(x, y, obstacle.getWidth(), obstacle.getHeight());
                g.setStroke(Color.web("#f4b08f"));
                g.setLineWidth(3);
                g.strokeArc(x + 5, y + 5, obstacle.getWidth() - 10,
                        obstacle.getHeight() - 10, 35, 100, ArcType.OPEN);
            }
        }
    }

    private void drawCat(GraphicsContext g, GameEngine engine) {
        double x = engine.getCat().getX();
        double y = engine.getCat().getY();
        g.setFill(Color.web("#e78950"));
        g.fillPolygon(new double[]{x + 34, x + 38, x + 44},
                new double[]{y + 16, y + 2, y + 15}, 3);
        g.fillPolygon(new double[]{x + 44, x + 51, x + 54},
                new double[]{y + 15, y + 3, y + 18}, 3);
        g.fillRoundRect(x + 4, y + 17, 48, 27, 18, 18);
        g.fillOval(x + 31, y + 7, 25, 25);
        g.setFill(Color.web("#fff1df"));
        g.fillOval(x + 36, y + 11, 5, 7);
        g.fillOval(x + 49, y + 11, 5, 7);
        g.setFill(Color.web("#29343a"));
        g.fillOval(x + 38, y + 13, 2.5, 4);
        g.fillOval(x + 51, y + 13, 2.5, 4);
        g.setFill(Color.web("#b85c62"));
        g.fillOval(x + 44, y + 21, 5, 3);
        g.setStroke(Color.web("#6e493f"));
        g.setLineWidth(1.2);
        g.strokeLine(x + 43, y + 24, x + 36, y + 25);
        g.strokeLine(x + 49, y + 24, x + 56, y + 25);
    }

    private String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
