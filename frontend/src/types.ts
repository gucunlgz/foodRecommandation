export interface FoodItem {
  id: number
  name: string
  type: string
  campus: string
  location: string
  cuisine: string | null
  averagePrice: number | null
  distanceMeters: number | null
  rating: number | null
  description: string | null
  signatureDish: string | null
  imageEmoji: string | null
  visualTone: string | null
  tasteTags: string[]
  mealPeriods: string[]
  dietaryTags: string[]
  allergens: string[]
  businessHours: string | null
  openNow: boolean
  dataVersion: string
  dataUpdatedAt: string
}

export interface ListResult { content: FoodItem[]; total: number; page: number; size: number; totalPages: number }
export interface Recommendation { item: FoodItem; score: number; reasons: string[] }
export interface RecommendationResult { items: Recommendation[]; total: number; algorithmVersion: string; suggestions: string[] }
export interface DataInfo { isMock: boolean; version: string; updatedAt: string; total: number; notice: string; allergenNotice: string }
export interface Filters {
  keyword: string; maxPrice: string; taste: string; maxDistance: string; minRating: string;
  openNow: boolean; mealPeriod: string; excludedAllergen: string; sort: string
}
export interface Preferences {
  budgetMax: string; taste: string; maxDistance: string; minRating: string;
  openNow: boolean; mealPeriod: string; dietaryTag: string; excludedAllergen: string
}
