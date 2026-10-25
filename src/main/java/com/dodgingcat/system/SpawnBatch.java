package com.dodgingcat.system;

import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.Obstacle;
import java.util.List;

/** Objects created during a single spawn-manager update. */
public record SpawnBatch(List<Food> food, List<Obstacle> obstacles) {

    public SpawnBatch {
        food = List.copyOf(food);
        obstacles = List.copyOf(obstacles);
    }
}
