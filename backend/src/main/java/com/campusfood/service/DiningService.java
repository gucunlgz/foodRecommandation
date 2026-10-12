package com.campusfood.service;

import com.campusfood.api.ApiException;
import com.campusfood.model.DiningItem;
import com.campusfood.model.DiningRepository;
import com.campusfood.model.DishItem;
import com.campusfood.model.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiningService {
    private final DiningRepository repository;
    private final DishRepository dishRepository;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> TASTES = Set.of("香辣", "清淡", "咸鲜", "酸甜", "浓郁", "甜口");
    private static final Set<String> MEALS = Set.of("早餐", "午餐", "晚餐", "夜宵", "下午茶");
    private static final Set<String> ALLERGENS = Set.of("花生", "牛奶", "鸡蛋", "小麦", "大豆", "海鲜");
    private static final Set<String> DIETS = Set.of("素食", "低脂", "清真");

    public DiningService(DiningRepository repository, DishRepository dishRepository) {
        this.repository = repository;
        this.dishRepository = dishRepository;
    }

    public record DishView(Long id, String name, String category, java.math.BigDecimal price,
                           List<String> tasteTags, List<String> dietaryTags,
                           List<String> allergens, boolean signature) {}

    public record ItemView(Long id, String name, String type, String campus, String location,
                           String cuisine, java.math.BigDecimal averagePrice, Integer distanceMeters,
                           java.math.BigDecimal rating, String description, String signatureDish,
                           String imageEmoji, String visualTone, List<String> tasteTags,
                           List<String> mealPeriods, List<String> dietaryTags, List<String> allergens,
                           String businessHours, boolean openNow, String dataVersion,
                           java.time.LocalDate dataUpdatedAt, List<DishView> menu,
                           DishView matchedDish) {}

    private record Candidate(DiningItem venue, DishItem dish, int relevance) {}

    private List<String> tags(String raw) {
        return raw == null || raw.isBlank() ? List.of() : Arrays.stream(raw.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private boolean open(DiningItem item) {
        if (item.businessHours == null || item.businessHours.isBlank()) return false;
        LocalTime now = LocalTime.now(ZONE);
        for (String slot : item.businessHours.split("\\|")) {
            String[] parts = slot.split("-");
            if (parts.length != 2) continue;
            try {
                LocalTime start = LocalTime.parse(parts[0]);
                LocalTime end = LocalTime.parse(parts[1]);
                if (!end.isBefore(start) && !now.isBefore(start) && now.isBefore(end)) return true;
                if (end.isBefore(start) && (!now.isBefore(start) || now.isBefore(end))) return true;
            } catch (RuntimeException ignored) { /* invalid mock hours become unavailable */ }
        }
        return false;
    }

    private DishView dishView(DishItem dish) {
        return new DishView(dish.id, dish.name, dish.category, dish.price,
            tags(dish.tasteTags), tags(dish.dietaryTags), tags(dish.allergens), dish.signature);
    }

    private ItemView view(DiningItem i, List<DishItem> menu, DishItem matched) {
        return new ItemView(i.id, i.name, i.type, i.campus, i.location, i.cuisine,
            i.averagePrice, i.distanceMeters, i.rating, i.description, i.signatureDish,
            i.imageEmoji, i.visualTone, tags(i.tasteTags), tags(i.mealPeriods),
            tags(i.dietaryTags), tags(i.allergens), i.businessHours, open(i),
            i.dataVersion, i.dataUpdatedAt, menu.stream().map(this::dishView).toList(),
            matched == null ? null : dishView(matched));
    }

    private Map<Long, List<DishItem>> menus() {
        return dishRepository.findByEnabledTrue().stream()
            .collect(Collectors.groupingBy(dish -> dish.diningItemId));
    }

    private void validateRange(Integer value, int min, int max, String field) {
        if (value != null && (value < min || value > max))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", field + "不在有效范围内。");
    }
    private void validateEnum(String value, Set<String> options, String field) {
        if (value != null && !value.isBlank() && !options.contains(value))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", field + "不是有效选项。");
    }
    private void validateCommon(Integer budget, String taste, Integer distance, Double rating,
                                String meal, String allergen) {
        validateRange(budget, 1, 1000, "预算");
        validateRange(distance, 1, 20000, "距离");
        if (rating != null && (!Double.isFinite(rating) || rating < 0 || rating > 5))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "评分不在有效范围内。");
        validateEnum(taste, TASTES, "口味");
        validateEnum(meal, MEALS, "用餐时段");
        validateEnum(allergen, ALLERGENS, "过敏原");
    }

    private boolean matchesVenue(DiningItem i, Integer distance, Double rating,
                                 Boolean openNow, String meal) {
        if (distance != null && (i.distanceMeters == null || i.distanceMeters > distance)) return false;
        if (rating != null && (i.rating == null || i.rating.doubleValue() < rating)) return false;
        if (Boolean.TRUE.equals(openNow) && !open(i)) return false;
        if (meal != null && !meal.isBlank() && !tags(i.mealPeriods).contains(meal)) return false;
        return true;
    }

    private boolean matchesDish(DishItem dish, Integer budget, String taste,
                                String allergen, String diet) {
        if (budget != null && (dish.price == null || dish.price.doubleValue() > budget)) return false;
        if (!blank(taste) && !tags(dish.tasteTags).contains(taste)) return false;
        if (!blank(allergen) && (blank(dish.allergens) || tags(dish.allergens).contains(allergen))) return false;
        if (!blank(diet) && !tags(dish.dietaryTags).contains(diet)) return false;
        return true;
    }

    private String normalized(String value) {
        return value == null ? "" : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    }

    private int dishRelevance(DishItem dish, String q) {
        if (q.isEmpty()) return 0;
        String name = normalized(dish.name);
        if (name.equals(q)) return 100;
        if (name.contains(q)) return 80;
        String canonical = switch (q) {
            case "西红柿炒鸡蛋", "西红柿炒蛋" -> "番茄炒蛋";
            case "番茄炒鸡蛋" -> "番茄炒蛋";
            case "牛肉面" -> "牛肉拉面";
            default -> q;
        };
        if (!canonical.equals(q) && name.contains(canonical)) return 65;
        if (q.equals("面条") && dish.category.equals("面条")) return 60;
        if (normalized(dish.category).equals(q)) return 55;
        return 0;
    }

    private int venueRelevance(DiningItem venue, String q) {
        if (q.isEmpty()) return 0;
        if (normalized(venue.name).equals(q)) return 90;
        if (normalized(venue.name).contains(q)) return 70;
        if (normalized(venue.cuisine).contains(q)) return 35;
        if (normalized(venue.location).contains(q)) return 20;
        return 0;
    }

    private int relevance(DiningItem venue, DishItem dish, String q, String scope) {
        if (q.isEmpty()) return 0;
        int dishScore = dishRelevance(dish, q);
        int venueScore = venueRelevance(venue, q);
        return switch (scope) {
            case "dish" -> dishScore;
            case "venue" -> Math.max(dishScore, venueScore);
            default -> Math.max(dishScore, venueScore);
        };
    }

    private Candidate bestCandidate(DiningItem venue, List<DishItem> menu, String q,
                                    String scope, String sort, Integer budget, String taste,
                                    String allergen, String diet) {
        Comparator<Candidate> selection = sort.equals("priceAsc")
            ? Comparator.comparing((Candidate candidate) -> candidate.dish.price)
                .thenComparing(Comparator.comparingInt(Candidate::relevance).reversed())
                .thenComparing(candidate -> !candidate.dish.signature)
            : Comparator.comparingInt(Candidate::relevance).reversed()
                .thenComparing(candidate -> !candidate.dish.signature)
                .thenComparing(candidate -> candidate.dish.price);
        return menu.stream().filter(dish -> matchesDish(dish, budget, taste, allergen, diet))
            .map(dish -> new Candidate(venue, dish, relevance(venue, dish, q, scope)))
            .filter(candidate -> q.isEmpty() || candidate.relevance > 0)
            .sorted(selection.thenComparing(candidate -> candidate.dish.id))
            .findFirst().orElse(null);
    }

    public Map<String, Object> list(String keyword, Integer maxPrice, String taste,
                                    Integer maxDistance, Double minRating, Boolean openNow,
                                    String mealPeriod, String excludedAllergen,
                                    String sort, int page, int size) {
        return list(keyword, maxPrice, taste, maxDistance, minRating, openNow, mealPeriod,
            excludedAllergen, sort, "all", page, size);
    }

    public Map<String, Object> list(String keyword, Integer maxPrice, String taste,
                                    Integer maxDistance, Double minRating, Boolean openNow,
                                    String mealPeriod, String excludedAllergen,
                                    String sort, String searchScope, int page, int size) {
        validateCommon(maxPrice, taste, maxDistance, minRating, mealPeriod, excludedAllergen);
        if (keyword != null && keyword.length() > 60) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "关键词不能超过60个字。");
        if (page < 0 || size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "分页参数无效。");
        if (!Set.of("recommended", "priceAsc", "distanceAsc", "ratingDesc").contains(sort))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "排序方式无效。");
        if (!Set.of("all", "dish", "venue").contains(searchScope))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "搜索范围无效。");
        String q = normalized(keyword);
        Map<Long, List<DishItem>> menus = menus();
        Comparator<Candidate> comparator = switch (sort) {
            case "priceAsc" -> Comparator.comparing(candidate -> candidate.dish.price);
            case "distanceAsc" -> Comparator.comparing(candidate -> candidate.venue.distanceMeters,
                Comparator.nullsLast(Comparator.naturalOrder()));
            case "ratingDesc" -> Comparator.comparing((Candidate candidate) -> candidate.venue.rating,
                Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparingInt(Candidate::relevance).reversed()
                .thenComparing(candidate -> candidate.venue.rating, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        List<Candidate> all = repository.findByEnabledTrue().stream()
            .filter(venue -> matchesVenue(venue, maxDistance, minRating, openNow, mealPeriod))
            .flatMap(venue -> {
                List<DishItem> menu = menus.getOrDefault(venue.id, List.of());
                if (searchScope.equals("dish")) return menu.stream()
                    .filter(dish -> matchesDish(dish, maxPrice, taste, excludedAllergen, null))
                    .map(dish -> new Candidate(venue, dish, dishRelevance(dish, q)))
                    .filter(candidate -> q.isEmpty() || candidate.relevance > 0);
                Candidate best = bestCandidate(venue, menu, q, searchScope, sort,
                    maxPrice, taste, excludedAllergen, null);
                return best == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(best);
            })
            .sorted(comparator.thenComparing(candidate -> candidate.venue.id)
                .thenComparing(candidate -> candidate.dish.id)).toList();
        long from = (long) page * size;
        List<ItemView> content = from >= all.size() ? List.of() : all.subList((int) from,
            (int) Math.min(from + size, all.size())).stream()
            .map(candidate -> view(candidate.venue, menus.get(candidate.venue.id), candidate.dish)).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content); result.put("total", all.size());
        result.put("page", page); result.put("size", size);
        result.put("totalPages", (all.size() + size - 1) / size);
        return result;
    }

    public ItemView detail(Long id) {
        DiningItem item = repository.findById(id).filter(i -> i.enabled)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "没有找到这条餐饮信息。"));
        List<DishItem> menu = dishRepository.findByDiningItemIdAndEnabledTrue(id);
        DishItem signature = menu.stream().filter(dish -> dish.signature).findFirst()
            .orElse(menu.isEmpty() ? null : menu.getFirst());
        return view(item, menu, signature);
    }

    public record Recommendation(ItemView item, int score, List<String> reasons) {}

    private record ScoredDish(DiningItem venue, DishItem dish, int score, List<String> reasons) {}

    public Map<String, Object> recommend(RecommendationRequest r) {
        validateCommon(r.budgetMax(), r.taste(), r.maxDistance(), r.minRating(), r.mealPeriod(), r.excludedAllergen());
        validateEnum(r.dietaryTag(), DIETS, "饮食偏好");
        int limit = r.limit() == null ? 5 : r.limit();
        validateRange(limit, 3, 5, "推荐数量");
        if (r.budgetMax() == null && blank(r.taste()) && r.maxDistance() == null && r.minRating() == null
            && !Boolean.TRUE.equals(r.openNow()) && blank(r.mealPeriod()) && blank(r.dietaryTag())
            && blank(r.excludedAllergen()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_PREFERENCES", "请至少选择一项偏好，再获取推荐。");
        Map<Long, List<DishItem>> menus = menus();
        Comparator<ScoredDish> ranking = Comparator.comparingInt(ScoredDish::score).reversed()
            .thenComparing(result -> result.venue.rating, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(result -> result.venue.distanceMeters, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(result -> result.venue.id);
        List<Recommendation> recommendations = repository.findByEnabledTrue().stream()
            .filter(venue -> matchesVenue(venue, r.maxDistance(), r.minRating(),
                r.openNow(), r.mealPeriod()))
            .map(venue -> menus.getOrDefault(venue.id, List.of()).stream()
                .filter(dish -> matchesDish(dish, r.budgetMax(), null,
                    r.excludedAllergen(), r.dietaryTag()))
                .map(dish -> score(venue, dish, r))
                .sorted(ranking).findFirst().orElse(null))
            .filter(Objects::nonNull)
            .sorted(ranking).limit(limit)
            .map(result -> new Recommendation(view(result.venue,
                menus.get(result.venue.id), result.dish), result.score, result.reasons))
            .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", recommendations); result.put("total", recommendations.size());
        result.put("algorithmVersion", "DISH_RULE_V2");
        result.put("suggestions", recommendations.isEmpty() ? List.of("适当提高预算", "扩大距离范围", "减少筛选条件") : List.of());
        return result;
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private ScoredDish score(DiningItem venue, DishItem dish, RecommendationRequest r) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        reasons.add("推荐菜品：" + dish.name);
        if (!blank(r.taste()) && tags(dish.tasteTags).contains(r.taste())) {
            score += 30; reasons.add("符合你喜欢的" + r.taste() + "口味");
        }
        if (r.budgetMax() != null && dish.price != null) {
            score += Math.max(5, 20 - (int) Math.round(10.0 * dish.price.doubleValue() / r.budgetMax()));
            reasons.add("菜品¥" + dish.price.stripTrailingZeros().toPlainString() + "，符合预算");
        }
        if (venue.distanceMeters != null) {
            score += Math.max(0, 20 - venue.distanceMeters / 100);
            if (r.maxDistance() != null || venue.distanceMeters <= 800)
                reasons.add("距离约" + venue.distanceMeters + "米");
        }
        if (venue.rating != null) {
            score += (int) Math.round(venue.rating.doubleValue() * 3);
            if (venue.rating.doubleValue() >= 4.5) reasons.add("模拟评分" + venue.rating + "分");
        }
        if (!blank(r.mealPeriod()) && tags(venue.mealPeriods).contains(r.mealPeriod())) {
            score += 10; reasons.add("适合" + r.mealPeriod());
        }
        if (!blank(r.dietaryTag()) && tags(dish.dietaryTags).contains(r.dietaryTag())) {
            score += 5; reasons.add("支持" + r.dietaryTag() + "需求");
        }
        if (Boolean.TRUE.equals(r.openNow()) && open(venue)) reasons.add("当前模拟营业时间内");
        return new ScoredDish(venue, dish, score, reasons.stream().limit(4).toList());
    }

    public Map<String, Object> options() {
        return Map.of("tastes", TASTES.stream().sorted().toList(), "mealPeriods", MEALS.stream().sorted().toList(),
            "allergens", ALLERGENS.stream().sorted().toList(), "dietaryTags", DIETS.stream().sorted().toList());
    }

    public Map<String, Object> dataInfo() {
        return Map.of("isMock", true, "version", "MOCK_V2", "updatedAt", "2026-10-09",
            "total", repository.count(), "dishTotal", dishRepository.count(),
            "notice", "全部为课程模拟数据，仅供功能演示；价格、评分、距离和营业状态不代表真实信息。",
            "allergenNotice", "模拟过敏原信息不构成食品安全保证，实际用餐请向商家现场确认。");
    }
}
