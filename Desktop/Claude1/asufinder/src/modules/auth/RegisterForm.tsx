'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import Link from 'next/link'
import { createClient } from '@/lib/supabase/client'
import { Button } from '@/ui/Button'
import { Input } from '@/ui/Input'

export function RegisterForm() {
  const t = useTranslations('auth')
  const tAuth = useTranslations('auth')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [username, setUsername] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    const supabase = createClient()
    const { error: authError } = await supabase.auth.signUp({
      email,
      password,
      options: { data: { username } },
    })
    setLoading(false)
    if (authError) {
      setError(authError.message)
      return
    }
    setSuccess(true)
  }

  if (success) {
    return (
      <div className="flex flex-col gap-3">
        <p style={{ color: 'var(--color-primary)', fontWeight: 500 }}>
          {tAuth('checkEmail')}
        </p>
        <p className="text-sm" style={{ color: 'var(--color-navy)', opacity: 0.75 }}>
          ¿Primera vez?{' '}
          <Link href="/como-funciona" style={{ color: 'var(--color-primary)' }}>
            Lee cómo funciona →
          </Link>
        </p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <Input
        label={t('username')}
        id="username"
        type="text"
        autoComplete="username"
        value={username}
        onChange={e => setUsername(e.target.value)}
        required
      />
      <Input
        label={t('email')}
        id="email"
        type="email"
        autoComplete="email"
        value={email}
        onChange={e => setEmail(e.target.value)}
        required
      />
      <Input
        label={t('password')}
        id="password"
        type="password"
        autoComplete="new-password"
        value={password}
        onChange={e => setPassword(e.target.value)}
        required
      />
      {error && (
        <p style={{ color: 'var(--color-error)', fontSize: '0.875rem' }}>{error}</p>
      )}
      <Button type="submit" disabled={loading}>
        {loading ? '...' : t('registerButton')}
      </Button>
    </form>
  )
}
