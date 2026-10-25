package com.dodgingcat.entity;

/** A fixed, one-way surface that the cat can land on from above. */
public record Platform(double x, double y, double width, double height) {

    public Platform {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Platform dimensions must be positive");
        }
    }
}
