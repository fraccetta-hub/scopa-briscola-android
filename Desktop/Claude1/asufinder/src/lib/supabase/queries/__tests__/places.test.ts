import { describe, it, expect, vi, beforeEach } from 'vitest'
import { searchPlaces, checkDuplicates } from '../places'

// ---------------------------------------------------------------------------
// Mock @/config/settings
// ---------------------------------------------------------------------------
vi.mock('@/config/settings', () => ({
  SETTINGS: {
    pagination: { searchResultsPerPage: 20, adminListPerPage: 50 },
    duplicates: { nameSimilarityThreshold: 0.6, barrioSimilarityThreshold: 0.8, radiusMeters: 100 },
  },
}))

// ---------------------------------------------------------------------------
// Build a chainable mock for the Supabase query builder
// ---------------------------------------------------------------------------
type MockBuilder = {
  select: ReturnType<typeof vi.fn>
  textSearch: ReturnType<typeof vi.fn>
  contains: ReturnType<typeof vi.fn>
  range: ReturnType<typeof vi.fn>
  eq: ReturnType<typeof vi.fn>
  // resolved value set per test
  _resolve: (v: unknown) => void
}

function makeBuilder(resolvedValue: unknown): MockBuilder {
  let resolveWith = resolvedValue
  const builder: MockBuilder = {
    select: vi.fn().mockReturnThis(),
    textSearch: vi.fn().mockReturnThis(),
    contains: vi.fn().mockReturnThis(),
    range: vi.fn().mockImplementation(() => Promise.resolve(resolveWith)),
    eq: vi.fn().mockImplementation(() => Promise.resolve(resolveWith)),
    _resolve: (v: unknown) => { resolveWith = v },
  }
  return builder
}

// Mock the Supabase browser client module
let mockBuilder: MockBuilder

vi.mock('../../server', () => ({
  createClient: vi.fn(async () => ({
    from: vi.fn(() => mockBuilder),
  })),
}))

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('searchPlaces', () => {
  beforeEach(() => {
    mockBuilder = makeBuilder({ data: [], error: null })
  })

  it('calls .textSearch() with the query string', async () => {
    await searchPlaces('zaini')
    expect(mockBuilder.textSearch).toHaveBeenCalledWith('name', 'zaini')
  })

  it('does NOT call .contains() when no category given', async () => {
    await searchPlaces('zaini')
    expect(mockBuilder.contains).not.toHaveBeenCalled()
  })

  it('calls .contains() with category when provided', async () => {
    await searchPlaces('zaini', 'supermercato')
    expect(mockBuilder.contains).toHaveBeenCalledWith('categories', ['supermercato'])
  })

  it('returns an empty array when data is null', async () => {
    mockBuilder._resolve({ data: null, error: null })
    const result = await searchPlaces('zaini')
    expect(result).toEqual([])
  })

  it('throws when Supabase returns an error', async () => {
    mockBuilder._resolve({ data: null, error: new Error('db error') })
    await expect(searchPlaces('zaini')).rejects.toThrow('db error')
  })
})

describe('checkDuplicates', () => {
  beforeEach(() => {
    mockBuilder = makeBuilder({ data: [], error: null })
  })

  it('returns empty array when no places in barrio', async () => {
    mockBuilder._resolve({ data: [], error: null })
    const result = await checkDuplicates('Pane & Salute', 'Palermo')
    expect(result).toEqual([])
  })

  it('returns empty array when data is null', async () => {
    mockBuilder._resolve({ data: null, error: null })
    const result = await checkDuplicates('Pane & Salute', 'Palermo')
    expect(result).toEqual([])
  })

  it('returns matching places when name is similar', async () => {
    const place = {
      id: '1', name: 'Pane e Salute', barrio: 'Palermo',
      categories: [], tags: [], address: null, lat: null, lng: null,
      phone: null, whatsapp: null, website: null, maps_url: null,
      hours: null, photo_url: null, status: 'live', source: 'user',
      is_featured: false, featured_until: null, added_by: null,
      created_at: '', updated_at: '',
    }
    mockBuilder._resolve({ data: [place], error: null })
    // "pane e salute" includes "pane" so the filter should pass it through
    // (our approximation: one includes the other)
    const result = await checkDuplicates('Pane', 'Palermo')
    expect(result.length).toBeGreaterThanOrEqual(1)
  })

  it('filters out non-matching places', async () => {
    const place = {
      id: '2', name: 'Bar Centrale', barrio: 'Palermo',
      categories: [], tags: [], address: null, lat: null, lng: null,
      phone: null, whatsapp: null, website: null, maps_url: null,
      hours: null, photo_url: null, status: 'live', source: 'user',
      is_featured: false, featured_until: null, added_by: null,
      created_at: '', updated_at: '',
    }
    mockBuilder._resolve({ data: [place], error: null })
    const result = await checkDuplicates('Zaini', 'Palermo')
    expect(result).toEqual([])
  })
})
