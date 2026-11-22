package com.lifepoint.model;

import jakarta.persistence.*;

@Entity
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;
    private String currency = "INR";
    private int xp = 0;
    private int level = 1;
    private double totalSavedByHabits = 0.0;

    public Profile() {}

    public Profile(String email, String name) {
        this.email = email;
        this.name = name;
        this.currency = "INR";
        this.xp = 0;
        this.level = 1;
        this.totalSavedByHabits = 0.0;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getXp() { return xp; }
    public void setXp(int xp) { 
        this.xp = xp;
        this.level = calculateLevel(xp);
    }

    public void addXp(int points) {
        this.xp += points;
        this.level = calculateLevel(this.xp);
    }

    public int getLevel() { 
        if (level < 1) level = calculateLevel(xp);
        return level; 
    }
    public void setLevel(int level) { this.level = level; }

    public double getTotalSavedByHabits() { return totalSavedByHabits; }
    public void setTotalSavedByHabits(double totalSavedByHabits) { this.totalSavedByHabits = totalSavedByHabits; }
    public void addSavedByHabits(double amount) { this.totalSavedByHabits += amount; }

    public static int calculateLevel(int xp) {
        // Level threshold: Level 1 = 0-99 XP, Level 2 = 100-249 XP, Level 3 = 250-449 XP, etc.
        if (xp <= 0) return 1;
        return (int) Math.floor(Math.sqrt(xp / 25.0)) + 1;
    }

    public int getXpForNextLevel() {
        int currentLvl = getLevel();
        return currentLvl * currentLvl * 25;
    }

    public int getXpForCurrentLevel() {
        int currentLvl = getLevel();
        if (currentLvl <= 1) return 0;
        return (currentLvl - 1) * (currentLvl - 1) * 25;
    }

    public int getLevelProgressPercent() {
        int currentBase = getXpForCurrentLevel();
        int nextTarget = getXpForNextLevel();
        if (nextTarget <= currentBase) return 100;
        int progress = (int) Math.round(((double) (xp - currentBase) / (nextTarget - currentBase)) * 100);
        return Math.min(100, Math.max(0, progress));
    }
}
