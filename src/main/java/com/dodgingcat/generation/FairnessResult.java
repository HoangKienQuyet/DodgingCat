package com.dodgingcat.generation;

/**
 * Result returned by the FairnessValidator when testing a candidate hazard.
 */
public record FairnessResult(boolean accepted, String rejectionReason) {

    public static FairnessResult accept() {
        return new FairnessResult(true, null);
    }

    public static FairnessResult reject(String reason) {
        return new FairnessResult(false, reason);
    }
}
