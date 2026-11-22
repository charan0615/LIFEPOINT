package com.lifepoint.dto;

public class BudgetStatus {
    private String category;
    private double monthlyLimit;
    private double spent;
    private double remaining;
    private int percentage;
    private String status; // SAFE, WARNING, DANGER

    public BudgetStatus(String category, double monthlyLimit, double spent) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.spent = spent;
        this.remaining = Math.max(0, monthlyLimit - spent);
        if (monthlyLimit > 0) {
            this.percentage = (int) Math.round((spent / monthlyLimit) * 100);
        } else {
            this.percentage = 100;
        }

        if (percentage >= 100) {
            this.status = "DANGER";
        } else if (percentage >= 80) {
            this.status = "WARNING";
        } else {
            this.status = "SAFE";
        }
    }

    public String getCategory() { return category; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public double getSpent() { return spent; }
    public double getRemaining() { return remaining; }
    public int getPercentage() { return percentage; }
    public String getStatus() { return status; }
}
