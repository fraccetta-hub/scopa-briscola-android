import { describe, it, expect } from 'vitest'
import { mapCategory } from '../category-mapper'

describe('mapCategory', () => {
  it('maps "pharmacy" to "farmacia"', () => {
    expect(mapCategory('pharmacy')).toBe('farmacia')
  })

  it('maps "supermarket" to "almacen"', () => {
    expect(mapCategory('supermarket')).toBe('almacen')
  })

  it('maps unknown tag to "otros"', () => {
    expect(mapCategory('unknown_tag')).toBe('otros')
  })

  it('maps "restaurant" to "restaurante"', () => {
    expect(mapCategory('restaurant')).toBe('restaurante')
  })

  it('maps empty string to "otros"', () => {
    expect(mapCategory('')).toBe('otros')
  })

  it('maps "fuel" to "nafta"', () => {
    expect(mapCategory('fuel')).toBe('nafta')
  })

  it('maps "convenience" to "almacen"', () => {
    expect(mapCategory('convenience')).toBe('almacen')
  })
})
