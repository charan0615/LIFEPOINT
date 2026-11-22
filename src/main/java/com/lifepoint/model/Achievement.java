package com.lifepoint.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String badgeCode;
    private String title;
    private String description;
    private String icon;
    private LocalDate unlockedOn;

    public Achievement() {}

    public Achievement(String ownerEmail, String badgeCode, String title, String description, String icon) {
        this.ownerEmail = ownerEmail;
        this.badgeCode = badgeCode;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.unlockedOn = LocalDate.now();
    }

    public Long getId() { return id; }
    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public LocalDate getUnlockedOn() { return unlockedOn; }
    public void setUnlockedOn(LocalDate unlockedOn) { this.unlockedOn = unlockedOn; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
