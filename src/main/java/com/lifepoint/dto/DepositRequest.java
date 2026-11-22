package com.lifepoint.dto;

import jakarta.validation.constraints.Positive;

public class DepositRequest {
    @Positive(message = "Deposit amount must be positive")
    private double amount;

    public DepositRequest() {}
    public DepositRequest(double amount) { this.amount = amount; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
