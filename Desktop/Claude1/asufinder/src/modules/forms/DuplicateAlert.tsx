'use client'

import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'
import type { Place } from '@/lib/supabase/types'

interface DuplicateAlertProps {
  matches: Place[]
  onContinue: () => void
  onCancel: () => void
}

export function DuplicateAlert({ matches, onContinue, onCancel }: DuplicateAlertProps) {
  const t = useTranslations('forms.addPlace')

  return (
    <div
      className="rounded-lg p-4 mb-4"
      style={{
        border: '1.5px solid var(--color-warning, #f59e0b)',
        backgroundColor: 'color-mix(in srgb, var(--color-warning, #f59e0b) 10%, var(--color-surface))',
      }}
    >
      <p className="font-medium mb-2" style={{ color: 'var(--color-navy)' }}>
        {t('duplicateWarning')}
      </p>
      <ul className="mb-4 space-y-1">
        {matches.map((place) => (
          <li key={place.id}>
            <a
              href={`/place/${place.id}`}
              className="underline text-sm"
              style={{ color: 'var(--color-primary)' }}
              target="_blank"
              rel="noopener noreferrer"
            >
              {place.name} — {place.barrio}
            </a>
          </li>
        ))}
      </ul>
      <div className="flex gap-3">
        <Button variant="primary" size="sm" onClick={onContinue}>
          {t('continueAnyway')}
        </Button>
        <Button variant="secondary" size="sm" onClick={onCancel}>
          {t('cancel')}
        </Button>
      </div>
    </div>
  )
}
