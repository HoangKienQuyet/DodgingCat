package com.dodgingcat.ui;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainMenu {

    private final int highScore;
    private final Runnable onStartGame;
    private final Runnable onShop;
    private final Runnable onHighScore;
    private final Runnable onSettings;
    private final Runnable onExit;

    public MainMenu(int highScore, Runnable onStartGame, Runnable onShop, Runnable onHighScore,
                    Runnable onSettings, Runnable onExit) {
        this.highScore = highScore;
        this.onStartGame = Objects.requireNonNull(onStartGame, "onStartGame must not be null");
        this.onShop = Objects.requireNonNull(onShop, "onShop must not be null");
        this.onHighScore = Objects.requireNonNull(onHighScore, "onHighScore must not be null");
        this.onSettings = Objects.requireNonNull(onSettings, "onSettings must not be null");
        this.onExit = Objects.requireNonNull(onExit, "onExit must not be null");
    }

    public Parent createView() {
        StackPane layeredRoot = new StackPane(CozyArtwork.createMenuBackdrop());
        layeredRoot.setPrefSize(900, 600);

        Label kicker = UiKit.label("✦  SMALL CAT  •  BIG ADVENTURE  ✦", "screen-kicker");
        Label title = UiKit.label("DODGING CAT", "screen-title");
        Label subtitle = UiKit.label("Leap, collect, and dodge your way through a cozy home.",
                "body-text");
        Label bestScore = UiKit.label("★  BEST SCORE   " + highScore, "score-value");

        VBox actions = new VBox(10,
                UiKit.menuButton("▶  PLAY", "primary-button", onStartGame),
                UiKit.menuButton("♣  CAT STYLE SHOP", "blue-button", onShop),
                UiKit.menuButton("★  HIGH SCORE", "gold-button", onHighScore),
                UiKit.menuButton("⚙  SETTINGS", "purple-button", onSettings));
        actions.setPrefWidth(315);
        VBox panel = new VBox(11, kicker, title, subtitle, bestScore, actions,
                UiKit.compactButton("EXIT GAME", "navy-button", onExit));
        panel.getStyleClass().add("menu-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxWidth(390);

        HBox content = new HBox(panel);
        content.setPadding(new Insets(60, 0, 38, 472));
        content.setAlignment(Pos.CENTER_LEFT);
        layeredRoot.getChildren().add(content);
        return layeredRoot;
    }
}
