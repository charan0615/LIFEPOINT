package com.lifepoint.dto;

public class LeaderboardEntry {
    private int rank;
    private String name;
    private String email;
    private int level;
    private int xp;
    private double totalSaved;
    private int maxStreak;

    public LeaderboardEntry() {}

    public LeaderboardEntry(int rank, String name, String email, int level, int xp, double totalSaved, int maxStreak) {
        this.rank = rank;
        this.name = name;
        this.email = email;
        this.level = level;
        this.xp = xp;
        this.totalSaved = totalSaved;
        this.maxStreak = maxStreak;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public double getTotalSaved() { return totalSaved; }
    public void setTotalSaved(double totalSaved) { this.totalSaved = totalSaved; }
    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
}
