'use client'

import { useTranslations } from 'next-intl'
import type { Profile } from '@/lib/supabase/types'

const MEDALS = ['🥇', '🥈', '🥉']

interface Props {
  profiles: Pick<Profile, 'id' | 'username' | 'points'>[]
}

export function Leaderboard({ profiles }: Props) {
  const t = useTranslations('auth')

  return (
    <div className="max-w-xl mx-auto p-6">
      <h1
        className="text-2xl font-bold mb-6"
        style={{ color: 'var(--color-navy)' }}
      >
        {t('leaderboardTitle')}
      </h1>
      <ol className="flex flex-col gap-2" style={{ listStyle: 'none', padding: 0 }}>
        {profiles.map((profile, index) => {
          const isTop3 = index < 3
          return (
            <li
              key={profile.id}
              className="flex items-center gap-4 rounded-lg px-4 py-3"
              style={{
                backgroundColor: isTop3 ? 'var(--color-mint)' : 'var(--color-surface)',
                border: '1px solid color-mix(in srgb, var(--color-navy) 10%, transparent)',
              }}
            >
              <span
                className="w-8 text-center font-bold"
                style={{
                  fontSize: isTop3 ? '1.25rem' : '0.875rem',
                  color: 'var(--color-navy)',
                }}
              >
                {isTop3 ? MEDALS[index] : index + 1}
              </span>
              <span
                className="flex-1 font-medium"
                style={{
                  fontSize: isTop3 ? '1.125rem' : '1rem',
                  color: isTop3 ? 'var(--color-primary)' : 'var(--color-navy)',
                }}
              >
                {profile.username}
              </span>
              <span
                className="font-bold"
                style={{ color: 'var(--color-accent)' }}
              >
                {profile.points} {t('points')}
              </span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}
