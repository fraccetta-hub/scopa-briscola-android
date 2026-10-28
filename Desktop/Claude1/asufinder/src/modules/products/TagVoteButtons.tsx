'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import type { PlaceProduct } from '@/lib/supabase/queries/products'

interface TagVoteButtonsProps {
  tag: PlaceProduct
  onVote: () => void
}

export function TagVoteButtons({ tag, onVote }: TagVoteButtonsProps) {
  const t = useTranslations('products')
  const [hidden, setHidden] = useState(false)

  if (hidden) return null

  async function handleVote(type: 'confirm' | 'deny') {
    const res = await fetch(`/api/products/${tag.id}/vote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type }),
    })
    if (res.status === 409) {
      // already voted
      setHidden(true)
      return
    }
    if (res.ok) {
      onVote()
    }
  }

  return (
    <span className="inline-flex items-center gap-1 ml-1">
      <button
        onClick={() => handleVote('confirm')}
        title={t('confirm')}
        className="text-xs px-1 rounded"
        style={{ color: 'var(--color-primary)', border: '1px solid var(--color-primary)' }}
      >
        ✓ {tag.confirmed_count}
      </button>
      <button
        onClick={() => handleVote('deny')}
        title={t('deny')}
        className="text-xs px-1 rounded"
        style={{ color: 'var(--color-danger, #c0392b)', border: '1px solid var(--color-danger, #c0392b)' }}
      >
        ✗ {tag.denied_count}
      </button>
    </span>
  )
}
