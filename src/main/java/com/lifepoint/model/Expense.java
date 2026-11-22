package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String title;
    private String category;
    private double amount;
    private LocalDate spentOn;

    public Expense() {}

    public Expense(String ownerEmail, String title, String category, double amount) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.spentOn = LocalDate.now();
    }

    public Expense(String ownerEmail, String title, String category, double amount, LocalDate spentOn) {
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.spentOn = (spentOn != null) ? spentOn : LocalDate.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getSpentOn() { return spentOn; }
    public void setSpentOn(LocalDate spentOn) { this.spentOn = spentOn; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public void update(String title, String category, double amount, LocalDate spentOn) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.spentOn = (spentOn != null) ? spentOn : this.spentOn;
    }
}
