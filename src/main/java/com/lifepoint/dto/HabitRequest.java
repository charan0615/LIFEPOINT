package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class HabitRequest {
    @NotBlank(message = "Habit title cannot be empty")
    @Size(max = 120, message = "Habit title cannot exceed 120 characters")
    private String title;

    private String category = "Personal";
    private boolean moneySaver = false;

    @PositiveOrZero(message = "Saved amount must be zero or positive")
    private double savedAmount = 0.0;

    private Long linkedGoalId;

    public HabitRequest() {}

    public HabitRequest(String title, String category, boolean moneySaver, double savedAmount, Long linkedGoalId) {
        this.title = title;
        this.category = category;
        this.moneySaver = moneySaver;
        this.savedAmount = savedAmount;
        this.linkedGoalId = linkedGoalId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isMoneySaver() { return moneySaver; }
    public void setMoneySaver(boolean moneySaver) { this.moneySaver = moneySaver; }
    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }
    public Long getLinkedGoalId() { return linkedGoalId; }
    public void setLinkedGoalId(Long linkedGoalId) { this.linkedGoalId = linkedGoalId; }
}
