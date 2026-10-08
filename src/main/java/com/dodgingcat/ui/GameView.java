package com.dodgingcat.ui;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameView {

    private final Runnable onMainMenu;

    public GameView(Runnable onMainMenu) {
        this.onMainMenu = Objects.requireNonNull(onMainMenu, "onMainMenu must not be null");
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.setTop(createHud());
        root.setCenter(createPlayArea());
        root.setStyle("-fx-background-color: #eef5f0;");
        return root;
    }

    private HBox createHud() {
        Label scoreLabel = new Label("Score: 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button menuButton = new Button("Main Menu");
        menuButton.setOnAction(event -> onMainMenu.run());

        HBox hud = new HBox(24, scoreLabel, menuButton);
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(12, 16, 12, 16));
        hud.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d5ddd8; -fx-border-width: 0 0 1 0;");
        return hud;
    }

    private Pane createPlayArea() {
        Pane playArea = new Pane();
        playArea.setStyle("-fx-background-color: #f4ead7;");

        Rectangle floor = new Rectangle(0, 420, 900, 95);
        floor.setFill(Color.web("#9bbd8b"));

        Circle catBody = new Circle(110, 380, 28);
        catBody.setFill(Color.web("#f48b50"));

        Circle food = new Circle(690, 360, 14);
        food.setFill(Color.web("#f2c94c"));

        Rectangle obstacle = new Rectangle(430, 370, 42, 50);
        obstacle.setArcWidth(8);
        obstacle.setArcHeight(8);
        obstacle.setFill(Color.web("#8b6f5a"));

        playArea.getChildren().addAll(floor, catBody, food, obstacle);
        return playArea;
    }
}
