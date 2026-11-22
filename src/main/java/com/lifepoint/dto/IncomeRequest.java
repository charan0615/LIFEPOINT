package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class IncomeRequest {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 120, message = "Title cannot exceed 120 characters")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate receivedOn;

    public IncomeRequest() {}

    public IncomeRequest(String title, String category, double amount, LocalDate receivedOn) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.receivedOn = receivedOn;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getReceivedOn() { return receivedOn; }
    public void setReceivedOn(LocalDate receivedOn) { this.receivedOn = receivedOn; }
}
