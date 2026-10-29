package com.dodgingcat.game;

/** Pickup coordinates describe its CENTER. Null kind denotes an ordinary fish. */
public final class Pickup {
    private final double x;
    private final double y;
    private final GameWorld.BuffKind kind;
    double age;

    Pickup(double x, double y, GameWorld.BuffKind kind) {
        this.x = x;
        this.y = y;
        this.kind = kind;
    }

    public double x() { return x; }
    public double y() { return y; }
    public GameWorld.BuffKind kind() { return kind; }
    public double radius() { return 11; }
    public double age() { return age; }
}
