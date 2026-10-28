// ASUFinder — Place queries
// All direct Supabase calls for the `places` table go here.

import { createClient } from '../server'
import { createAdminClient } from '../admin'
import type { Place } from '../types'
import type { Database } from '../types'
import { SETTINGS } from '@/config/settings'

type PlaceInsert = Database['public']['Tables']['places']['Insert']

export async function searchPlaces(
  query: string,
  category?: string,
  page = 0,
  limit = SETTINGS.pagination.searchResultsPerPage,
): Promise<Place[]> {
  const supabase = await createClient()
  const offset = page * limit

  let req = supabase
    .from('places')
    .select('*')
    .textSearch('name', query)

  if (category) {
    req = req.contains('categories', [category])
  }

  const { data, error } = await req.range(offset, offset + limit - 1)

  if (error) throw error
  const nameResults: Place[] = data ?? []

  // Also search by product tags on page 0 only (tag results don't paginate)
  let tagResults: Place[] = []
  if (page === 0 && query.trim()) {
    try {
      const { searchByTag } = await import('./products')
      tagResults = await searchByTag(query, 20)
    } catch {
      // tag search is best-effort
    }
  }

  // Deduplicate by place id
  const seen = new Set<string>(nameResults.map((p) => p.id))
  const merged = [...nameResults]
  for (const place of tagResults) {
    if (!seen.has(place.id)) {
      seen.add(place.id)
      merged.push(place)
    }
  }

  return merged
}

export async function getPlace(id: string): Promise<Place | null> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('places')
    .select('*')
    .eq('id', id)
    .single()

  if (error) {
    if (error.code === 'PGRST116') return null
    throw error
  }
  return data
}

export async function insertPlace(data: PlaceInsert): Promise<Place> {
  const supabase = await createClient()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: inserted, error } = await supabase
    .from('places')
    .insert(data as any)
    .select()
    .single()

  if (error) throw error
  return inserted
}

export async function updatePlace(id: string, patch: Partial<Place>): Promise<Place> {
  const supabase = await createClient()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, error } = await (supabase as any)
    .from('places')
    .update(patch)
    .eq('id', id)
    .select()
    .single() as { data: Place | null; error: { message: string } | null }

  if (error) throw new Error(error.message)
  if (!data) throw new Error('No data returned from update')
  return data
}

/**
 * Client-side duplicate check: fetch places in the same barrio and filter by
 * name similarity locally.
 *
 * TODO: Replace with pg_trgm raw SQL query when Supabase project is live:
 *   SELECT * FROM places
 *   WHERE similarity(name, $1) > $threshold AND barrio = $2
 * This approximation uses simple string inclusion as a stand-in.
 */
export async function getPlaceAdmin(id: string): Promise<Place | null> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('places')
    .select('*')
    .eq('id', id)
    .single()

  if (error) {
    if (error.code === 'PGRST116') return null
    throw error
  }
  return data
}

export async function updatePlaceAdmin(id: string, patch: Partial<Place>): Promise<Place> {
  const supabase = createAdminClient()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, error } = await (supabase as any)
    .from('places')
    .update(patch)
    .eq('id', id)
    .select()
    .single() as { data: Place | null; error: { message: string } | null }

  if (error) throw new Error(error.message)
  if (!data) throw new Error('No data returned from update')
  return data
}

export async function checkDuplicates(
  name: string,
  barrio: string,
  _lat?: number,
  _lng?: number,
): Promise<Place[]> {
  const supabase = await createClient()
  const threshold = SETTINGS.duplicates.nameSimilarityThreshold

  const { data, error } = await supabase
    .from('places')
    .select('*')
    .eq('barrio', barrio)

  if (error) throw error
  if (!data || data.length === 0) return []

  const nameLower = name.toLowerCase()

  return (data as Place[]).filter((place) => {
    const placeName = place.name.toLowerCase()
    // Simple similarity approximation: one includes the other, or shared
    // prefix length / max length exceeds the threshold.
    if (placeName.includes(nameLower) || nameLower.includes(placeName)) return true
    const maxLen = Math.max(nameLower.length, placeName.length)
    if (maxLen === 0) return false
    let common = 0
    for (let i = 0; i < Math.min(nameLower.length, placeName.length); i++) {
      if (nameLower[i] === placeName[i]) common++
      else break
    }
    return common / maxLen >= threshold
  })
}
