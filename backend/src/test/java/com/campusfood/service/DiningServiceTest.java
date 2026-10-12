package com.campusfood.service;

import com.campusfood.api.ApiException;
import com.campusfood.model.DiningItem;
import com.campusfood.model.DiningRepository;
import com.campusfood.model.DishItem;
import com.campusfood.model.DishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiningServiceTest {
    private DiningRepository venues;
    private DishRepository dishes;
    private DiningService service;

    @BeforeEach
    void setUp() {
        venues = mock(DiningRepository.class);
        dishes = mock(DishRepository.class);
        service = new DiningService(venues, dishes);
        DiningItem noodleShop = venue(1L, "校园面馆", 30);
        DiningItem bakery = venue(2L, "西区面包房", 12);
        DishItem noodle = dish(1L, 1L, "红烧牛肉拉面", "面条", 28, "浓郁", "", "小麦", true);
        DishItem safe = dish(2L, 1L, "番茄炒蛋饭", "饭类", 16, "清淡", "素食", "鸡蛋", false);
        DishItem unsafe = dish(3L, 1L, "麻酱拌面", "面条", 18, "浓郁", "素食", "花生,小麦", false);
        DishItem bread = dish(4L, 2L, "全麦面包", "早餐", 12, "清淡", "素食", "小麦", true);
        when(venues.findByEnabledTrue()).thenReturn(List.of(noodleShop, bakery));
        when(venues.findById(1L)).thenReturn(Optional.of(noodleShop));
        when(dishes.findByEnabledTrue()).thenReturn(List.of(noodle, safe, unsafe, bread));
        when(dishes.findByDiningItemIdAndEnabledTrue(1L)).thenReturn(List.of(noodle, safe, unsafe));
    }

    private DiningItem venue(Long id, String name, int averagePrice) {
        DiningItem item = new DiningItem(name, "食堂窗口", "主校区", "一食堂", "面食",
            BigDecimal.valueOf(averagePrice), 300, new BigDecimal("4.8"), "模拟数据", name,
            "🍜", "orange", "浓郁", "午餐,晚餐", "", "小麦", "00:00-23:59");
        item.id = id;
        return item;
    }

    private DishItem dish(Long id, Long venueId, String name, String category, int price,
                          String taste, String diet, String allergens, boolean signature) {
        DishItem item = new DishItem(venueId, name, category, BigDecimal.valueOf(price),
            taste, diet, allergens, signature);
        item.id = id;
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<DiningService.ItemView> results(String keyword, Integer budget, String allergen,
                                                  String scope) {
        return (List<DiningService.ItemView>) service.list(keyword, budget, null, null,
            null, false, null, allergen, "recommended", scope, 0, 20).get("content");
    }

    @Test
    void genericNoodleQueryFindsNoodlesButNotBread() {
        List<DiningService.ItemView> matches = results("面条", null, null, "dish");
        assertEquals(2, matches.size());
        assertEquals("校园面馆", matches.getFirst().name());
        assertEquals("红烧牛肉拉面", matches.getFirst().matchedDish().name());
        assertEquals("麻酱拌面", matches.get(1).matchedDish().name());
        assertEquals(1, results("面条", null, null, "venue").size());
    }

    @Test
    void searchCanSeparateDishAndVenueResults() {
        assertEquals(0, results("校园面馆", null, null, "dish").size());
        assertEquals(1, results("校园面馆", null, null, "venue").size());
        assertEquals(1, results("牛肉面", null, null, "dish").size());
    }

    @Test
    void synonymMatchesActualDish() {
        assertEquals("番茄炒蛋饭", results("西红柿炒鸡蛋", null, null, "dish")
            .getFirst().matchedDish().name());
    }

    @Test
    void budgetAndAllergenUseTheMatchedDishNotVenueAverage() {
        List<DiningService.ItemView> matches = results(null, 20, "花生", "all");
        assertEquals("番茄炒蛋饭", matches.getFirst().matchedDish().name());
        assertEquals(16, matches.getFirst().matchedDish().price().intValue());
        assertEquals(30, matches.getFirst().averagePrice().intValue());
    }

    @Test
    void priceSortUsesTheCheapestMatchingDish() {
        @SuppressWarnings("unchecked")
        List<DiningService.ItemView> matches = (List<DiningService.ItemView>) service.list(
            null, null, null, null, null, false, null, null,
            "priceAsc", "venue", 0, 20).get("content");
        DiningService.ItemView noodleShop = matches.stream()
            .filter(match -> match.id().equals(1L)).findFirst().orElseThrow();
        assertEquals("番茄炒蛋饭", noodleShop.matchedDish().name());
        assertEquals(16, noodleShop.matchedDish().price().intValue());
    }

    @Test
    void recommendationExplainsTheChosenDishAndRespectsItsRestrictions() {
        RecommendationRequest request = new RecommendationRequest(20, null, null, null,
            false, null, null, "花生", 5);
        @SuppressWarnings("unchecked")
        List<DiningService.Recommendation> matches = (List<DiningService.Recommendation>) service.recommend(request).get("items");
        DiningService.Recommendation chosen = matches.stream()
            .filter(match -> match.item().id().equals(1L)).findFirst().orElseThrow();
        assertEquals("番茄炒蛋饭", chosen.item().matchedDish().name());
        assertTrue(chosen.reasons().stream().anyMatch(reason -> reason.contains("番茄炒蛋饭")));
        assertTrue(chosen.reasons().stream().anyMatch(reason -> reason.contains("¥16")));
    }

    @Test
    void detailContainsAllAvailableDishes() {
        assertEquals(3, service.detail(1L).menu().size());
    }

    @Test
    void emptyPreferencesAreRejected() {
        RecommendationRequest request = new RecommendationRequest(null, null, null, null,
            false, null, null, null, null);
        assertEquals("EMPTY_PREFERENCES", assertThrows(ApiException.class,
            () -> service.recommend(request)).code);
    }

    @Test
    void noMatchReturnsSuggestions() {
        RecommendationRequest request = new RecommendationRequest(1, null, null, null,
            false, null, null, null, 5);
        assertEquals(0, service.recommend(request).get("total"));
        assertFalse(((List<?>) service.recommend(request).get("suggestions")).isEmpty());
    }
}
