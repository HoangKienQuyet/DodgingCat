package com.dodgingcat.collision;

/**
 * Utility functions for spatial collisions.
 */
public final class CollisionDetector {

    private CollisionDetector() {}

    public static boolean aabb(double x1, double y1, double w1, double h1,
                               double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2;
    }

    public static boolean circleAABB(double cx, double cy, double radius,
                                    double rx, double ry, double rw, double rh) {
        double closestX = Math.clamp(cx, rx, rx + rw);
        double closestY = Math.clamp(cy, ry, ry + rh);
        double dx = cx - closestX;
        double dy = cy - closestY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    public static boolean horizontalSegmentIntersectsAABB(double segY, double segX1, double segX2,
                                                         double rx, double ry, double rw, double rh) {
        if (segY < ry || segY > ry + rh) return false;
        double minX = Math.min(segX1, segX2);
        double maxX = Math.max(segX1, segX2);
        return maxX > rx && minX < rx + rw;
    }
}
