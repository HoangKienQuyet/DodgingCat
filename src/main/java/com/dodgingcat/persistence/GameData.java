package com.dodgingcat.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable profile data model for JSON persistence.
 */
public class GameData {
    private int singleHighScore = 0;
    private int coopHighScore = 0;
    private int totalFishes = 0;
    private String selectedMap = "kitchen";
    private String lastPlayedMap;
    private String p1Skin = "ginger";
    private String p2Skin = "tuxedo";
    private String language = "en";
    private double bgmVolume = 0.7;
    private double sfxVolume = 0.8;
    private int skinGachaPulls = 0;

    private List<String> unlockedMaps = new ArrayList<>();
    private List<String> unlockedSkins = new ArrayList<>();
    private List<LeaderboardEntry> singleLeaderboard = new ArrayList<>();
    private List<LeaderboardEntry> coopLeaderboard = new ArrayList<>();

    public GameData() {
        // Defaults
        unlockedMaps.add("kitchen");
        unlockedSkins.add("ginger");
        unlockedSkins.add("tuxedo");
    }

    public int getSingleHighScore() {
        return singleHighScore;
    }

    public void setSingleHighScore(int singleHighScore) {
        this.singleHighScore = singleHighScore;
    }

    public int getCoopHighScore() {
        return coopHighScore;
    }

    public void setCoopHighScore(int coopHighScore) {
        this.coopHighScore = coopHighScore;
    }

    public int getTotalFishes() {
        return totalFishes;
    }

    public void setTotalFishes(int totalFishes) {
        this.totalFishes = totalFishes;
    }

    public void addFishes(int amount) {
        this.totalFishes = Math.max(0, this.totalFishes + amount);
    }

    public boolean spendFishes(int amount) {
        if (amount <= totalFishes) {
            totalFishes -= amount;
            return true;
        }
        return false;
    }

    // Legacy Pate aliases for backwards compatibility
    public int getTotalPate() { return getTotalFishes(); }
    public void setTotalPate(int amount) { setTotalFishes(amount); }
    public void addPate(int amount) { addFishes(amount); }
    public boolean spendPate(int amount) { return spendFishes(amount); }

    public String getSelectedMap() {
        return selectedMap;
    }

    public void setSelectedMap(String selectedMap) {
        // Older profiles have no lastPlayedMap; preserve their previous selection
        // before a new loadout choice changes it.
        if (lastPlayedMap == null) lastPlayedMap = this.selectedMap;
        this.selectedMap = selectedMap;
    }

    public String getLastPlayedMap() { return lastPlayedMap == null ? selectedMap : lastPlayedMap; }
    public void setLastPlayedMap(String map) { lastPlayedMap = map; }

    public String getSelectedTheme() {
        return getSelectedMap();
    }

    public void setSelectedTheme(String theme) {
        setSelectedMap(theme);
    }

    public String getP1Skin() {
        return p1Skin;
    }

    public void setP1Skin(String p1Skin) {
        this.p1Skin = p1Skin;
    }

    public String getP2Skin() {
        return p2Skin;
    }

    public void setP2Skin(String p2Skin) {
        this.p2Skin = p2Skin;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public double getBgmVolume() {
        return bgmVolume;
    }

    public void setBgmVolume(double bgmVolume) {
        this.bgmVolume = bgmVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

    public int getSkinGachaPulls() { return skinGachaPulls; }
    public void setSkinGachaPulls(int pulls) { skinGachaPulls = Math.max(0, pulls); }

    public List<String> getUnlockedMaps() {
        return unlockedMaps;
    }

    public void setUnlockedMaps(List<String> unlockedMaps) {
        this.unlockedMaps = unlockedMaps;
    }

    public List<String> getUnlockedThemes() {
        return getUnlockedMaps();
    }

    public void setUnlockedThemes(List<String> themes) {
        setUnlockedMaps(themes);
    }

    public List<String> getUnlockedSkins() {
        return unlockedSkins;
    }

    public void setUnlockedSkins(List<String> unlockedSkins) {
        this.unlockedSkins = unlockedSkins;
    }

    public List<LeaderboardEntry> getSingleLeaderboard() {
        return singleLeaderboard;
    }

    public void setSingleLeaderboard(List<LeaderboardEntry> singleLeaderboard) {
        this.singleLeaderboard = singleLeaderboard;
    }

    public List<LeaderboardEntry> getCoopLeaderboard() {
        return coopLeaderboard;
    }

    public void setCoopLeaderboard(List<LeaderboardEntry> coopLeaderboard) {
        this.coopLeaderboard = coopLeaderboard;
    }

    public int addSingleRun(int score, double time) {
        if (score > singleHighScore) {
            singleHighScore = score;
        }
        LeaderboardEntry entry = new LeaderboardEntry("Single Cat", score, time);
        singleLeaderboard.add(entry);
        singleLeaderboard.sort(null);
        int rank = singleLeaderboard.indexOf(entry) + 1;
        if (singleLeaderboard.size() > 10) {
            singleLeaderboard = new ArrayList<>(singleLeaderboard.subList(0, 10));
        }
        return rank <= 10 ? rank : 0;
    }

    public int addCoopRun(int score, double time) {
        if (score > coopHighScore) {
            coopHighScore = score;
        }
        LeaderboardEntry entry = new LeaderboardEntry("Co-op Team", score, time);
        coopLeaderboard.add(entry);
        coopLeaderboard.sort(null);
        int rank = coopLeaderboard.indexOf(entry) + 1;
        if (coopLeaderboard.size() > 10) {
            coopLeaderboard = new ArrayList<>(coopLeaderboard.subList(0, 10));
        }
        return rank <= 10 ? rank : 0;
    }
}
