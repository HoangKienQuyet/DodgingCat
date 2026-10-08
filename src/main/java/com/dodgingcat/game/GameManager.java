package com.dodgingcat.game;

import com.dodgingcat.ui.GameView;
import com.dodgingcat.ui.MainMenu;
import com.dodgingcat.util.Constants;
import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameManager {

    private final Stage stage;
    private GameState currentState;

    public GameManager(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.stage.setTitle("Dodging Cat");
        this.stage.setResizable(false);
    }

    public void showMainMenu() {
        currentState = GameState.MENU;
        MainMenu mainMenu = new MainMenu(this::startGame, stage::close);
        show(mainMenu.createView());
    }

    public void startGame() {
        currentState = GameState.PLAYING;
        GameView gameView = new GameView(this::showMainMenu);
        show(gameView.createView());
    }

    public GameState getCurrentState() {
        return currentState;
    }

    private void show(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
            return;
        }

        scene.setRoot(root);
    }
}
