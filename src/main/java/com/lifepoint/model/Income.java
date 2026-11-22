package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Income {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String title;
    private String category = "Salary";
    private double amount;
    private LocalDate receivedOn;

    public Income() {}

    public Income(String ownerEmail, String title, String category, double amount) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.category = (category == null || category.isBlank()) ? "Salary" : category;
        this.amount = amount;
        this.receivedOn = LocalDate.now();
    }

    public Income(String ownerEmail, String title, String category, double amount, LocalDate receivedOn) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.category = (category == null || category.isBlank()) ? "Salary" : category;
        this.amount = amount;
        this.receivedOn = (receivedOn != null) ? receivedOn : LocalDate.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getReceivedOn() { return receivedOn; }
    public void setReceivedOn(LocalDate receivedOn) { this.receivedOn = receivedOn; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public void update(String title, String category, double amount, LocalDate receivedOn) {
        this.title = title;
        this.category = (category == null || category.isBlank()) ? "Salary" : category;
        this.amount = amount;
        this.receivedOn = (receivedOn != null) ? receivedOn : this.receivedOn;
    }
}
