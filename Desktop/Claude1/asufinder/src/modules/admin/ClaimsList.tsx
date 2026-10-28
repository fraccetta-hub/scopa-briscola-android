'use client'

// ASUFinder — ClaimsList: admin review of pending owner claims

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'
import type { ClaimRow } from '@/lib/supabase/queries/admin'

interface Props {
  claims: ClaimRow[]
}

export function ClaimsList({ claims }: Props) {
  const t = useTranslations('admin')
  const [items, setItems] = useState<ClaimRow[]>(claims)
  const [loading, setLoading] = useState<string | null>(null)

  async function updateClaim(id: string, approved: boolean) {
    setLoading(id)
    try {
      const res = await fetch(`/api/admin/claims/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approved }),
      })
      if (!res.ok) throw new Error('Request failed')
      setItems(prev => prev.filter(c => c.id !== id))
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
        {t('claims')}
      </h2>
      <div className="overflow-x-auto rounded-xl" style={{ backgroundColor: 'var(--color-surface)' }}>
        <table className="w-full text-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-bg)' }}>
              <th className="text-left px-4 py-3 font-medium">Lugar</th>
              <th className="text-left px-4 py-3 font-medium">Barrio</th>
              <th className="text-left px-4 py-3 font-medium">Usuario</th>
              <th className="text-left px-4 py-3 font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map(claim => (
              <tr key={claim.id} style={{ borderBottom: '1px solid var(--color-bg)' }}>
                <td className="px-4 py-3 font-medium">{claim.places?.name ?? claim.place_id}</td>
                <td className="px-4 py-3">{claim.places?.barrio ?? '—'}</td>
                <td className="px-4 py-3">{claim.profiles?.username ?? claim.user_id}</td>
                <td className="px-4 py-3 flex gap-2">
                  <Button
                    size="sm"
                    variant="primary"
                    onClick={() => updateClaim(claim.id, true)}
                    disabled={loading === claim.id}
                  >
                    {t('approve')}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => updateClaim(claim.id, false)}
                    disabled={loading === claim.id}
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
