package com.dodgingcat.save;

/** One completed run; scores represent the run's survival time in seconds. */
public record RunResult(double score, int fish) {
    public RunResult {
        if (!Double.isFinite(score) || score < 0) {
            throw new IllegalArgumentException("Score must be finite and non-negative.");
        }
        if (fish < 0) {
            throw new IllegalArgumentException("Fish must be non-negative.");
        }
    }
}
