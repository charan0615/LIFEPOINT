package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class BudgetRequest {
    @NotBlank(message = "Category is required")
    private String category;

    @Positive(message = "Monthly limit must be greater than zero")
    private double monthlyLimit;

    public BudgetRequest() {}

    public BudgetRequest(String category, double monthlyLimit) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
