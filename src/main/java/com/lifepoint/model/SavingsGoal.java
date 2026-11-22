package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String title;
    private double targetAmount;
    private double currentAmount = 0.0;
    private LocalDate targetDate;
    private String category = "General";
    private String color = "#6366f1";
    private boolean completed = false;

    public SavingsGoal() {}

    public SavingsGoal(String ownerEmail, String title, double targetAmount, LocalDate targetDate, String category, String color) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = 0.0;
        this.targetDate = targetDate;
        this.category = (category == null || category.isBlank()) ? "General" : category;
        this.color = (color == null || color.isBlank()) ? "#6366f1" : color;
        this.completed = false;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { 
        this.currentAmount = currentAmount;
        this.completed = this.currentAmount >= this.targetAmount;
    }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public void addFunds(double amount) {
        if (amount > 0) {
            this.currentAmount += amount;
            if (this.currentAmount >= this.targetAmount) {
                this.completed = true;
            }
        }
    }

    public int getProgressPercentage() {
        if (targetAmount <= 0) return 100;
        int p = (int) Math.round((currentAmount / targetAmount) * 100);
        return Math.min(100, Math.max(0, p));
    }

    public void update(String title, double targetAmount, LocalDate targetDate, String category, String color) {
        this.title = title;
        this.targetAmount = targetAmount;
        if (targetDate != null) this.targetDate = targetDate;
        if (category != null && !category.isBlank()) this.category = category;
        if (color != null && !color.isBlank()) this.color = color;
        this.completed = this.currentAmount >= this.targetAmount;
    }
}
