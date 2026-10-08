package com.dodgingcat.ui;

import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainMenu {

    private final Runnable onStartGame;
    private final Runnable onExit;

    public MainMenu(Runnable onStartGame, Runnable onExit) {
        this.onStartGame = Objects.requireNonNull(onStartGame, "onStartGame must not be null");
        this.onExit = Objects.requireNonNull(onExit, "onExit must not be null");
    }

    public Parent createView() {
        Label title = new Label("DODGING CAT");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setStyle("-fx-text-fill: #263238;");

        Button startButton = createMenuButton("Start Game");
        startButton.setOnAction(event -> onStartGame.run());

        Button exitButton = createMenuButton("Exit");
        exitButton.setOnAction(event -> onExit.run());

        VBox root = new VBox(24, title, startButton, exitButton);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f7f3dc, #b7dce2);");

        return root;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(180);
        button.setMinHeight(42);
        button.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        return button;
    }
}
