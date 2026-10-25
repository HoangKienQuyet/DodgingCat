package com.dodgingcat.game;

import java.util.EnumSet;
import java.util.Set;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Translates JavaFX key events into simple gameplay input flags. */
public final class InputController {

    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private final EventHandler<KeyEvent> keyPressedHandler = this::onKeyPressed;
    private final EventHandler<KeyEvent> keyReleasedHandler = this::onKeyReleased;
    private Scene scene;
    private boolean jumpRequested;
    private boolean pauseRequested;

    public void attach(Scene scene) {
        detach();
        this.scene = scene;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
    }

    public void detach() {
        if (scene != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
            scene = null;
        }
        pressedKeys.clear();
        jumpRequested = false;
        pauseRequested = false;
    }

    public boolean isMovingLeft() {
        return pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A);
    }

    public boolean isMovingRight() {
        return pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D);
    }

    public boolean consumeJumpRequest() {
        boolean requested = jumpRequested;
        jumpRequested = false;
        return requested;
    }

    public boolean consumePauseRequest() {
        boolean requested = pauseRequested;
        pauseRequested = false;
        return requested;
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean firstPress = pressedKeys.add(code);
        if (firstPress) {
            if (code == KeyCode.ESCAPE) {
                pauseRequested = true;
                event.consume();
            } else if (code == KeyCode.SPACE || code == KeyCode.UP || code == KeyCode.W) {
                jumpRequested = true;
            }
        }
    }

    private void onKeyReleased(KeyEvent event) {
        pressedKeys.remove(event.getCode());
    }
}
