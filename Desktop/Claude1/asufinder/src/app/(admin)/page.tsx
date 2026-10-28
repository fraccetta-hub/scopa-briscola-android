// ASUFinder — Admin dashboard overview (server component)

import { getTranslations } from 'next-intl/server'
import { getPendingPlaces, getPendingClaims } from '@/lib/supabase/queries/admin'

export default async function AdminPage() {
  const t = await getTranslations('admin')

  const [pending, claims] = await Promise.all([
    getPendingPlaces(),
    getPendingClaims(),
  ])

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6" style={{ color: 'var(--color-navy)' }}>
        {t('title')}
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="rounded-xl p-6" style={{ backgroundColor: 'var(--color-surface)' }}>
          <p className="text-sm font-medium mb-1" style={{ color: 'var(--color-navy)' }}>
            {t('pending')}
          </p>
          <p className="text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>
            {pending.length}
          </p>
        </div>
        <div className="rounded-xl p-6" style={{ backgroundColor: 'var(--color-surface)' }}>
          <p className="text-sm font-medium mb-1" style={{ color: 'var(--color-navy)' }}>
            {t('claims')}
          </p>
          <p className="text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>
            {claims.length}
          </p>
        </div>
      </div>
    </div>
  )
}
