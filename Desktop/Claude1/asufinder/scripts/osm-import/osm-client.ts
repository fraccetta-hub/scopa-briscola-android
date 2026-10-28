// ASUFinder OSM Import — Overpass API Client

export interface OsmElement {
  type: 'node' | 'way'
  id: number
  lat?: number
  lon?: number
  center?: { lat: number; lon: number }
  tags?: Record<string, string>
}

export interface OverpassResponse {
  elements: OsmElement[]
}

const OVERPASS_URL = 'https://overpass-api.de/api/interpreter'

/**
 * Fetch OSM elements from the Overpass API for the given bounding box.
 * Uses a POST request with the standard Gran Asunción query template.
 */
export async function fetchOverpass(bbox: {
  south: number
  west: number
  north: number
  east: number
}): Promise<OsmElement[]> {
  const { south, west, north, east } = bbox

  const query = `[out:json][timeout:60];
(
  node["amenity"](${south},${west},${north},${east});
  node["shop"](${south},${west},${north},${east});
  node["tourism"](${south},${west},${north},${east});
  way["amenity"](${south},${west},${north},${east});
  way["shop"](${south},${west},${north},${east});
);
out center;`

  const body = new URLSearchParams({ data: query })

  const response = await fetch(OVERPASS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })

  if (!response.ok) {
    throw new Error(`Overpass API error: ${response.status} ${response.statusText}`)
  }

  const json = (await response.json()) as OverpassResponse
  return json.elements ?? []
}
