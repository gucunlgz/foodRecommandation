package com.campusfood.service;

import com.campusfood.api.ApiException;
import com.campusfood.model.DiningItem;
import com.campusfood.model.DiningRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiningService {
    private final DiningRepository repository;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> TASTES = Set.of("香辣", "清淡", "咸鲜", "酸甜", "浓郁", "甜口");
    private static final Set<String> MEALS = Set.of("早餐", "午餐", "晚餐", "夜宵", "下午茶");
    private static final Set<String> ALLERGENS = Set.of("花生", "牛奶", "鸡蛋", "小麦", "大豆", "海鲜");
    private static final Set<String> DIETS = Set.of("素食", "低脂", "清真");

    public DiningService(DiningRepository repository) { this.repository = repository; }

    public record ItemView(Long id, String name, String type, String campus, String location,
                           String cuisine, java.math.BigDecimal averagePrice, Integer distanceMeters,
                           java.math.BigDecimal rating, String description, String signatureDish,
                           String imageEmoji, String visualTone, List<String> tasteTags,
                           List<String> mealPeriods, List<String> dietaryTags, List<String> allergens,
                           String businessHours, boolean openNow, String dataVersion,
                           java.time.LocalDate dataUpdatedAt) {}

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

    private ItemView view(DiningItem i) {
        return new ItemView(i.id, i.name, i.type, i.campus, i.location, i.cuisine,
            i.averagePrice, i.distanceMeters, i.rating, i.description, i.signatureDish,
            i.imageEmoji, i.visualTone, tags(i.tasteTags), tags(i.mealPeriods),
            tags(i.dietaryTags), tags(i.allergens), i.businessHours, open(i),
            i.dataVersion, i.dataUpdatedAt);
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

    private boolean matches(DiningItem i, String keyword, Integer budget, String taste,
                            Integer distance, Double rating, Boolean openNow, String meal,
                            String allergen, String diet) {
        String q = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        if (!q.isEmpty() && !(i.name + " " + i.location + " " + i.cuisine + " " + i.signatureDish)
            .toLowerCase(Locale.ROOT).contains(q)) return false;
        if (budget != null && (i.averagePrice == null || i.averagePrice.doubleValue() > budget)) return false;
        if (taste != null && !taste.isBlank() && !tags(i.tasteTags).contains(taste)) return false;
        if (distance != null && (i.distanceMeters == null || i.distanceMeters > distance)) return false;
        if (rating != null && (i.rating == null || i.rating.doubleValue() < rating)) return false;
        if (Boolean.TRUE.equals(openNow) && !open(i)) return false;
        if (meal != null && !meal.isBlank() && !tags(i.mealPeriods).contains(meal)) return false;
        if (allergen != null && !allergen.isBlank()
            && (i.allergens == null || i.allergens.isBlank() || tags(i.allergens).contains(allergen))) return false;
        if (diet != null && !diet.isBlank() && !tags(i.dietaryTags).contains(diet)) return false;
        return true;
    }

    public Map<String, Object> list(String keyword, Integer maxPrice, String taste,
                                    Integer maxDistance, Double minRating, Boolean openNow,
                                    String mealPeriod, String excludedAllergen,
                                    String sort, int page, int size) {
        validateCommon(maxPrice, taste, maxDistance, minRating, mealPeriod, excludedAllergen);
        if (keyword != null && keyword.length() > 60) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "关键词不能超过60个字。");
        if (page < 0 || size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "分页参数无效。");
        if (!Set.of("recommended", "priceAsc", "distanceAsc", "ratingDesc").contains(sort))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "排序方式无效。");
        Comparator<DiningItem> comparator = switch (sort) {
            case "priceAsc" -> Comparator.comparing(i -> i.averagePrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "distanceAsc" -> Comparator.comparing(i -> i.distanceMeters, Comparator.nullsLast(Comparator.naturalOrder()));
            case "ratingDesc" -> Comparator.comparing((DiningItem i) -> i.rating, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing((DiningItem i) -> i.rating, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        List<DiningItem> all = repository.findByEnabledTrue().stream()
            .filter(i -> matches(i, keyword, maxPrice, taste, maxDistance, minRating,
                openNow, mealPeriod, excludedAllergen, null))
            .sorted(comparator.thenComparing(i -> i.id)).toList();
        long from = (long) page * size;
        List<ItemView> content = from >= all.size() ? List.of() : all.subList((int) from,
            (int) Math.min(from + size, all.size())).stream().map(this::view).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content); result.put("total", all.size());
        result.put("page", page); result.put("size", size);
        result.put("totalPages", (all.size() + size - 1) / size);
        return result;
    }

    public ItemView detail(Long id) {
        DiningItem item = repository.findById(id).filter(i -> i.enabled)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "没有找到这条餐饮信息。"));
        return view(item);
    }

    public record Recommendation(ItemView item, int score, List<String> reasons) {}

    public Map<String, Object> recommend(RecommendationRequest r) {
        validateCommon(r.budgetMax(), r.taste(), r.maxDistance(), r.minRating(), r.mealPeriod(), r.excludedAllergen());
        validateEnum(r.dietaryTag(), DIETS, "饮食偏好");
        int limit = r.limit() == null ? 5 : r.limit();
        validateRange(limit, 3, 5, "推荐数量");
        if (r.budgetMax() == null && blank(r.taste()) && r.maxDistance() == null && r.minRating() == null
            && !Boolean.TRUE.equals(r.openNow()) && blank(r.mealPeriod()) && blank(r.dietaryTag())
            && blank(r.excludedAllergen()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_PREFERENCES", "请至少选择一项偏好，再获取推荐。");
        List<Recommendation> recommendations = repository.findByEnabledTrue().stream()
            .filter(i -> matches(i, null, r.budgetMax(), null, r.maxDistance(), r.minRating(),
                r.openNow(), null, r.excludedAllergen(), null))
            .filter(i -> blank(r.dietaryTag()) || tags(i.dietaryTags).contains(r.dietaryTag()))
            .map(i -> score(i, r))
            .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                .thenComparing(x -> x.item.rating(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(x -> x.item.distanceMeters(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(x -> x.item.id()))
            .limit(limit).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", recommendations); result.put("total", recommendations.size());
        result.put("algorithmVersion", "RULE_V1");
        result.put("suggestions", recommendations.isEmpty() ? List.of("适当提高预算", "扩大距离范围", "减少筛选条件") : List.of());
        return result;
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private Recommendation score(DiningItem i, RecommendationRequest r) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (!blank(r.taste()) && tags(i.tasteTags).contains(r.taste())) {
            score += 30; reasons.add("符合你喜欢的" + r.taste() + "口味");
        }
        if (r.budgetMax() != null && i.averagePrice != null) {
            score += Math.max(5, 20 - (int) Math.round(10.0 * i.averagePrice.doubleValue() / r.budgetMax()));
            reasons.add("人均约¥" + i.averagePrice.stripTrailingZeros().toPlainString() + "，符合预算");
        }
        if (i.distanceMeters != null) {
            score += Math.max(0, 20 - i.distanceMeters / 100);
            if (r.maxDistance() != null || i.distanceMeters <= 800)
                reasons.add("距离约" + i.distanceMeters + "米");
        }
        if (i.rating != null) {
            score += (int) Math.round(i.rating.doubleValue() * 3);
            if (i.rating.doubleValue() >= 4.5) reasons.add("模拟评分" + i.rating + "分");
        }
        if (!blank(r.mealPeriod()) && tags(i.mealPeriods).contains(r.mealPeriod())) {
            score += 10; reasons.add("适合" + r.mealPeriod());
        }
        if (!blank(r.dietaryTag()) && tags(i.dietaryTags).contains(r.dietaryTag())) {
            score += 5; reasons.add("支持" + r.dietaryTag() + "需求");
        }
        if (Boolean.TRUE.equals(r.openNow()) && open(i)) reasons.add("当前模拟营业时间内");
        if (reasons.isEmpty()) reasons.add("综合评分、价格与距离较合适");
        return new Recommendation(view(i), score, reasons.stream().limit(4).toList());
    }

    public Map<String, Object> options() {
        return Map.of("tastes", TASTES.stream().sorted().toList(), "mealPeriods", MEALS.stream().sorted().toList(),
            "allergens", ALLERGENS.stream().sorted().toList(), "dietaryTags", DIETS.stream().sorted().toList());
    }

    public Map<String, Object> dataInfo() {
        return Map.of("isMock", true, "version", "MOCK_V1", "updatedAt", "2026-10-09",
            "total", repository.count(), "notice", "全部为课程模拟数据，仅供功能演示；价格、评分、距离和营业状态不代表真实信息。",
            "allergenNotice", "模拟过敏原信息不构成食品安全保证，实际用餐请向商家现场确认。");
    }
}
