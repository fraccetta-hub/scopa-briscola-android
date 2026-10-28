'use client'

// ASUFinder — UsersList: admin management of user roles and bans

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Badge } from '@/ui/Badge'
import { Button } from '@/ui/Button'
import type { ProfileRow } from '@/lib/supabase/queries/admin'

interface Props {
  users: ProfileRow[]
  /** Role of the currently logged-in admin (passed from server component via cookie/session) */
  callerRole?: 'admin' | 'superadmin'
}

export function UsersList({ users, callerRole = 'admin' }: Props) {
  const t = useTranslations('admin')
  const [items, setItems] = useState<ProfileRow[]>(users)
  const [loading, setLoading] = useState<string | null>(null)

  async function updateRole(id: string, role: string) {
    setLoading(id + '-role')
    try {
      const res = await fetch(`/api/admin/users/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role }),
      })
      if (!res.ok) throw new Error('Request failed')
      setItems(prev => prev.map(u => u.id === id ? { ...u, role: role as ProfileRow['role'] } : u))
    } finally {
      setLoading(null)
    }
  }

  async function banUser(id: string) {
    setLoading(id + '-ban')
    try {
      const res = await fetch(`/api/admin/users/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ banned: true }),
      })
      if (!res.ok) throw new Error('Request failed')
      setItems(prev => prev.map(u => u.id === id ? { ...u, role: 'user' as ProfileRow['role'] } : u))
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
        {t('users')}
      </h2>
      <div className="overflow-x-auto rounded-xl" style={{ backgroundColor: 'var(--color-surface)' }}>
        <table className="w-full text-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-bg)' }}>
              <th className="text-left px-4 py-3 font-medium">{t('colUsername')}</th>
              <th className="text-left px-4 py-3 font-medium">{t('role')}</th>
              <th className="text-left px-4 py-3 font-medium">{t('colPoints')}</th>
              <th className="text-left px-4 py-3 font-medium">{t('colActions')}</th>
            </tr>
          </thead>
          <tbody>
            {items.map(user => {
              const isTargetSuperadmin = user.role === 'superadmin'
              const canModify = callerRole === 'superadmin' || !isTargetSuperadmin
              return (
                <tr key={user.id} style={{ borderBottom: '1px solid var(--color-bg)' }}>
                  <td className="px-4 py-3 font-medium">{user.username}</td>
                  <td className="px-4 py-3">
                    <Badge variant="role">{user.role}</Badge>
                  </td>
                  <td className="px-4 py-3">{user.points}</td>
                  <td className="px-4 py-3">
                    {canModify ? (
                      <div className="flex gap-2 flex-wrap">
                        <select
                          value={user.role}
                          disabled={loading === user.id + '-role'}
                          onChange={e => updateRole(user.id, e.target.value)}
                          className="text-xs border rounded px-1 py-0.5"
                          style={{ borderColor: 'var(--color-bg)', backgroundColor: 'var(--color-surface)' }}
                        >
                          <option value="user">user</option>
                          <option value="admin">admin</option>
                          {callerRole === 'superadmin' && <option value="superadmin">superadmin</option>}
                        </select>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => banUser(user.id)}
                          disabled={loading === user.id + '-ban'}
                        >
                          {t('ban')}
                        </Button>
                      </div>
                    ) : (
                      <span className="text-xs" style={{ color: 'var(--color-navy)', opacity: 0.5 }}>—</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
