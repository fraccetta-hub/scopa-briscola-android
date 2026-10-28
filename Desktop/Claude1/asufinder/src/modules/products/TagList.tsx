'use client'

import { useState, useEffect, useCallback } from 'react'
import { useTranslations } from 'next-intl'
import type { PlaceProduct } from '@/lib/supabase/queries/products'
import { TagVoteButtons } from './TagVoteButtons'
import { AddTagForm } from './AddTagForm'

interface TagListProps {
  placeId: string
  userId?: string | null
}

export function TagList({ placeId, userId = null }: TagListProps) {
  const t = useTranslations('products')
  const [tags, setTags] = useState<PlaceProduct[]>([])
  const [loading, setLoading] = useState(true)

  const fetchTags = useCallback(async () => {
    setLoading(true)
    try {
      const res = await fetch(`/api/places/${placeId}/products`)
      if (res.ok) {
        const data = await res.json() as PlaceProduct[]
        setTags(data)
      }
    } finally {
      setLoading(false)
    }
  }, [placeId])

  useEffect(() => {
    void fetchTags()
  }, [fetchTags])

  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-lg font-semibold" style={{ color: 'var(--color-navy)' }}>
        {t('title')}
      </h2>
      {loading ? null : tags.length === 0 ? (
        <p className="text-sm" style={{ opacity: 0.6 }}>{t('noTags')}</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {tags.map((tag) => (
            <span
              key={tag.id}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-sm"
              style={
                tag.status === 'confirmed'
                  ? { background: 'var(--color-primary)', color: '#fff' }
                  : { background: 'var(--color-primary)', color: '#fff', opacity: 0.5 }
              }
              title={tag.status === 'uncertain' ? t('uncertain') : undefined}
            >
              {tag.tag}
              <TagVoteButtons tag={tag} onVote={fetchTags} />
            </span>
          ))}
        </div>
      )}
      {userId !== null && (
        <AddTagForm placeId={placeId} onSuccess={fetchTags} />
      )}
    </section>
  )
}
