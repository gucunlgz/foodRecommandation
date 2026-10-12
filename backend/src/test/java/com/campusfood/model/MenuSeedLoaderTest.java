package com.campusfood.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuSeedLoaderTest {
    @Test
    void categoryUsesDishMeaningNotAnyMatchingCharacter() {
        assertEquals("面条", MenuSeedLoader.categoryOf("番茄牛肉面", "面食"));
        assertEquals("早餐", MenuSeedLoader.categoryOf("全麦面包", "中式早点"));
        assertEquals("甜品", MenuSeedLoader.categoryOf("抹茶千层", "甜品"));
        assertEquals("饮品", MenuSeedLoader.categoryOf("手打柠檬茶", "饮品"));
    }
}
