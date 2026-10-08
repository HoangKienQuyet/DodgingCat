package com.dodgingcat;

import com.dodgingcat.game.GameManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        GameManager gameManager = new GameManager(stage);
        gameManager.showMainMenu();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 
