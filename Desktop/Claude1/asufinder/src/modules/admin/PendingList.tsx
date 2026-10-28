'use client'

// ASUFinder — PendingList: admin review of places awaiting approval

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Badge } from '@/ui/Badge'
import { Button } from '@/ui/Button'
import type { PlaceRow } from '@/lib/supabase/queries/admin'

interface Props {
  places: PlaceRow[]
}

export function PendingList({ places }: Props) {
  const t = useTranslations('admin')
  const tPlace = useTranslations('place')
  const [items, setItems] = useState<PlaceRow[]>(places)
  const [loading, setLoading] = useState<string | null>(null)

  async function updateStatus(id: string, status: 'live' | 'rejected') {
    setLoading(id)
    try {
      const res = await fetch(`/api/admin/places/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      if (!res.ok) throw new Error('Request failed')
      setItems(prev => prev.filter(p => p.id !== id))
    } finally {
      setLoading(null)
    }
  }

  async function toggleFeatured(id: string, current: boolean) {
    setLoading(id + '-feat')
    try {
      const res = await fetch(`/api/admin/places/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ is_featured: !current }),
      })
      if (!res.ok) throw new Error('Request failed')
      setItems(prev => prev.map(p => p.id === id ? { ...p, is_featured: !current } : p))
    } finally {
      setLoading(null)
    }
  }

  if (items.length === 0) {
    return (
      <p className="text-center py-12" style={{ color: 'var(--color-navy)' }}>
        {t('noItems')}
      </p>
    )
  }

  return (
    <div>
      <h2 className="text-xl font-bold mb-4" style={{ color: 'var(--color-navy)' }}>
        {t('pending')}
      </h2>
      <div className="overflow-x-auto rounded-xl" style={{ backgroundColor: 'var(--color-surface)' }}>
        <table className="w-full text-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-bg)' }}>
              <th className="text-left px-4 py-3 font-medium">{tPlace('status').replace(/\s.*/, '')}</th>
              <th className="text-left px-4 py-3 font-medium">Nombre</th>
              <th className="text-left px-4 py-3 font-medium">Barrio</th>
              <th className="text-left px-4 py-3 font-medium">{t('featured')}</th>
              <th className="text-left px-4 py-3 font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map(place => (
              <tr key={place.id} style={{ borderBottom: '1px solid var(--color-bg)' }}>
                <td className="px-4 py-3">
                  <Badge variant="status" status={place.status as 'pending' | 'needs_review'}>
                    {tPlace(`status.${place.status === 'needs_review' ? 'review' : place.status}`)}
                  </Badge>
                </td>
                <td className="px-4 py-3 font-medium">{place.name}</td>
                <td className="px-4 py-3">{place.barrio}</td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => toggleFeatured(place.id, place.is_featured)}
                    disabled={loading === place.id + '-feat'}
                    className="text-xs px-2 py-1 rounded"
                    style={{
                      backgroundColor: place.is_featured ? 'var(--color-accent)' : 'var(--color-bg)',
                      color: place.is_featured ? 'var(--color-white)' : 'var(--color-navy)',
                    }}
                  >
                    {t('featured')}
                  </button>
                </td>
                <td className="px-4 py-3 flex gap-2">
                  <Button
                    size="sm"
                    variant="primary"
                    onClick={() => updateStatus(place.id, 'live')}
                    disabled={loading === place.id}
                  >
                    {t('approve')}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => updateStatus(place.id, 'rejected')}
                    disabled={loading === place.id}
                  >
                    {t('reject')}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
