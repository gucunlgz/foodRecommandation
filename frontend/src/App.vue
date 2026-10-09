<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import FoodCard from './FoodCard.vue'
import { getDataInfo, getItem, getItems, getRecommendations } from './api'
import { useFoodStore } from './store'
import type { DataInfo, FoodItem, ListResult, RecommendationResult } from './types'

const router = useRouter()
const route = useRoute()
const store = useFoodStore()
const { filters, preferences } = storeToRefs(store)
const page = computed(() => route.path === '/' ? 'home' : route.path.startsWith('/food/') ? 'detail' : route.path.slice(1))
const loading = ref(false)
const error = ref('')
const formError = ref('')
const list = ref<ListResult | null>(null)
const featured = ref<FoodItem[]>([])
const detail = ref<FoodItem | null>(null)
const recommendations = ref<RecommendationResult | null>(null)
const dataInfo = ref<DataInfo | null>(null)
const showMoreFilters = ref(false)
const currentPage = ref(0)
let activeRequest = 0

const tasteOptions = ['香辣', '清淡', '咸鲜', '酸甜', '浓郁', '甜口']
const mealOptions = ['早餐', '午餐', '晚餐', '夜宵', '下午茶']
const allergenOptions = ['花生', '牛奶', '鸡蛋', '小麦', '大豆', '海鲜']

function listParams() {
  const p = new URLSearchParams()
  Object.entries(filters.value).forEach(([key, value]) => {
    if (value !== '' && value !== false && !(key === 'sort' && value === 'recommended')) p.set(key, String(value))
  })
  p.set('page', String(currentPage.value))
  p.set('size', '9')
  return p
}

async function loadList() {
  const id = ++activeRequest
  loading.value = true; error.value = ''
  try { const result = await getItems(listParams()); if (id === activeRequest) list.value = result }
  catch (e) { if (id === activeRequest) error.value = (e as Error).message }
  finally { if (id === activeRequest) loading.value = false }
}

async function loadDetail() {
  const id = ++activeRequest
  loading.value = true; error.value = ''; detail.value = null
  try { const result = await getItem(String(route.params.id)); if (id === activeRequest) detail.value = result }
  catch (e) { if (id === activeRequest) error.value = (e as Error).message }
  finally { if (id === activeRequest) loading.value = false }
}

function submitSearch() {
  currentPage.value = 0
  if (page.value !== 'explore') router.push('/explore')
  else loadList()
}

function setShortcut(key: 'taste' | 'mealPeriod' | 'maxPrice' | 'openNow', value: string | boolean) {
  store.resetFilters()
  if (key === 'openNow') filters.value.openNow = Boolean(value)
  else filters.value[key] = String(value)
  currentPage.value = 0
  if (page.value !== 'explore') router.push('/explore')
  else loadList()
}

