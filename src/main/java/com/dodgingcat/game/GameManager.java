package com.dodgingcat.game;

import com.dodgingcat.io.HighScoreRepository;
import com.dodgingcat.ui.GameView;
import com.dodgingcat.ui.GameOverView;
import com.dodgingcat.ui.FeatureInfoView;
import com.dodgingcat.ui.MainMenu;
import com.dodgingcat.ui.PauseView;
import com.dodgingcat.ui.UiKit;
import com.dodgingcat.util.Constants;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameManager {

    private static final System.Logger LOGGER = System.getLogger(GameManager.class.getName());

    private final Stage stage;
    private final HighScoreRepository highScoreRepository;
    private int highScore;
    private GameState currentState;
    private GameEngine activeGame;
    private GameLoop gameLoop;
    private GameView gameView;
    private PauseView pauseView;

    public GameManager(Stage stage) {
        this(stage, new HighScoreRepository());
    }

    public GameManager(Stage stage, HighScoreRepository highScoreRepository) {
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.highScoreRepository = Objects.requireNonNull(highScoreRepository,
                "high score repository must not be null");
        this.highScore = highScoreRepository.load();
        this.stage.setTitle("Dodging Cat");
        this.stage.setResizable(false);
    }

    public void showMainMenu() {
        stopPlayingPresentation();
        detachPauseInput();
        activeGame = null;
        currentState = GameState.MENU;
        MainMenu mainMenu = new MainMenu(highScore, this::startGame,
                () -> showFeatureInfo("CAT STYLE SHOP",
                        "A wardrobe full of hats, scarves, glasses, crowns, and cat styles is planned for a later milestone."),
                () -> showFeatureInfo("HIGH SCORE",
                        "Your current best score is " + highScore + ". Full score history arrives in a later milestone."),
                () -> showFeatureInfo("SETTINGS",
                        "Sound, display, and accessibility settings are planned for a later milestone."),
                stage::close);
        show(mainMenu.createView());
    }

    public void startGame() {
        if (!canTransitionTo(GameState.PLAYING)) {
            return;
        }
        stopPlayingPresentation();
        detachPauseInput();
        currentState = GameState.PLAYING;
        activeGame = new GameEngine(Constants.GAME_WIDTH, Constants.GAME_HEIGHT, new Random());
        showActiveGame();
    }

    public void pauseGame() {
        if (!canTransitionTo(GameState.PAUSED) || activeGame == null || activeGame.isGameOver()) {
            return;
        }

        stopPlayingPresentation();
        currentState = GameState.PAUSED;
        pauseView = new PauseView(activeGame.getScore(), this::resumeGame, this::showMainMenu);
        show(pauseView.createView());
        pauseView.attachInput(stage.getScene());
    }

    public void resumeGame() {
        if (!canTransitionTo(GameState.PLAYING) || activeGame == null) {
            return;
        }

        detachPauseInput();
        currentState = GameState.PLAYING;
        showActiveGame();
    }

    private void showActiveGame() {
        gameView = new GameView(highScore, this::showMainMenu, this::pauseGame);
        show(gameView.createView());
        gameView.attachInput(stage.getScene());
        gameLoop = new GameLoop(activeGame, gameView, gameView.getInputController(),
                this::showGameOver, this::pauseGame);
        gameLoop.start();
    }

    public GameState getCurrentState() {
        return currentState;
    }

    private void show(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            UiKit.applyTheme(scene);
            stage.setScene(scene);
            return;
        }

        scene.setRoot(root);
        UiKit.applyTheme(scene);
    }

    private void showFeatureInfo(String title, String message) {
        currentState = GameState.MENU;
        show(new FeatureInfoView(title, message, this::showMainMenu).createView());
    }

    private void showGameOver(int finalScore) {
        if (!canTransitionTo(GameState.GAME_OVER)) {
            return;
        }
        stopPlayingPresentation();
        detachPauseInput();
        activeGame = null;
        currentState = GameState.GAME_OVER;
        boolean newRecord = finalScore > highScore;
        if (newRecord) {
            highScore = finalScore;
            try {
                highScoreRepository.save(highScore);
            } catch (IOException exception) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Could not save the new high score; it remains available for this session.",
                        exception);
            }
        }
        GameOverView gameOverView = new GameOverView(
                finalScore, highScore, newRecord, this::startGame, this::showMainMenu);
        show(gameOverView.createView());
    }

    private boolean canTransitionTo(GameState nextState) {
        return currentState == null || currentState.canTransitionTo(nextState);
    }

    private void stopPlayingPresentation() {
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (gameView != null) {
            gameView.detachInput();
            gameView = null;
        }
    }

    private void detachPauseInput() {
        if (pauseView != null) {
            pauseView.detachInput();
            pauseView = null;
        }
    }
}
