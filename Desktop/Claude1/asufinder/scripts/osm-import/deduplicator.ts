// ASUFinder OSM Import — Duplicate Checker

import { checkDuplicates } from '../../src/lib/supabase/queries/places'

/**
 * Returns true if a place with a similar name already exists in the same barrio.
 * Delegates similarity logic to checkDuplicates which uses the configured threshold.
 */
export async function isDuplicate(name: string, barrio: string): Promise<boolean> {
  const matches = await checkDuplicates(name, barrio)
  return matches.length > 0
}
