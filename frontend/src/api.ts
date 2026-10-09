import type { DataInfo, FoodItem, ListResult, RecommendationResult } from './types'

interface Envelope<T> { success: boolean; data?: T; message?: string }

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

export function getItems(params: URLSearchParams) { return request<ListResult>(`/dining-items?${params.toString()}`) }
export function getItem(id: string) { return request<FoodItem>(`/dining-items/${encodeURIComponent(id)}`) }
export function getDataInfo() { return request<DataInfo>('/meta/data-info') }
export function getRecommendations(body: Record<string, unknown>) {
  return request<RecommendationResult>('/recommendations', { method: 'POST', body: JSON.stringify(body) })
}
