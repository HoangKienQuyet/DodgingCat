package com.dodgingcat.input;

import javafx.scene.input.KeyCode;
import java.util.EnumSet;
import java.util.Set;

/**
 * Manages keyboard states and desktop mouse pointer interactions.
 */
public class InputManager {
    private final Set<KeyCode> activeKeys = EnumSet.noneOf(KeyCode.class);
    private final Set<KeyCode> justPressedKeys = EnumSet.noneOf(KeyCode.class);

    // Mouse coordinates and state
    private double mouseX = -100;
    private double mouseY = -100;
    private boolean mouseClicked = false;
    private boolean mouseJustClicked = false;
    private double scrollDeltaY = 0.0;

    public void onKeyPressed(KeyCode code) {
        if (!activeKeys.contains(code)) {
            justPressedKeys.add(code);
        }
        activeKeys.add(code);
    }

    public void onKeyReleased(KeyCode code) {
        activeKeys.remove(code);
    }

    public void onMouseMoved(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void onMousePressed(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        this.mouseClicked = true;
        this.mouseJustClicked = true;
    }

    public void onMouseReleased(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        this.mouseClicked = false;
    }

    public void onMouseScroll(double deltaY) {
        scrollDeltaY += deltaY;
    }

    public double consumeScrollDeltaY() {
        double value = scrollDeltaY;
        scrollDeltaY = 0.0;
        return value;
    }

    public void clearFrameTransitions() {
        justPressedKeys.clear();
        mouseJustClicked = false;
        scrollDeltaY = 0.0;
    }

    public boolean isKeyDown(KeyCode code) {
        return activeKeys.contains(code);
    }

    public boolean isKeyJustPressed(KeyCode code) {
        return justPressedKeys.contains(code);
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public boolean isMouseJustClicked() {
        return mouseJustClicked;
    }

    public boolean isMouseDown() {
        return mouseClicked;
    }

    public boolean isMouseHovering(double rx, double ry, double rw, double rh) {
        return mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;
    }

    public boolean isClickedInside(double rx, double ry, double rw, double rh) {
        return mouseJustClicked && isMouseHovering(rx, ry, rw, rh);
    }

    // Player 1 controls (WASD)
    public boolean isP1Left() {
        return activeKeys.contains(KeyCode.A);
    }

    public boolean isP1Right() {
        return activeKeys.contains(KeyCode.D);
    }

    public boolean isP1Jump() {
        return isKeyJustPressed(KeyCode.W);
    }

    public boolean isP1DropDown() {
        return activeKeys.contains(KeyCode.S);
    }

    // Player 2 controls (Arrow keys)
    public boolean isP2Left() {
        return activeKeys.contains(KeyCode.LEFT);
    }

    public boolean isP2Right() {
        return activeKeys.contains(KeyCode.RIGHT);
    }

    public boolean isP2Jump() {
        return isKeyJustPressed(KeyCode.UP);
    }

    public boolean isP2DropDown() {
        return activeKeys.contains(KeyCode.DOWN);
    }

    // General navigation
    public boolean isPausePressed() {
        return isKeyJustPressed(KeyCode.ESCAPE) || isKeyJustPressed(KeyCode.P);
    }

    public boolean isActionPressed() {
        return isKeyJustPressed(KeyCode.SPACE) || isKeyJustPressed(KeyCode.ENTER);
    }

    public boolean isRestartPressed() {
        return isKeyJustPressed(KeyCode.R);
    }

    public void reset() {
        activeKeys.clear();
        justPressedKeys.clear();
        mouseClicked = false;
        mouseJustClicked = false;
        scrollDeltaY = 0.0;
    }
}
