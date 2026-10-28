'use client'

import Link from 'next/link'
import { useTranslations } from 'next-intl'
import type { Place } from '@/lib/supabase/types'
import { Badge } from '@/ui/Badge'

interface PlaceCardProps {
  place: Place
}

export function PlaceCard({ place }: PlaceCardProps) {
  const t = useTranslations('place')

  const showStatus = place.status === 'needs_review' || place.status === 'pending'

  return (
    <div
      className="rounded-lg p-4 flex flex-col gap-3 shadow-sm"
      style={{
        backgroundColor: 'var(--color-surface)',
        border: '1px solid color-mix(in srgb, var(--color-navy) 12%, transparent)',
      }}
    >
      <div className="flex items-start justify-between gap-2">
        <Link
          href={`/place/${place.id}`}
          className="text-lg font-semibold hover:underline"
          style={{ color: 'var(--color-navy)' }}
        >
          {place.name}
        </Link>
        {showStatus && (
          <Badge variant="status" status={place.status as 'pending' | 'needs_review'}>
            {place.status === 'needs_review' ? t('status.review') : t('status.pending')}
          </Badge>
        )}
      </div>

      {place.categories.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {place.categories.map((cat) => (
            <Badge key={cat} variant="category">
              {cat}
            </Badge>
          ))}
        </div>
      )}

      <p className="text-sm" style={{ color: 'var(--color-navy)', opacity: 0.7 }}>
        {place.barrio}
        {place.address ? ` · ${place.address}` : ''}
      </p>

      <div className="flex flex-wrap gap-2 mt-auto">
        {place.phone && (
          <a
            href={`tel:${place.phone}`}
            className="text-sm font-medium"
            style={{ color: 'var(--color-primary)' }}
          >
            {t('phone')}: {place.phone}
          </a>
        )}
        {place.whatsapp && (
          <a
            href={`https://wa.me/${place.whatsapp}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center px-3 py-1.5 rounded-md text-sm font-medium"
            style={{
              backgroundColor: 'transparent',
              color: 'var(--color-primary)',
              border: '1.5px solid var(--color-primary)',
            }}
          >
            WhatsApp
          </a>
        )}
        {place.maps_url && (
          <a
            href={place.maps_url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center px-3 py-1.5 rounded-md text-sm font-medium"
            style={{ color: 'var(--color-navy)', opacity: 0.7 }}
          >
            Maps
          </a>
        )}
      </div>
    </div>
  )
}
