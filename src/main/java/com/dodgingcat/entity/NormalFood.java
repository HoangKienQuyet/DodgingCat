package com.dodgingcat.entity;

public final class NormalFood extends Food {

    public static final int SCORE_VALUE = 10;
    public static final double SIZE = 22;

    public NormalFood(double x, double y) {
        super(x, y, SIZE);
    }

    @Override
    public int getScoreValue() {
        return SCORE_VALUE;
    }
}
