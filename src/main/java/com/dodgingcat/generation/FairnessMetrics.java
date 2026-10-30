package com.dodgingcat.generation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks measurable metrics on procedural candidate generation, rejections, and fairness rule evaluations.
 */
public class FairnessMetrics {
    private int totalCandidatesGenerated = 0;
    private int candidatesAccepted = 0;
    private int candidatesRejected = 0;
    private final Map<String, Integer> rejectionCountsByReason = new HashMap<>();

    public synchronized void recordCandidate(boolean accepted, String reason) {
        totalCandidatesGenerated++;
        if (accepted) {
            candidatesAccepted++;
        } else {
            candidatesRejected++;
            String normalizedReason = (reason != null && !reason.isBlank()) ? reason : "Unknown";
            rejectionCountsByReason.put(normalizedReason, rejectionCountsByReason.getOrDefault(normalizedReason, 0) + 1);
        }
    }

    public synchronized double getInvalidGenerationRate() {
        if (totalCandidatesGenerated == 0) return 0.0;
        return (double) candidatesRejected / totalCandidatesGenerated;
    }

    public synchronized double getAcceptanceRate() {
        if (totalCandidatesGenerated == 0) return 0.0;
        return (double) candidatesAccepted / totalCandidatesGenerated;
    }

    public synchronized int getTotalCandidatesGenerated() {
        return totalCandidatesGenerated;
    }

    public synchronized int getCandidatesAccepted() {
        return candidatesAccepted;
    }

    public synchronized int getCandidatesRejected() {
        return candidatesRejected;
    }

    public synchronized Map<String, Integer> getRejectionCountsByReason() {
        return Collections.unmodifiableMap(new HashMap<>(rejectionCountsByReason));
    }

    public synchronized void reset() {
        totalCandidatesGenerated = 0;
        candidatesAccepted = 0;
        candidatesRejected = 0;
        rejectionCountsByReason.clear();
    }
}
