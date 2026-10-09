import type { DataInfo, FoodItem, ListResult, RecommendationResult } from './types'

interface Envelope<T> { success: boolean; data?: T; message?: string }

function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('服务返回的餐饮数据格式异常，请稍后重试。')
  }
  return value as Record<string, unknown>
}

function stringValue(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function optionalString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value : null
}

function numberValue(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0) : []
}

function foodItem(value: unknown): FoodItem {
  const item = record(value)
  const id = numberValue(item.id)
  if (id === null) throw new Error('服务返回的餐饮数据缺少编号，请稍后重试。')
  return {
    id,
    name: stringValue(item.name, '未命名餐饮'),
    type: stringValue(item.type, '餐饮'),
    campus: stringValue(item.campus, '校区未提供'),
    location: stringValue(item.location, '位置未提供'),
    cuisine: optionalString(item.cuisine),
    averagePrice: numberValue(item.averagePrice),
    distanceMeters: numberValue(item.distanceMeters),
    rating: numberValue(item.rating),
    description: optionalString(item.description),
    signatureDish: optionalString(item.signatureDish),
    imageEmoji: optionalString(item.imageEmoji),
    visualTone: optionalString(item.visualTone),
    tasteTags: stringList(item.tasteTags),
    mealPeriods: stringList(item.mealPeriods),
    dietaryTags: stringList(item.dietaryTags),
    allergens: stringList(item.allergens),
    businessHours: optionalString(item.businessHours),
    openNow: item.openNow === true,
    dataVersion: stringValue(item.dataVersion, 'MOCK_V1'),
    dataUpdatedAt: stringValue(item.dataUpdatedAt, '未知'),
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api/v1${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...init?.headers } })
  } catch {
    throw new Error('连接服务失败，请确认后端已启动后重试。')
  }
  let payload: Envelope<T>
  try { payload = await response.json() as Envelope<T> }
  catch { throw new Error('服务响应异常，请稍后重试。') }
  if (!response.ok || !payload.success || payload.data === undefined) throw new Error(payload.message || '请求失败，请重试。')
  return payload.data
}

export async function getItems(params: URLSearchParams): Promise<ListResult> {
  const data = record(await request<unknown>(`/dining-items?${params.toString()}`))
  if (!Array.isArray(data.content)) throw new Error('服务返回的列表数据不完整，请稍后重试。')
  return { ...data, content: data.content.map(foodItem) } as ListResult
}
export async function getItem(id: string): Promise<FoodItem> {
  return foodItem(await request<unknown>(`/dining-items/${encodeURIComponent(id)}`))
}
export function getDataInfo() { return request<DataInfo>('/meta/data-info') }
export async function getRecommendations(body: Record<string, unknown>): Promise<RecommendationResult> {
  const data = record(await request<unknown>('/recommendations', { method: 'POST', body: JSON.stringify(body) }))
  if (!Array.isArray(data.items)) throw new Error('服务返回的推荐数据不完整，请稍后重试。')
  const items = data.items.map((value: unknown) => {
    const recommendation = record(value)
    return { ...recommendation, item: foodItem(recommendation.item), reasons: stringList(recommendation.reasons) }
  })
  return { ...data, items, suggestions: stringList(data.suggestions) } as RecommendationResult
}
