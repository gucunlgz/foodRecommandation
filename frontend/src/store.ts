import { defineStore } from 'pinia'
import type { Filters, Preferences } from './types'

const initialFilters = (): Filters => ({ keyword: '', maxPrice: '', taste: '', maxDistance: '', minRating: '', openNow: false, mealPeriod: '', excludedAllergen: '', sort: 'recommended' })
const initialPreferences = (): Preferences => ({ budgetMax: '', taste: '', maxDistance: '', minRating: '', openNow: false, mealPeriod: '', dietaryTag: '', excludedAllergen: '' })

export const useFoodStore = defineStore('food', {
  state: () => ({ filters: initialFilters(), preferences: initialPreferences() }),
  actions: { resetFilters() { this.filters = initialFilters() }, resetPreferences() { this.preferences = initialPreferences() } },
})
