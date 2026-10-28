// ASUFinder OSM Import — Category Mapper
// Maps an OSM amenity/shop tag string to an ASUFinder CategorySlug.

import { CATEGORIES } from '../../src/config/categories'
import type { CategorySlug } from '../../src/config/categories'

/**
 * Map an OSM tag value (e.g. "pharmacy", "supermarket") to a CategorySlug.
 * Falls back to 'otros' when no category matches.
 */
export function mapCategory(osmTag: string): CategorySlug {
  for (const category of CATEGORIES) {
    if ((category.osm as readonly string[]).includes(osmTag)) {
      return category.slug as CategorySlug
    }
  }
  return 'otros'
}
