// ASUFinder OSM Import — Duplicate Checker
// Uses admin client to bypass RLS (checkDuplicates from queries/places uses the public client
// which cannot read non-live places — admin client sees all rows for accurate dedup).

import { createAdminClient } from '../../src/lib/supabase/admin'

/**
 * Returns true if a place with a similar name already exists in the same barrio.
 * Similarity threshold: any match via ilike (pg_trgm used by searchPlaces for text search).
 */
export async function isDuplicate(name: string, barrio: string): Promise<boolean> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('places')
    .select('id')
    .eq('barrio', barrio)
    .ilike('name', `%${name}%`)
    .limit(1)

  if (error) {
    console.warn(`Dedup check failed for "${name}" in ${barrio}: ${error.message}`)
    return false // on error, allow insert rather than silently drop
  }
  return (data ?? []).length > 0
}
