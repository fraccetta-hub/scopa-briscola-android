// ASUFinder OSM Import — Main Orchestrator
// Usage: npx tsx scripts/osm-import/index.ts
// Dry-run: DRY_RUN=true npx tsx scripts/osm-import/index.ts

import { fetchOverpass } from './osm-client'
import { mapCategory } from './category-mapper'
import { isDuplicate } from './deduplicator'
import { createAdminClient } from '../../src/lib/supabase/admin'

const BBOX = { south: -25.7, west: -57.8, north: -25.1, east: -57.3 }
const BATCH_SIZE = 100
const DRY_RUN = process.env.DRY_RUN === 'true'

interface PlaceRow {
  name: string
  categories: string[]
  tags: string[]
  barrio: string
  address: string | null
  lat: number | null
  lng: number | null
  phone: string | null
  whatsapp: string | null
  website: string | null
  maps_url: string | null
  hours: string | null
  source: 'osm'
  status: 'live'
  is_featured: boolean
}

/**
 * Extract the OSM tag key that represents the primary type of the element.
 * Checks amenity, shop, and tourism in order.
 */
function getPrimaryTag(tags: Record<string, string>): string | undefined {
  return tags['amenity'] ?? tags['shop'] ?? tags['tourism']
}

/**
 * Extract barrio from OSM address tags. Falls back to 'Gran Asunción'.
 */
function extractBarrio(tags: Record<string, string>): string {
  return (
    tags['addr:suburb'] ??
    tags['addr:neighbourhood'] ??
    tags['addr:city'] ??
    'Gran Asunción'
  )
}

/**
 * Build a human-readable address string from OSM address tags.
 */
function buildAddress(tags: Record<string, string>): string | null {
  const parts = [
    tags['addr:street'],
    tags['addr:housenumber'],
    tags['addr:city'],
  ].filter(Boolean)
  return parts.length > 0 ? parts.join(' ') : null
}

async function main(): Promise<void> {
  console.log(`[osm-import] Starting${DRY_RUN ? ' (DRY RUN)' : ''}...`)

  const elements = await fetchOverpass(BBOX)
  console.log(`[osm-import] Fetched ${elements.length} OSM elements`)

  // Narrow type: supabase is only used after the DRY_RUN early-return below
  const supabase = DRY_RUN ? (null as ReturnType<typeof createAdminClient> | null) : createAdminClient()

  const rows: PlaceRow[] = []
  let skippedNoName = 0
  let skippedDuplicate = 0

  for (const element of elements) {
    const tags = element.tags ?? {}
    const name = tags['name']

    if (!name) {
      skippedNoName++
      continue
    }

    const primaryTag = getPrimaryTag(tags)
    const category = mapCategory(primaryTag ?? '')
    const barrio = extractBarrio(tags)

    // Skip duplicates (uses Supabase — skip check in dry-run to avoid DB calls)
    if (!DRY_RUN) {
      const duplicate = await isDuplicate(name, barrio)
      if (duplicate) {
        skippedDuplicate++
        continue
      }
    }

    // Resolve lat/lng: nodes have top-level lat/lon, ways have center
    const lat = element.lat ?? element.center?.lat ?? null
    const lng = element.lon ?? element.center?.lon ?? null

    rows.push({
      name,
      categories: [category],
      tags: primaryTag ? [primaryTag] : [],
      barrio,
      address: buildAddress(tags),
      lat,
      lng,
      phone: tags['phone'] ?? null,
      whatsapp: tags['contact:whatsapp'] ?? null,
      website: tags['website'] ?? tags['contact:website'] ?? null,
      maps_url: null,
      hours: tags['opening_hours'] ?? null,
      source: 'osm',
      status: 'live',
      is_featured: false,
    })
  }

  console.log(
    `[osm-import] Prepared ${rows.length} rows (skipped: ${skippedNoName} no-name, ${skippedDuplicate} duplicates)`,
  )

  if (DRY_RUN) {
    console.log(`[osm-import] DRY RUN — would insert ${rows.length} places`)
    if (rows.length > 0) {
      console.log('[osm-import] Sample row:', JSON.stringify(rows[0], null, 2))
    }
    return
  }

  // Batch insert in groups of BATCH_SIZE
  // On error: log and continue rather than exit — partial runs can be safely re-run
  // because dedup prevents double-inserts of already-inserted rows.
  let insertedCount = 0
  let errorCount = 0
  for (let i = 0; i < rows.length; i += BATCH_SIZE) {
    const batch = rows.slice(i, i + BATCH_SIZE)
    // TS5.9 + supabase incompatibility: cast to any
    const { error } = await (supabase as any).from('places').insert(batch as any)
    if (error) {
      console.error(`[osm-import] Insert error at batch ${Math.floor(i / BATCH_SIZE) + 1}:`, error.message)
      errorCount += batch.length
      continue
    }
    insertedCount += batch.length
    console.log(`[osm-import] Inserted batch ${Math.floor(i / BATCH_SIZE) + 1}: ${insertedCount}/${rows.length}`)
  }

  console.log(
    `[osm-import] Done: inserted ${insertedCount}, skipped ${skippedDuplicate} duplicates, ${errorCount} errors`,
  )
}

main().catch((err) => {
  console.error('[osm-import] Fatal error:', err)
  process.exit(1)
})
