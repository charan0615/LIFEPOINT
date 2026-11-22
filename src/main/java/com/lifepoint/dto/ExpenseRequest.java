package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ExpenseRequest {
    @NotBlank(message = "Description cannot be empty")
    @Size(max = 120, message = "Description cannot exceed 120 characters")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate spentOn;

    public ExpenseRequest() {}

    public ExpenseRequest(String title, String category, double amount, LocalDate spentOn) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.spentOn = spentOn;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getSpentOn() { return spentOn; }
    public void setSpentOn(LocalDate spentOn) { this.spentOn = spentOn; }
}
