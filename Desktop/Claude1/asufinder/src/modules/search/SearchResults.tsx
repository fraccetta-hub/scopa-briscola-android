'use client'

import { useTranslations } from 'next-intl'
import type { Place } from '@/lib/supabase/types'
import { PlaceCard } from '@/modules/places/PlaceCard'

interface SearchResultsProps {
  places: Place[]
  loading?: boolean
}

function SkeletonCard() {
  return (
    <div
      className="rounded-lg p-4 flex flex-col gap-3"
      style={{
        backgroundColor: 'var(--color-surface)',
        border: '1px solid color-mix(in srgb, var(--color-navy) 12%, transparent)',
      }}
    >
      <div
        className="h-5 rounded animate-pulse"
        style={{ backgroundColor: 'color-mix(in srgb, var(--color-navy) 12%, transparent)', width: '60%' }}
      />
      <div
        className="h-4 rounded animate-pulse"
        style={{ backgroundColor: 'color-mix(in srgb, var(--color-navy) 8%, transparent)', width: '40%' }}
      />
      <div
        className="h-4 rounded animate-pulse"
        style={{ backgroundColor: 'color-mix(in srgb, var(--color-navy) 8%, transparent)', width: '80%' }}
      />
    </div>
  )
}

export function SearchResults({ places, loading = false }: SearchResultsProps) {
  const t = useTranslations('search')

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    )
  }

  if (places.length === 0) {
    return (
      <p className="text-center py-12" style={{ color: 'var(--color-navy)', opacity: 0.6 }}>
        {t('noResults')}
      </p>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {places.map((place) => (
        <PlaceCard key={place.id} place={place} />
      ))}
    </div>
  )
}
