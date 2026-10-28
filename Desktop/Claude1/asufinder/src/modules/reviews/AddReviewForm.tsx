'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'

interface AddReviewFormProps {
  placeId: string
  onSuccess: () => void
}

export function AddReviewForm({ placeId, onSuccess }: AddReviewFormProps) {
  const t = useTranslations('reviews')
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const res = await fetch(`/api/places/${placeId}/reviews`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ body }),
      })

      if (res.status === 409) {
        setError(t('alreadyReviewed'))
        return
      }
      if (res.status === 429) {
        setError(t('rateLimit'))
        return
      }
      if (!res.ok) {
        setError(`Error ${res.status}`)
        return
      }

      setBody('')
      setSuccess(true)
      onSuccess()
    } finally {
      setSubmitting(false)
    }
  }

  if (success) {
    return (
      <p style={{ color: 'var(--color-primary)', fontWeight: 500 }}>
        {t('success')}
      </p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3">
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={t('placeholder')}
        maxLength={1000}
        rows={4}
        required
        disabled={submitting}
        style={{
          border: '1px solid var(--color-border)',
          borderRadius: 6,
          padding: '8px 12px',
          fontSize: 14,
          color: 'var(--color-navy)',
          background: 'var(--color-surface)',
          resize: 'vertical',
        }}
      />
      {error && (
        <p style={{ color: 'var(--color-danger)', fontSize: 14 }}>{error}</p>
      )}
      <div>
        <Button type="submit" size="sm" variant="primary" disabled={submitting || body.trim().length === 0}>
          {t('submit')}
        </Button>
      </div>
    </form>
  )
}
