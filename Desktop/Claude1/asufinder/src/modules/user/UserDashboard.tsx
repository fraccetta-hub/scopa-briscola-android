'use client'

import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import type { User } from '@supabase/supabase-js'
import type { Place, Profile } from '@/lib/supabase/types'
import { createClient } from '@/lib/supabase/client'
import { Button } from '@/ui/Button'
import { Badge } from '@/ui/Badge'

interface Props {
  user: User
  places: Place[]
  profile: Profile | null
}

export function UserDashboard({ user, places, profile }: Props) {
  const t = useTranslations('auth')
  const router = useRouter()

  async function handleLogout() {
    const supabase = createClient()
    await supabase.auth.signOut()
    router.push('/')
  }

  const displayName = profile?.username ?? user.email ?? user.id

  return (
    <div
      className="max-w-3xl mx-auto p-6"
      style={{ color: 'var(--color-navy)' }}
    >
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">{t('dashboard')}</h1>
        <Button variant="secondary" onClick={handleLogout}>
          {t('logout')}
        </Button>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <div
          className="rounded-lg p-4 text-center"
          style={{ backgroundColor: 'var(--color-mint)' }}
        >
          <p className="text-sm">{t('contributor')}</p>
          <p className="font-bold text-lg truncate">{displayName}</p>
        </div>
        <div
          className="rounded-lg p-4 text-center"
          style={{ backgroundColor: 'var(--color-mint)' }}
        >
          <p className="text-sm">{t('points')}</p>
          <p className="font-bold text-lg">{profile?.points ?? 0}</p>
        </div>
        <div
          className="rounded-lg p-4 text-center"
          style={{ backgroundColor: 'var(--color-mint)' }}
        >
          <p className="text-sm">{t('placesContributed')}</p>
          <p className="font-bold text-lg">{places.length}</p>
        </div>
      </div>

      {places.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr style={{ borderBottom: '1px solid var(--color-bg)' }}>
                <th className="text-left py-2 pr-4">Nombre</th>
                <th className="text-left py-2 pr-4">Barrio</th>
                <th className="text-left py-2 pr-4">Estado</th>
                <th className="text-left py-2">Fecha</th>
              </tr>
            </thead>
            <tbody>
              {places.map(place => (
                <tr
                  key={place.id}
                  style={{ borderBottom: '1px solid var(--color-bg)' }}
                >
                  <td className="py-2 pr-4 font-medium">{place.name}</td>
                  <td className="py-2 pr-4">{place.barrio}</td>
                  <td className="py-2 pr-4">
                    <Badge
                      variant="status"
                      status={place.status === 'needs_review' ? 'needs_review' : place.status as 'live' | 'pending' | 'closed' | 'rejected'}
                    >
                      {place.status}
                    </Badge>
                  </td>
                  <td className="py-2">
                    {new Date(place.created_at).toLocaleDateString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
