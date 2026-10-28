'use client'

import { useEffect, useState, useCallback } from 'react'
import { useTranslations } from 'next-intl'
import type { ReviewRow } from '@/lib/supabase/queries/reviews'
import { AddReviewForm } from './AddReviewForm'

interface ReviewListProps {
  placeId: string
}

export function ReviewList({ placeId }: ReviewListProps) {
  const t = useTranslations('reviews')
  const [reviews, setReviews] = useState<ReviewRow[]>([])
  const [loading, setLoading] = useState(true)

  const fetchReviews = useCallback(async () => {
    try {
      const res = await fetch(`/api/places/${placeId}/reviews`)
      if (res.ok) {
        const data = await res.json() as ReviewRow[]
        setReviews(data)
      }
    } finally {
      setLoading(false)
    }
  }, [placeId])

  useEffect(() => {
    void fetchReviews()
  }, [fetchReviews])

  return (
    <section className="flex flex-col gap-4">
      <h2
        className="text-lg font-semibold"
        style={{ color: 'var(--color-navy)' }}
      >
        {t('title')}
      </h2>

      {loading ? null : reviews.length === 0 ? (
        <p style={{ color: 'var(--color-navy)', opacity: 0.6, fontSize: 14 }}>
          {t('noReviews')}
        </p>
      ) : (
        <ul className="flex flex-col gap-3">
          {reviews.map((review) => (
            <li
              key={review.id}
              style={{
                border: '1px solid var(--color-border)',
                borderRadius: 8,
                padding: '12px 16px',
                background: 'var(--color-surface)',
              }}
            >
              <p style={{ color: 'var(--color-navy)', fontSize: 14, marginBottom: 4 }}>
                {review.body}
              </p>
              <p style={{ color: 'var(--color-navy)', opacity: 0.5, fontSize: 12 }}>
                {new Date(review.created_at).toLocaleDateString()}
              </p>
            </li>
          ))}
        </ul>
      )}

      <div>
        <p
          className="font-medium text-sm mb-2"
          style={{ color: 'var(--color-navy)' }}
        >
          {t('addReview')}
        </p>
        <AddReviewForm placeId={placeId} onSuccess={fetchReviews} />
      </div>
    </section>
  )
}
