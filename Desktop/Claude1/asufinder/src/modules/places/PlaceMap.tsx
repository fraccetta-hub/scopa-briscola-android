'use client'

import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import icon from 'leaflet/dist/images/marker-icon.png'
import iconShadow from 'leaflet/dist/images/marker-shadow.png'

// Fix Leaflet default marker icons in Next.js
const DefaultIcon = L.icon({
  iconUrl: (icon as unknown as { src: string }).src ?? (icon as unknown as string),
  shadowUrl: (iconShadow as unknown as { src: string }).src ?? (iconShadow as unknown as string),
  iconSize: [25, 41],
  iconAnchor: [12, 41],
})
L.Marker.mergeOptions({ icon: DefaultIcon })

interface PlaceMapProps {
  lat: number
  lng: number
  name: string
}

export function PlaceMap({ lat, lng, name }: PlaceMapProps) {
  return (
    <div data-testid="place-map" style={{ height: '300px', width: '100%', borderRadius: '0.5rem', overflow: 'hidden' }}>
      <MapContainer
        center={[lat, lng]}
        zoom={15}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom={false}
      >
        <TileLayer
          attribution="© OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={[lat, lng]}>
          <Popup>{name}</Popup>
        </Marker>
      </MapContainer>
    </div>
  )
}
