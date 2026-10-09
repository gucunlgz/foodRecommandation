package com.campusfood.service;

import com.campusfood.api.ApiException;
import com.campusfood.model.DiningItem;
import com.campusfood.model.DiningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiningServiceTest {
    private DiningRepository repository;
    private DiningService service;

    @BeforeEach
    void setUp() {
        repository = mock(DiningRepository.class);
        service = new DiningService(repository);
        DiningItem safe = item("清淡蔬菜饭", 18, "清淡", "大豆");
        safe.id = 1L;
        DiningItem unsafe = item("香辣花生面", 16, "香辣", "花生,小麦");
        unsafe.id = 2L;
        DiningItem expensive = item("精品牛排", 45, "浓郁", "牛奶");
        expensive.id = 3L;
        when(repository.findByEnabledTrue()).thenReturn(List.of(safe, unsafe, expensive));
    }

    private DiningItem item(String name, int price, String taste, String allergens) {
        return new DiningItem(name, "食堂窗口", "主校区", "一食堂", "家常菜",
            BigDecimal.valueOf(price), 300, new BigDecimal("4.8"), "模拟数据", name,
            "🍚", "orange", taste, "午餐,晚餐", "", allergens, "00:00-23:59");
    }

    @Test
    void recommendationRespectsBudgetAndAllergen() {
        RecommendationRequest request = new RecommendationRequest(20, null, null, null,
            false, null, null, "花生", 5);
        @SuppressWarnings("unchecked")
        List<DiningService.Recommendation> results = (List<DiningService.Recommendation>) service.recommend(request).get("items");
        assertEquals(1, results.size());
        assertEquals("清淡蔬菜饭", results.getFirst().item().name());
        assertTrue(results.getFirst().reasons().stream().anyMatch(reason -> reason.contains("预算")));
    }

    @Test
    void emptyPreferencesAreRejected() {
        RecommendationRequest request = new RecommendationRequest(null, null, null, null,
            false, null, null, null, null);
        assertEquals("EMPTY_PREFERENCES", assertThrows(ApiException.class,
            () -> service.recommend(request)).code);
    }

    @Test
    void noMatchReturnsEmptyResultWithSuggestions() {
        RecommendationRequest request = new RecommendationRequest(1, null, null, null,
            false, null, null, null, 5);
        assertEquals(0, service.recommend(request).get("total"));
        assertFalse(((List<?>) service.recommend(request).get("suggestions")).isEmpty());
    }
}