function resetFilters() { store.resetFilters(); currentPage.value = 0; loadList() }
function changePage(next: number) { currentPage.value = next; loadList(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
function scrollToPreferences() { document.querySelector('.preference-panel')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }

async function submitRecommendation() {
  formError.value = ''; error.value = ''
  const p = preferences.value
  if (![p.budgetMax, p.taste, p.maxDistance, p.minRating, p.mealPeriod, p.dietaryTag, p.excludedAllergen].some(Boolean) && !p.openNow) {
    formError.value = '先选至少一项偏好，我们才能认真帮你挑。'; return
  }
  if ((p.budgetMax && (+p.budgetMax < 1 || +p.budgetMax > 1000)) || (p.maxDistance && (+p.maxDistance < 1 || +p.maxDistance > 20000))) {
    formError.value = '预算或距离不在有效范围内，请检查后重试。'; return
  }
  loading.value = true; recommendations.value = null
  const body: Record<string, unknown> = { limit: 5, openNow: p.openNow }
  if (p.budgetMax) body.budgetMax = Number(p.budgetMax)
  if (p.maxDistance) body.maxDistance = Number(p.maxDistance)
  if (p.minRating) body.minRating = Number(p.minRating)
  for (const key of ['taste', 'mealPeriod', 'dietaryTag', 'excludedAllergen'] as const) if (p[key]) body[key] = p[key]
  try { recommendations.value = await getRecommendations(body); setTimeout(() => document.getElementById('recommend-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 30) }
  catch (e) { error.value = (e as Error).message }
  finally { loading.value = false }
}

async function loadPage() {
  error.value = ''
  if (page.value === 'home') {
    loading.value = true
    try { featured.value = (await getItems(new URLSearchParams({ size: '6', sort: 'ratingDesc' }))).content }
    catch (e) { error.value = (e as Error).message }
    finally { loading.value = false }
  } else if (page.value === 'explore') loadList()
  else if (page.value === 'detail') loadDetail()
}

onMounted(async () => {
  try { dataInfo.value = await getDataInfo() } catch { /* page-level requests show the retry state */ }
})
watch(() => route.fullPath, () => { activeRequest++; loadPage() }, { immediate: true })
</script>

<template>
  <div class="app-shell">
    <header class="site-header"><div class="header-inner">
      <RouterLink to="/" class="brand"><span class="brand-mark">✳</span><span>食刻<span class="brand-accent">校园</span><small>Campus Bites</small></span></RouterLink>
      <nav class="desktop-nav" aria-label="主导航"><RouterLink to="/">首页</RouterLink><RouterLink to="/explore">发现美食</RouterLink><RouterLink to="/recommend">智能推荐</RouterLink><RouterLink to="/about-data">数据说明</RouterLink></nav>
      <RouterLink to="/recommend" class="header-cta">今天吃什么 <span>↗</span></RouterLink>
    </div></header>

    <main>
      <template v-if="page === 'home'">
        <section class="hero"><div class="hero-inner"><div class="hero-copy"><span class="eyebrow">✦ 你的校园美食灵感库</span><h1>好好吃饭，<br><em>从这一刻开始。</em></h1><p>预算、口味、距离都帮你考虑好了。下一顿好吃的，就在校园里。</p><div class="hero-search"><span>⌕</span><input v-model="filters.keyword" aria-label="搜索餐厅或菜品" placeholder="搜一搜，今天想吃什么？" @keyup.enter="submitSearch" /><button @click="submitSearch">搜索美食 →</button></div><div class="hero-note">🔥 热门搜索：<button @click="setShortcut('taste', '香辣')">香辣</button><button @click="setShortcut('maxPrice', '20')">20元以内</button><button @click="setShortcut('mealPeriod', '晚餐')">晚餐</button></div></div><div class="hero-visual" aria-hidden="true"><span class="hero-orbit orbit-one"></span><span class="hero-orbit orbit-two"></span><div class="hero-main-food">🍜</div><span class="floating-food float-one">🥟</span><span class="floating-food float-two">🥗</span><span class="floating-food float-three">🍓</span><div class="hero-sticker">今日份<br><strong>好心情</strong> ✨</div></div></div></section>
        <section class="quick-section container"><div class="section-heading"><div><span class="section-kicker">EAT BY MOOD</span><h2>想吃什么，从这里开始</h2></div><RouterLink to="/explore" class="text-link">查看全部美食 ↗</RouterLink></div><div class="quick-grid"><button @click="setShortcut('mealPeriod', '早餐')"><span class="quick-icon tone-yellow">🥯</span><strong>元气早餐</strong><small>开启好一天</small></button><button @click="setShortcut('taste', '香辣')"><span class="quick-icon tone-red">🌶️</span><strong>无辣不欢</strong><small>今天来点刺激</small></button><button @click="setShortcut('taste', '清淡')"><span class="quick-icon tone-green">🥬</span><strong>清爽轻食</strong><small>吃得舒服一点</small></button><button @click="setShortcut('maxPrice', '20')"><span class="quick-icon tone-purple">💰</span><strong>20元吃好</strong><small>钱包也能开心</small></button><button @click="setShortcut('openNow', true)"><span class="quick-icon tone-blue">🕒</span><strong>正在营业</strong><small>现在就出发</small></button></div></section>
        <section class="container featured-section"><div class="section-heading"><div><span class="section-kicker">PICKS FOR YOU</span><h2>校园人气美食 <span class="heading-star">✳</span></h2><p>从模拟餐饮中挑选评分较高的几家，先逛逛再决定。</p></div><RouterLink to="/explore" class="text-link">探索更多 ↗</RouterLink></div><div v-if="loading" class="skeleton-grid"><div v-for="n in 6" :key="n" class="skeleton-card"></div></div><div v-else-if="error" class="state-box"><span>😕</span><h3>暂时没连上美食数据库</h3><p>{{ error }}</p><button @click="loadPage">重新加载</button></div><div v-else class="food-grid"><FoodCard v-for="item in featured" :key="item.id" :item="item" /></div></section>
        <section class="recommend-banner container"><div><span>✦ 不知道吃什么？</span><h2>告诉我们你的口味<br>剩下的交给食刻校园</h2><p>按预算、距离和忌口，给你一份有理由的推荐清单。</p><RouterLink to="/recommend">开启智能推荐 <span>→</span></RouterLink></div><div class="banner-art" aria-hidden="true">🍱</div></section>
      </template>

      <template v-else-if="page === 'explore'"><section class="page-top container"><span class="section-kicker">EXPLORE THE CAMPUS</span><h1>发现校园好味道<span class="heading-star">✳</span></h1><p>随心搜索，筛出刚刚好的那一餐。</p></section><section class="container explore-layout"><aside class="filter-panel"><div class="filter-title"><h2>筛选条件</h2><button @click="resetFilters">重置全部</button></div><label>搜索美食<div class="input-icon"><span>⌕</span><input v-model="filters.keyword" placeholder="名称、菜系或招牌菜" @keyup.enter="submitSearch" /></div></label><label>人均预算<div class="input-suffix"><input v-model="filters.maxPrice" type="number" min="1" max="1000" placeholder="不限" /> <span>元以内</span></div></label><label>喜欢的口味<select v-model="filters.taste"><option value="">全部口味</option><option v-for="v in tasteOptions" :key="v">{{ v }}</option></select></label><label>最远距离<select v-model="filters.maxDistance"><option value="">不限距离</option><option value="300">300米内</option><option value="500">500米内</option><option value="1000">1公里内</option><option value="2000">2公里内</option></select></label><button class="more-filter" @click="showMoreFilters = !showMoreFilters">{{ showMoreFilters ? '收起更多' : '更多筛选' }} <span>{{ showMoreFilters ? '⌃' : '⌄' }}</span></button><div v-if="showMoreFilters" class="extra-filters"><label>最低评分<select v-model="filters.minRating"><option value="">不限评分</option><option value="4">4.0分起</option><option value="4.5">4.5分起</option><option value="4.8">4.8分起</option></select></label><label>用餐时段<select v-model="filters.mealPeriod"><option value="">全部时段</option><option v-for="v in mealOptions" :key="v">{{ v }}</option></select></label><label>避开过敏原<select v-model="filters.excludedAllergen"><option value="">不筛选</option><option v-for="v in allergenOptions" :key="v">{{ v }}</option></select></label></div><label class="check-row"><input v-model="filters.openNow" type="checkbox" /> 只看当前模拟营业</label><button class="primary wide" @click="submitSearch">查看筛选结果 →</button></aside><div class="explore-content"><div class="results-toolbar"><div><strong>{{ list?.total ?? 0 }}</strong> 个好去处 <span>等你发现</span></div><select v-model="filters.sort" aria-label="排序方式" @change="submitSearch"><option value="recommended">综合推荐</option><option value="ratingDesc">评分最高</option><option value="priceAsc">价格最低</option><option value="distanceAsc">距离最近</option></select></div><div v-if="loading" class="skeleton-grid"><div v-for="n in 6" :key="n" class="skeleton-card"></div></div><div v-else-if="error" class="state-box"><span>😕</span><h3>暂时无法加载</h3><p>{{ error }}</p><button @click="loadList">重新加载</button></div><div v-else-if="list && !list.content.length" class="state-box"><span>🔎</span><h3>没有找到合适的美食</h3><p>试试放宽预算或距离，或调整口味条件。</p><button @click="resetFilters">清除筛选条件</button></div><template v-else><div class="food-grid"><FoodCard v-for="item in list?.content" :key="item.id" :item="item" /></div><div v-if="list && list.totalPages > 1" class="pagination"><button :disabled="currentPage === 0" @click="changePage(currentPage - 1)">← 上一页</button><span>第 {{ currentPage + 1 }} / {{ list.totalPages }} 页</span><button :disabled="currentPage >= list.totalPages - 1" @click="changePage(currentPage + 1)">下一页 →</button></div></template></div></section></template>

      <template v-else-if="page === 'recommend'"><section class="recommend-hero"><div class="container"><span class="section-kicker">YOUR NEXT BITE</span><h1>今天吃什么？<br><em>交给我们来想。</em></h1><p>选几个你在意的条件，马上收获一份专属于你的校园美食清单。</p><span class="recommend-hero-food" aria-hidden="true">🍳</span></div></section><section class="container recommend-layout"><div class="preference-panel"><div class="panel-header"><span class="panel-step">01 / 告诉我们你的偏好</span><button @click="store.resetPreferences(); recommendations = null; formError = ''">清空条件 ↗</button></div><div class="form-grid"><label>这顿预算 <span>人均价格上限</span><div class="input-suffix"><input v-model="preferences.budgetMax" type="number" min="1" max="1000" placeholder="例如 25" /><span>元</span></div></label><label>步行距离 <span>离得近一点</span><select v-model="preferences.maxDistance"><option value="">不限距离</option><option value="300">300米内</option><option value="500">500米内</option><option value="1000">1公里内</option><option value="2000">2公里内</option></select></label><label>口味偏好 <span>你今天想吃的</span><select v-model="preferences.taste"><option value="">不限口味</option><option v-for="v in tasteOptions" :key="v">{{ v }}</option></select></label><label>用餐时间 <span>什么时候吃</span><select v-model="preferences.mealPeriod"><option value="">不限时段</option><option v-for="v in mealOptions" :key="v">{{ v }}</option></select></label><label>最低评分 <span>参考模拟评分</span><select v-model="preferences.minRating"><option value="">不限评分</option><option value="4">4.0分起</option><option value="4.5">4.5分起</option><option value="4.8">4.8分起</option></select></label><label>饮食偏好 <span>更贴合你的选择</span><select v-model="preferences.dietaryTag"><option value="">没有特殊要求</option><option value="素食">素食</option><option value="低脂">低脂</option><option value="清真">清真</option></select></label><label>避开过敏原 <span>谨慎选择</span><select v-model="preferences.excludedAllergen"><option value="">不筛选</option><option v-for="v in allergenOptions" :key="v">{{ v }}</option></select></label><label class="check-row checkbox-pref"><input v-model="preferences.openNow" type="checkbox" /><span><strong>只看当前模拟营业</strong><small>以模拟营业时间为准</small></span></label></div><p class="allergen-note">⚠️ 过敏原信息为模拟数据，实际用餐请现场确认。</p><p v-if="formError" class="form-error" role="alert">{{ formError }}</p><button class="primary recommend-submit" :disabled="loading" @click="submitRecommendation">{{ loading ? '正在挑选...' : '为我推荐好吃的 ✨' }}</button></div><div class="recommend-side"><span>🍽️</span><h3>一份懂你的<br>校园美食清单</h3><p>先排除不符合预算和忌口的选择，再结合口味、距离、评分和用餐时段排序。</p><div><span>01</span> 真实匹配的推荐理由</div><div><span>02</span> 一次给出 3–5 个选项</div><div><span>03</span> 条件不合适也会告诉你</div></div></section><section v-if="recommendations || error" id="recommend-results" class="container results-section"><div class="section-heading"><div><span class="section-kicker">MATCHED FOR YOU</span><h2>专属推荐清单</h2><p>每一条理由都来自模拟数据里的实际字段。</p></div></div><div v-if="error" class="state-box"><span>😕</span><h3>推荐暂时失败</h3><p>{{ error }}</p><button @click="submitRecommendation">再试一次</button></div><div v-else-if="recommendations && !recommendations.items.length" class="state-box"><span>🍽️</span><h3>暂时没有完全符合条件的选择</h3><p>{{ recommendations.suggestions.join(' · ') }}</p><button @click="scrollToPreferences">调整筛选条件</button></div><div v-else class="food-grid"><FoodCard v-for="(rec, index) in recommendations?.items" :key="rec.item.id" :item="rec.item" :rank="index + 1" :reasons="rec.reasons" /></div></section></template>

      <template v-else-if="page === 'detail'"><div class="container detail-wrap"><RouterLink to="/explore" class="back-link">← 返回发现美食</RouterLink><div v-if="loading" class="state-box">正在准备美食详情…</div><div v-else-if="error" class="state-box"><span>😕</span><h3>找不到这份美食</h3><p>{{ error }}</p><RouterLink to="/explore">返回美食列表 →</RouterLink></div><template v-else-if="detail"><div class="detail-grid"><div class="detail-art food-art" :class="`tone-${detail.visualTone || 'orange'}`"><span>{{ detail.imageEmoji || '🍽️' }}</span><small>{{ detail.type }}</small></div><div class="detail-main"><span class="section-kicker">CAMPUS FOOD GUIDE</span><h1>{{ detail.name }}</h1><p class="detail-description">{{ detail.description || '暂无详细介绍。' }}</p><div class="detail-metrics"><div><strong class="rating">★ {{ detail.rating ?? '—' }}</strong><span>模拟评分</span></div><div><strong>¥{{ detail.averagePrice ?? '—' }}</strong><span>人均价格</span></div><div><strong>{{ detail.distanceMeters == null ? '—' : `${detail.distanceMeters}m` }}</strong><span>参考距离</span></div></div><div class="detail-status"><span class="status" :class="detail.openNow ? 'open' : 'closed'">{{ detail.openNow ? '模拟营业中' : '模拟未营业' }}</span><span>营业时间 {{ detail.businessHours?.replaceAll('|', ' / ') || '未提供' }}</span></div><RouterLink to="/recommend" class="primary detail-cta">按我的偏好找美食 →</RouterLink></div></div><div class="detail-below"><div class="detail-info-card"><h2>这家有什么好吃的？</h2><div class="detail-row"><span>📍 所在位置</span><strong>{{ detail.location }} · {{ detail.campus }}</strong></div><div class="detail-row"><span>🍽️ 招牌推荐</span><strong>{{ detail.signatureDish || '暂无资料' }}</strong></div><div class="detail-row"><span>🥢 菜系风格</span><strong>{{ detail.cuisine || '暂无资料' }}</strong></div><div class="detail-row"><span>🌶️ 口味标签</span><strong>{{ detail.tasteTags.join('、') || '暂无资料' }}</strong></div><div class="detail-row"><span>🕒 适合时段</span><strong>{{ detail.mealPeriods.join('、') || '暂无资料' }}</strong></div><div class="detail-row"><span>🌿 饮食标签</span><strong>{{ detail.dietaryTags.join('、') || '暂无资料' }}</strong></div><div class="detail-row"><span>⚠️ 模拟过敏原</span><strong>{{ detail.allergens.join('、') || '信息缺失，请现场确认' }}</strong></div></div><div class="detail-note"><span>✳</span><h3>关于这些数据</h3><p>本条为 {{ detail.dataVersion }} 课程模拟数据，更新于 {{ detail.dataUpdatedAt }}。价格、距离、评分、营业时间和过敏原均不代表真实信息。</p><RouterLink to="/about-data">了解数据说明 →</RouterLink></div></div></template></div></template>

      <template v-else-if="page === 'about-data'"><section class="container about-page"><span class="section-kicker">ABOUT THE DATA</span><h1>关于这份校园美食地图</h1><p class="about-lead">这个项目用于展示如何把预算、口味、距离和忌口变成可解释的美食推荐。</p><div class="about-grid"><div><span>01</span><h2>全部是模拟数据</h2><p>餐厅、窗口、菜品、名称、位置、价格、评分、距离、营业时间和标签均为课程模拟内容，不代表实际商家。</p></div><div><span>02</span><h2>推荐有据可查</h2><p>推荐结果先遵守预算与忌口条件，再结合口味、距离、评分和用餐时段排序。每条理由来自其数据字段。</p></div><div><span>03</span><h2>用餐前请核实</h2><p>模拟营业信息与过敏原信息不能作为真实决策依据。实际用餐请向商家核对价格、营业情况和食材。</p></div></div><div class="data-stamp"><span>数据版本 <strong>{{ dataInfo?.version || 'MOCK_V1' }}</strong></span><span>更新日期 <strong>{{ dataInfo?.updatedAt || '2026-10-09' }}</strong></span><span>模拟记录 <strong>{{ dataInfo?.total ?? '—' }} 条</strong></span></div></section></template>
      <template v-else><section class="container state-box not-found"><span>🧭</span><h1>走错路啦</h1><p>这个页面还没有上菜，去发现一些好吃的吧。</p><RouterLink to="/">回到首页 →</RouterLink></section></template>
    </main>

    <footer class="site-footer"><div class="container footer-inner"><div class="footer-brand"><strong>✳ 食刻校园</strong><p>每一餐，都值得认真对待。</p></div><div class="footer-links"><RouterLink to="/explore">发现美食</RouterLink><RouterLink to="/recommend">智能推荐</RouterLink><RouterLink to="/about-data">数据说明</RouterLink></div><p class="footer-disclaimer">课程模拟数据 · 仅供功能演示<br>不代表真实价格、评分或营业信息</p></div></footer>
    <nav class="mobile-nav" aria-label="移动端导航"><RouterLink to="/"><span>⌂</span>首页</RouterLink><RouterLink to="/explore"><span>⌕</span>发现</RouterLink><RouterLink to="/recommend"><span>✳</span>推荐</RouterLink><RouterLink to="/about-data"><span>ⓘ</span>说明</RouterLink></nav>
  </div>
</template>
