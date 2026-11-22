package com.lifepoint.dto;

import jakarta.validation.constraints.NotBlank;

public class CurrencyRequest {
    @NotBlank(message = "Currency code is required")
    private String currency;

    public CurrencyRequest() {}
    public CurrencyRequest(String currency) { this.currency = currency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
