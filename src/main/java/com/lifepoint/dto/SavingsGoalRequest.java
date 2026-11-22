package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class SavingsGoalRequest {
    @NotBlank(message = "Goal title cannot be empty")
    @Size(max = 120, message = "Goal title cannot exceed 120 characters")
    private String title;

    @Positive(message = "Target amount must be greater than zero")
    private double targetAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;

    private String category = "General";
    private String color = "#6366f1";

    public SavingsGoalRequest() {}

    public SavingsGoalRequest(String title, double targetAmount, LocalDate targetDate, String category, String color) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.category = category;
        this.color = color;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
