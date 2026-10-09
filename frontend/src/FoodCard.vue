<script setup lang="ts">
import type { FoodItem } from './types'
defineProps<{ item: FoodItem; rank?: number; reasons?: string[] }>()
</script>

<template>
  <RouterLink :to="`/food/${item.id}`" class="food-card">
    <div class="food-art" :class="`tone-${item.visualTone || 'orange'}`">
      <span class="art-glow"></span><span class="art-emoji">{{ item.imageEmoji || '🍽️' }}</span>
      <span v-if="rank" class="rank-pill">TOP {{ rank }}</span>
      <span class="art-type">{{ item.type }}</span>
    </div>
    <div class="card-body">
      <div class="card-heading"><h3>{{ item.name }}</h3><span class="status" :class="item.openNow ? 'open' : 'closed'">{{ item.openNow ? '营业中' : '未营业' }}</span></div>
      <p class="card-place">📍 {{ item.location }}</p>
      <div class="card-meta"><span class="rating">★ {{ item.rating ?? '暂无评分' }}</span><span>人均 <strong>¥{{ item.averagePrice ?? '—' }}</strong></span><span>{{ item.distanceMeters == null ? '距离未知' : `${item.distanceMeters}m` }}</span></div>
      <div v-if="reasons?.length" class="reason-list"><span v-for="reason in reasons.slice(0, 2)" :key="reason">{{ reason }}</span></div>
      <div v-else class="tag-list"><span v-for="tag in item.tasteTags.slice(0, 2)" :key="tag">{{ tag }}</span><span v-if="item.signatureDish">招牌 · {{ item.signatureDish }}</span></div>
    </div>
  </RouterLink>
</template>
