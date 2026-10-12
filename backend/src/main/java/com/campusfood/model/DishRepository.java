package com.campusfood.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DishRepository extends JpaRepository<DishItem, Long> {
    List<DishItem> findByEnabledTrue();
    List<DishItem> findByDiningItemId(Long diningItemId);
    List<DishItem> findByDiningItemIdAndEnabledTrue(Long diningItemId);
}
