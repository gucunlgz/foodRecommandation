package com.campusfood.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dining_item")
public class DiningItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    @Column(nullable = false) public String type;
    @Column(nullable = false) public String campus;
    @Column(nullable = false) public String location;
    public String cuisine;
    @Column(precision = 8, scale = 2) public BigDecimal averagePrice;
    public Integer distanceMeters;
    @Column(precision = 2, scale = 1) public BigDecimal rating;
    @Column(length = 1000) public String description;
    public String signatureDish;
    public String imageEmoji;
    public String visualTone;
    public String tasteTags;
    public String mealPeriods;
    public String dietaryTags;
    public String allergens;
    public String businessHours;
    public String dataVersion = "MOCK_V1";
    public LocalDate dataUpdatedAt = LocalDate.of(2026, 10, 9);
    public boolean enabled = true;

    protected DiningItem() {}

    public DiningItem(String name, String type, String campus, String location, String cuisine,
                      BigDecimal averagePrice, Integer distanceMeters, BigDecimal rating,
                      String description, String signatureDish, String imageEmoji, String visualTone,
                      String tasteTags, String mealPeriods, String dietaryTags, String allergens, String businessHours) {
        this.name = name; this.type = type; this.campus = campus; this.location = location;
        this.cuisine = cuisine; this.averagePrice = averagePrice; this.distanceMeters = distanceMeters;
        this.rating = rating; this.description = description; this.signatureDish = signatureDish;
        this.imageEmoji = imageEmoji; this.visualTone = visualTone; this.tasteTags = tasteTags;
        this.mealPeriods = mealPeriods; this.dietaryTags = dietaryTags; this.allergens = allergens;
        this.businessHours = businessHours;
    }
}
