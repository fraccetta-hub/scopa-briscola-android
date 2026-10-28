'use client'

import dynamic from 'next/dynamic'
import { useTranslations } from 'next-intl'
import type { Place } from '@/lib/supabase/types'
import { Badge } from '@/ui/Badge'

const PlaceMap = dynamic(() => import('./PlaceMap').then((m) => m.PlaceMap), { ssr: false })

interface PlaceDetailProps {
  place: Place
}

export function PlaceDetail({ place }: PlaceDetailProps) {
  const t = useTranslations('place')

  return (
    <article className="flex flex-col gap-6 max-w-2xl mx-auto px-4 py-8">
      <div className="flex items-start gap-3 flex-wrap">
        <h1
          className="text-2xl md:text-3xl font-bold flex-1"
          style={{ color: 'var(--color-navy)' }}
        >
          {place.name}
        </h1>
        <div className="flex flex-wrap gap-2">
          <Badge variant="status" status={place.status as 'live' | 'pending' | 'needs_review' | 'closed' | 'rejected'}>
            {t(`status.${place.status === 'needs_review' ? 'review' : place.status}`)}
          </Badge>
          {place.is_featured && (
            <Badge variant="featured">{t('featured')}</Badge>
          )}
        </div>
      </div>

      {place.categories.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {place.categories.map((cat) => (
            <Badge key={cat} variant="category">{cat}</Badge>
          ))}
        </div>
      )}

      <div className="flex flex-col gap-2">
        <p style={{ color: 'var(--color-navy)' }}>
          <span className="font-medium">{place.barrio}</span>
          {place.address && <span style={{ opacity: 0.7 }}> · {place.address}</span>}
        </p>

        {place.phone && (
          <p>
            <span className="font-medium text-sm" style={{ color: 'var(--color-navy)' }}>{t('phone')}: </span>
            <a href={`tel:${place.phone}`} style={{ color: 'var(--color-primary)' }}>
              {place.phone}
            </a>
          </p>
        )}

        {place.whatsapp && (
          <p>
            <span className="font-medium text-sm" style={{ color: 'var(--color-navy)' }}>{t('whatsapp')}: </span>
            <a
              href={`https://wa.me/${place.whatsapp.replace(/[\s+]/g, '')}`}
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: 'var(--color-primary)' }}
            >
              {place.whatsapp}
            </a>
          </p>
        )}

        {place.website && (
          <p>
            <span className="font-medium text-sm" style={{ color: 'var(--color-navy)' }}>{t('website')}: </span>
            <a
              href={place.website}
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: 'var(--color-primary)' }}
            >
              {place.website}
            </a>
          </p>
        )}

        {place.maps_url && (
          <p>
            <a
              href={place.maps_url}
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: 'var(--color-primary)' }}
            >
              {t('addToMap')}
            </a>
          </p>
        )}

        {place.hours && (
          <p>
            <span className="font-medium text-sm" style={{ color: 'var(--color-navy)' }}>{t('hours')}: </span>
            <span style={{ color: 'var(--color-navy)', opacity: 0.8 }}>{place.hours}</span>
          </p>
        )}
      </div>

      {place.lat !== null && place.lng !== null && (
        <PlaceMap lat={place.lat} lng={place.lng} name={place.name} />
      )}
    </article>
  )
}
