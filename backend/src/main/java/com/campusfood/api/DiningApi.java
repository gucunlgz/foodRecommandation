package com.campusfood.api;

import com.campusfood.service.DiningService;
import com.campusfood.service.RecommendationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DiningApi {
    private final DiningService service;
    public DiningApi(DiningService service) { this.service = service; }

    @GetMapping("/dining-items")
    public Map<String, Object> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer maxPrice,
                                    @RequestParam(required = false) String taste,
                                    @RequestParam(required = false) Integer maxDistance,
                                    @RequestParam(required = false) Double minRating,
                                    @RequestParam(required = false) Boolean openNow,
                                    @RequestParam(required = false) String mealPeriod,
                                    @RequestParam(required = false) String excludedAllergen,
                                    @RequestParam(defaultValue = "recommended") String sort,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "12") int size) {
        return Map.of("success", true, "data", service.list(keyword, maxPrice, taste, maxDistance,
            minRating, openNow, mealPeriod, excludedAllergen, sort, page, size));
    }

    @GetMapping("/dining-items/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return Map.of("success", true, "data", service.detail(id));
    }

    @PostMapping("/recommendations")
    public Map<String, Object> recommend(@Valid @RequestBody RecommendationRequest request) {
        return Map.of("success", true, "data", service.recommend(request));
    }

    @GetMapping("/meta/filter-options")
    public Map<String, Object> options() {
        return Map.of("success", true, "data", service.options());
    }

    @GetMapping("/meta/data-info")
    public Map<String, Object> dataInfo() {
        return Map.of("success", true, "data", service.dataInfo());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("success", true, "data", Map.of("status", "UP", "dataMode", "MOCK_V1"));
    }
}
