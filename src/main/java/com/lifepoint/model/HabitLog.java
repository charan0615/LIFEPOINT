package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class HabitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private Long habitId;
    private String habitTitle;
    private LocalDate completedDate;
    private double savedAmount = 0.0;

    public HabitLog() {}

    public HabitLog(String ownerEmail, Long habitId, String habitTitle, LocalDate completedDate, double savedAmount) {
        this.ownerEmail = ownerEmail;
        this.habitId = habitId;
        this.habitTitle = habitTitle;
        this.completedDate = completedDate;
        this.savedAmount = savedAmount;
    }

    public Long getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public Long getHabitId() { return habitId; }
    public void setHabitId(Long habitId) { this.habitId = habitId; }
    public String getHabitTitle() { return habitTitle; }
    public void setHabitTitle(String habitTitle) { this.habitTitle = habitTitle; }
    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }
}
