package com.dodgingcat.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * High score record entry for single-player or co-op.
 */
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    private final String playerName;
    private final int score;
    private final double survivalTime;
    private final String timestamp;

    public LeaderboardEntry(String playerName, int score, double survivalTime) {
        this.playerName = playerName;
        this.score = score;
        this.survivalTime = survivalTime;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public double getSurvivalTime() {
        return survivalTime;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        return Integer.compare(other.score, this.score); // Descending order
    }
}
