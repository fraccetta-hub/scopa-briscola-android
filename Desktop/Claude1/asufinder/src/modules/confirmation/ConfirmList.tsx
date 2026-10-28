'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { ConfirmCard } from './ConfirmCard'
import type { Place } from '@/lib/supabase/types'

interface ConfirmListProps {
  places: Place[]
}

export function ConfirmList({ places }: ConfirmListProps) {
  const t = useTranslations('confirmation')
  const [queue, setQueue] = useState<Place[]>(places)
  const [message, setMessage] = useState<string | null>(null)

  async function handleConfirm(place: Place) {
    try {
      const res = await fetch(`/api/places/${place.id}/confirm`, { method: 'POST' })
      if (res.ok) {
        setMessage(t('success'))
      } else {
        const body = await res.json() as { error?: string }
        setMessage(body.error ?? '')
      }
    } catch {
      setMessage('')
    }
    setQueue((prev) => prev.filter((p) => p.id !== place.id))
  }

  function handleSkip(place: Place) {
    setQueue((prev) => prev.filter((p) => p.id !== place.id))
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold" style={{ color: 'var(--color-navy)' }}>
        {t('title')}
      </h1>
      {message && (
        <p className="text-sm" style={{ color: 'var(--color-primary)' }}>{message}</p>
      )}
      {queue.length === 0 ? (
        <p className="text-sm" style={{ color: 'var(--color-navy)', opacity: 0.6 }}>
          {t('description')}
        </p>
      ) : (
        queue.map((place) => (
          <ConfirmCard
            key={place.id}
            place={place}
            onConfirm={() => handleConfirm(place)}
            onSkip={() => handleSkip(place)}
          />
        ))
      )}
    </div>
  )
}
