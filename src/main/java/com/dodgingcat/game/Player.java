package com.dodgingcat.game;

/** Read-only view of the cat. Coordinates describe the top-left of its hit box. */
public final class Player {
    public static final double WIDTH = 32;
    public static final double HEIGHT = 38;

    private final int id;
    boolean alive = true;
    int fishCollected;
    boolean shieldActive;
    double invincibilityRemaining;
    double doubleFishRemaining;
    double damageGraceRemaining;
    double jumpBufferRemaining;
    boolean jumpWasHeld;
    boolean dropWasHeld;
    double x;
    double y;
    double vx;
    double vy;
    boolean grounded;
    boolean facingRight = true;
    GameWorld.Platform support;
    double coyoteRemaining;
    double ignorePlatformsRemaining;

    Player(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.grounded = true;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double width() { return WIDTH; }
    public double height() { return HEIGHT; }
    public double vx() { return vx; }
    public double vy() { return vy; }
    public boolean grounded() { return grounded; }
    public boolean facingRight() { return facingRight; }
    public int id() { return id; }
    public boolean alive() { return alive; }
    public int fishCollected() { return fishCollected; }
    public boolean shieldActive() { return shieldActive; }
    public double invincibilityRemaining() { return invincibilityRemaining; }
    public double doubleFishRemaining() { return doubleFishRemaining; }
}
