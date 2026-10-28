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

  // Client-side abort timeout (90s) in case the connection stalls before the server timeout fires
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 90_000)

  let response: Response
  try {
    response = await fetch(OVERPASS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
      signal: controller.signal,
    })
  } finally {
    clearTimeout(timeout)
  }

  if (!response.ok) {
    throw new Error(`Overpass API error: ${response.status} ${response.statusText}`)
  }

  const json = (await response.json()) as OverpassResponse
  return json.elements ?? []
}
