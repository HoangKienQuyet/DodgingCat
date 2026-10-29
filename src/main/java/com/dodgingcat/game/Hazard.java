package com.dodgingcat.game;

/** Ordinary obstacles move immediately; only lasers may have a harmless warning phase. */
public final class Hazard {
    public static final double FLAME_LIFETIME_SECONDS = 1.65;
    private final GameWorld.HazardKind kind;
    double x;
    double y;
    private final double width;
    private final double height;
    private final int direction;
    private final double speed;
    private double warningRemaining;
    private double age;
    private double activeAge;

    Hazard(GameWorld.HazardKind kind, double x, double y, double width, double height,
           int direction, double speed, double warningSeconds) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.speed = speed;
        this.warningRemaining = kind == GameWorld.HazardKind.LASER && Double.isFinite(warningSeconds)
                ? Math.max(0, warningSeconds) : 0;
    }

    void update(double dt) {
        age += dt;
        double activeDt = Math.max(0, dt - warningRemaining);
        warningRemaining = Math.max(0, warningRemaining - dt);
        if (activeDt <= 0) return;
        activeAge += activeDt;
        switch (kind) {
            case KNIFE -> y += speed * activeDt;
            case DART, POISON, SCISSORS -> x += direction * speed * activeDt;
            case LASER, FLAME -> { }
        }
    }

    boolean expired() {
        return y > GameWorld.HEIGHT + 64 || x > GameWorld.WIDTH + 64 || x + width < -64
                || (kind == GameWorld.HazardKind.LASER && activeAge >= 0.62)
                || (kind == GameWorld.HazardKind.FLAME && activeAge + 1e-10 >= FLAME_LIFETIME_SECONDS);
    }

    public GameWorld.HazardKind kind() { return kind; }
    public double x() { return x; }
    public double y() { return y; }
    public double width() { return width; }
    public double height() { return height; }
    public double warningRemaining() { return warningRemaining; }
    public boolean active() { return warningRemaining <= 0; }
    /** +1 moves right, -1 moves left for horizontal hazards; stationary/falling hazards use 0. */
    public int direction() { return direction; }
    public double age() { return age; }
    double speed() { return speed; }
}
