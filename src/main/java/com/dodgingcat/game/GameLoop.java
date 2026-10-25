package com.dodgingcat.game;

import com.dodgingcat.ui.GameView;
import java.util.Objects;
import java.util.function.IntConsumer;
import javafx.animation.AnimationTimer;

/** Runs one model update and one render per JavaFX animation pulse. */
public final class GameLoop extends AnimationTimer {

    private static final double MAX_FRAME_SECONDS = 0.05;

    private final GameEngine engine;
    private final GameView view;
    private final InputController input;
    private final IntConsumer onGameOver;
    private final Runnable onPause;
    private long previousFrameNanos;

    public GameLoop(GameEngine engine, GameView view, InputController input, IntConsumer onGameOver,
                    Runnable onPause) {
        this.engine = Objects.requireNonNull(engine);
        this.view = Objects.requireNonNull(view);
        this.input = Objects.requireNonNull(input);
        this.onGameOver = Objects.requireNonNull(onGameOver);
        this.onPause = Objects.requireNonNull(onPause);
    }

    @Override
    public void handle(long now) {
        if (previousFrameNanos == 0) {
            previousFrameNanos = now;
            view.render(engine);
            return;
        }

        double deltaSeconds = Math.min((now - previousFrameNanos) / 1_000_000_000.0,
                MAX_FRAME_SECONDS);
        previousFrameNanos = now;

        if (input.consumePauseRequest()) {
            stop();
            onPause.run();
            return;
        }
        if (input.consumeJumpRequest()) {
            engine.jump();
        }
        engine.update(deltaSeconds, input.isMovingLeft(), input.isMovingRight());
        view.render(engine);
        if (engine.isGameOver()) {
            stop();
            onGameOver.accept(engine.getScore());
        }
    }
}
