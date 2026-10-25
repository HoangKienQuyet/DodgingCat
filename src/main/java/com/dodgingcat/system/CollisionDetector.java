package com.dodgingcat.system;

import com.dodgingcat.entity.GameObject;
import java.util.Objects;

/** Reusable axis-aligned bounding-box collision checks. */
public final class CollisionDetector {

    public boolean intersects(GameObject first, GameObject second) {
        Objects.requireNonNull(first, "first object must not be null");
        Objects.requireNonNull(second, "second object must not be null");
        return first.getX() < second.getX() + second.getWidth()
                && first.getX() + first.getWidth() > second.getX()
                && first.getY() < second.getY() + second.getHeight()
                && first.getY() + first.getHeight() > second.getY();
    }
}
