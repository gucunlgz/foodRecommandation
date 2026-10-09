package com.campusfood.service;

public record RecommendationRequest(Integer budgetMax, String taste, Integer maxDistance,
                                    Double minRating, Boolean openNow, String mealPeriod,
                                    String dietaryTag, String excludedAllergen, Integer limit) {}
