package com.campusfood.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Idempotently expands each existing mock venue into a three-dish mock menu. */
final class MenuSeedLoader {
    private MenuSeedLoader() {}

    private record Sample(String name, String category, int priceOffset,
                          String tastes, String diets, String allergens) {}

    static void seed(DiningRepository venues, DishRepository dishes) {
        List<DishItem> additions = new ArrayList<>();
        for (DiningItem venue : venues.findAll()) {
            if (venue.id == null || venue.signatureDish == null || venue.signatureDish.isBlank()) continue;
            if (!"MOCK_V2".equals(venue.dataVersion)) {
                venue.dataVersion = "MOCK_V2";
                venues.save(venue);
            }
            List<DishItem> existingDishes = dishes.findByDiningItemId(venue.id);
            Set<String> names = existingDishes.stream()
                .map(dish -> dish.name).collect(Collectors.toSet());
            for (DishItem existing : existingDishes) {
                String category = categoryOf(existing.name, venue.cuisine);
                if (existing.signature && !existing.category.equals(category)) {
                    existing.category = category;
                    dishes.save(existing);
                }
            }
            if (!names.contains(venue.signatureDish)) {
                additions.add(new DishItem(venue.id, venue.signatureDish,
                    categoryOf(venue.signatureDish, venue.cuisine), venue.averagePrice,
                    venue.tasteTags, venue.dietaryTags, venue.allergens, true));
            }
            for (Sample sample : samples(venue.cuisine)) {
                if (names.contains(sample.name) || sample.name.equals(venue.signatureDish)) continue;
                String diets = sample.diets;
                if (venue.dietaryTags != null && List.of(venue.dietaryTags.split(",")).contains("清真"))
                    diets = diets.isBlank() ? "清真" : diets + ",清真";
                additions.add(new DishItem(venue.id, sample.name, sample.category,
                    BigDecimal.valueOf(Math.max(6, venue.averagePrice.intValue() + sample.priceOffset)),
                    sample.tastes, diets, sample.allergens, false));
            }
        }
        dishes.saveAll(additions);
    }

    static String categoryOf(String name, String cuisine) {
        if (name.contains("米线")) return "米线";
        if (name.contains("面") && !name.contains("面包")) return "面条";
        if (name.contains("馄饨")) return "馄饨";
        if (name.contains("饭")) return "饭类";
        if (name.contains("咖啡") || name.contains("拿铁") || name.endsWith("茶")
            || name.contains("果昔") || name.contains("气泡水")) return "饮品";
        if (name.contains("包") || name.contains("饼") || name.contains("肠粉")) return "早餐";
        return switch (cuisine == null ? "" : cuisine) {
            case "甜品" -> "甜品";
            case "饮品", "咖啡" -> "饮品";
            case "火锅" -> "火锅";
            case "轻食" -> "轻食";
            case "汤品" -> "汤品";
            case "砂锅" -> "砂锅";
            case "中式早点" -> "早餐";
            case "米线" -> "米线";
            case "烧烤" -> "烧烤";
            default -> "特色菜";
        };
    }

