package com.campusfood.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiningRepository extends JpaRepository<DiningItem, Long> {
    List<DiningItem> findByEnabledTrue();
}
