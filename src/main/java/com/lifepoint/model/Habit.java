package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Habit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String title;
    private String frequency = "DAILY";
    private String category = "Personal";
    private LocalDate completedOn;

    private int currentStreak = 0;
    private int bestStreak = 0;
    private int totalCompletions = 0;

    // Financial link ("Saved by choice")
    private boolean moneySaver = false;
    private double savedAmount = 0.0;
    private Long linkedGoalId;

    public Habit() {}

    public Habit(String ownerEmail, String title) {
        this.ownerEmail = ownerEmail;
        this.title = title;
    }

    public Habit(String ownerEmail, String title, String category, boolean moneySaver, double savedAmount, Long linkedGoalId) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.category = (category == null || category.isBlank()) ? "Personal" : category;
        this.moneySaver = moneySaver;
        this.savedAmount = savedAmount;
        this.linkedGoalId = linkedGoalId;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public int getTotalCompletions() { return totalCompletions; }
    public void setTotalCompletions(int totalCompletions) { this.totalCompletions = totalCompletions; }

    public boolean isMoneySaver() { return moneySaver; }
    public void setMoneySaver(boolean moneySaver) { this.moneySaver = moneySaver; }
    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }
    public Long getLinkedGoalId() { return linkedGoalId; }
    public void setLinkedGoalId(Long linkedGoalId) { this.linkedGoalId = linkedGoalId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public boolean isDoneToday() {
        return completedOn != null && LocalDate.now().equals(completedOn);
    }

    public boolean completeToday() {
        if (isDoneToday()) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (completedOn != null && completedOn.equals(today.minusDays(1))) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }

        totalCompletions++;
        completedOn = today;
        return true;
    }

    public void update(String title, String category, boolean moneySaver, double savedAmount, Long linkedGoalId) {
        this.title = title;
        this.category = (category == null || category.isBlank()) ? "Personal" : category;
        this.moneySaver = moneySaver;
        this.savedAmount = savedAmount;
        this.linkedGoalId = linkedGoalId;
    }
}