    private static List<Sample> samples(String cuisine) {
        return switch (cuisine == null ? "" : cuisine) {
            case "面食" -> List.of(
                new Sample("香菇鸡汤面", "面条", -2, "清淡,咸鲜", "", "小麦,鸡蛋"),
                new Sample("麻酱拌面", "面条", 1, "浓郁,咸鲜", "素食", "小麦,花生,大豆"));
            case "米线" -> List.of(
                new Sample("酸汤牛肉米线", "米线", 3, "酸甜,香辣", "", "大豆"),
                new Sample("菌菇清汤米线", "米线", -2, "清淡,咸鲜", "素食", "大豆"));
            case "川菜" -> List.of(
                new Sample("宫保鸡丁饭", "饭类", 3, "香辣,咸鲜", "", "花生,大豆"),
                new Sample("番茄炒蛋饭", "饭类", -2, "酸甜,清淡", "素食", "鸡蛋"));
            case "轻食" -> List.of(
                new Sample("鸡胸藜麦碗", "轻食", 2, "清淡,咸鲜", "低脂", "鸡蛋"),
                new Sample("牛油果蔬菜碗", "轻食", -1, "清淡", "素食,低脂", ""));
            case "日式简餐" -> List.of(
                new Sample("照烧鸡肉饭", "饭类", 4, "咸鲜,浓郁", "", "大豆,小麦"),
                new Sample("蔬菜紫菜饭卷", "饭类", -1, "清淡,咸鲜", "素食", "大豆"));
            case "烧烤" -> List.of(
                new Sample("孜然鸡肉串饭", "饭类", 2, "香辣,浓郁", "", "花生,大豆"),
                new Sample("烤蔬菜拼盘", "烧烤", -3, "咸鲜", "素食", "大豆"));
            case "砂锅" -> List.of(
                new Sample("番茄牛腩砂锅", "砂锅", 4, "酸甜,浓郁", "", "大豆"),
                new Sample("菌菇豆腐砂锅", "砂锅", -2, "清淡,咸鲜", "素食", "大豆"));
            case "中式早点" -> List.of(
                new Sample("鲜肉小笼包", "早餐", 2, "咸鲜", "", "小麦,大豆"),
                new Sample("玉米鸡蛋羹", "早餐", -2, "清淡", "素食", "鸡蛋"));
            case "甜品" -> List.of(
                new Sample("抹茶千层", "甜品", 3, "甜口", "素食", "牛奶,鸡蛋,小麦"),
                new Sample("红豆椰奶杯", "甜品", -2, "甜口,清淡", "素食", "牛奶"));
            case "家常菜" -> List.of(
                new Sample("香菇鸡肉盖饭", "饭类", 2, "咸鲜,清淡", "", "大豆"),
                new Sample("家常豆腐饭", "饭类", -2, "咸鲜,清淡", "素食", "大豆,小麦"));
            case "西式简餐" -> List.of(
                new Sample("奶油蘑菇意面", "面条", 2, "浓郁,咸鲜", "素食", "小麦,牛奶"),
                new Sample("黑椒鸡肉意面", "面条", 4, "浓郁,咸鲜", "", "小麦,大豆"));
            case "素食" -> List.of(
                new Sample("黑椒菌菇饭", "饭类", 2, "清淡,浓郁", "素食", "大豆"),
                new Sample("番茄豆腐饭", "饭类", -2, "酸甜,清淡", "素食,低脂", "大豆"));
            case "咖喱" -> List.of(
                new Sample("日式咖喱鸡饭", "饭类", 3, "浓郁,咸鲜", "", "牛奶,小麦"),
                new Sample("咖喱时蔬饭", "饭类", -2, "浓郁,清淡", "素食", "牛奶"));
            case "咖啡" -> List.of(
                new Sample("燕麦拿铁", "饮品", 2, "甜口,浓郁", "素食", "牛奶"),
                new Sample("美式咖啡", "饮品", -3, "浓郁", "素食", ""));
            case "粤式简餐" -> List.of(
                new Sample("腊味煲仔饭", "饭类", 3, "咸鲜,浓郁", "", "大豆"),
                new Sample("鲜虾鸡蛋肠粉", "早餐", -2, "清淡,咸鲜", "", "海鲜,鸡蛋"));
            case "快餐" -> List.of(
                new Sample("蒜香鸡腿饭", "饭类", 2, "浓郁,咸鲜", "", "大豆"),
                new Sample("番茄鸡蛋饭", "饭类", -3, "酸甜,清淡", "素食", "鸡蛋"));
            case "饮品" -> List.of(
                new Sample("手打柠檬茶", "饮品", 1, "酸甜,甜口", "素食", ""),
                new Sample("鲜橙气泡水", "饮品", -2, "酸甜", "素食", ""));
            case "西北风味" -> List.of(
                new Sample("清汤牛肉拉面", "面条", -1, "清淡,咸鲜", "", "小麦"),
                new Sample("孜然牛肉饭", "饭类", 3, "浓郁,咸鲜", "", "小麦"));
            case "东南亚风味" -> List.of(
                new Sample("冬阴功海鲜米线", "米线", 2, "酸甜,香辣", "", "海鲜"),
                new Sample("椰香咖喱鸡饭", "饭类", -1, "浓郁,咸鲜", "", "牛奶"));
            case "火锅" -> List.of(
                new Sample("单人麻辣小火锅", "火锅", 3, "香辣,浓郁", "", "花生,大豆"),
                new Sample("番茄菌菇小火锅", "火锅", -3, "酸甜,清淡", "素食", "大豆"));
            case "汤品" -> List.of(
                new Sample("鲜鱼丸汤饭", "汤品", 2, "清淡,咸鲜", "", "海鲜"),
                new Sample("菌菇豆腐汤", "汤品", -3, "清淡", "素食", "大豆"));
            default -> List.of(
                new Sample("家常鸡肉饭", "饭类", 2, "咸鲜", "", "大豆"),
                new Sample("清炒时蔬饭", "饭类", -2, "清淡", "素食", "大豆"));
        };
    }
}
