package com.campusfood.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dish_item", uniqueConstraints = @UniqueConstraint(
    name = "uk_dish_venue_name", columnNames = {"dining_item_id", "name"}))
public class DishItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "dining_item_id", nullable = false) public Long diningItemId;
    @Column(nullable = false) public String name;
    @Column(nullable = false) public String category;
    @Column(nullable = false, precision = 8, scale = 2) public BigDecimal price;
    public String tasteTags;
    public String dietaryTags;
    public String allergens;
    public boolean signature;
    public boolean enabled = true;

    protected DishItem() {}

    public DishItem(Long diningItemId, String name, String category, BigDecimal price,
                    String tasteTags, String dietaryTags, String allergens, boolean signature) {
        this.diningItemId = diningItemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.tasteTags = tasteTags;
        this.dietaryTags = dietaryTags;
        this.allergens = allergens;
        this.signature = signature;
    }
}
