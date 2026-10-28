'use client'

import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'
import { Badge } from '@/ui/Badge'
import type { Place } from '@/lib/supabase/types'
import { SETTINGS } from '@/config/settings'

interface ConfirmCardProps {
  place: Place
  onConfirm: () => void
  onSkip: () => void
}

export function ConfirmCard({ place, onConfirm, onSkip }: ConfirmCardProps) {
  const t = useTranslations('confirmation')
  const tPlace = useTranslations('place')

  return (
    <div
      className="rounded-lg p-4 space-y-3"
      style={{
        border: '1.5px solid color-mix(in srgb, var(--color-navy) 15%, transparent)',
        backgroundColor: 'var(--color-surface)',
      }}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="font-semibold text-base" style={{ color: 'var(--color-navy)' }}>
            {place.name}
          </h3>
          <p className="text-sm" style={{ color: 'var(--color-navy)', opacity: 0.7 }}>
            {place.barrio}
          </p>
        </div>
        <Badge variant="status" status="pending">{tPlace('status.pending')}</Badge>
      </div>

      {place.categories.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {place.categories.map((cat) => (
            <Badge key={cat} variant="category">{cat}</Badge>
          ))}
        </div>
      )}

      <p className="text-xs" style={{ color: 'var(--color-navy)', opacity: 0.6 }}>
        {tPlace('confirmCount', { count: 0, required: SETTINGS.confirmations.required })}
      </p>

      <div className="flex gap-3">
        <Button variant="primary" size="sm" onClick={onConfirm}>
          {t('confirm')}
        </Button>
        <Button variant="ghost" size="sm" onClick={onSkip}>
          {t('notFound')}
        </Button>
      </div>
    </div>
  )
}
