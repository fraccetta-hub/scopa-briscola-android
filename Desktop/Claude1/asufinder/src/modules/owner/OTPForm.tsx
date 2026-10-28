'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'
import { Input } from '@/ui/Input'

interface OTPFormProps {
  placeId: string
  onSuccess: () => void
}

export function OTPForm({ placeId, onSuccess }: OTPFormProps) {
  const t = useTranslations('owner')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`/api/places/${placeId}/verify-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ otp }),
      })
      if (res.ok) {
        setSuccess(true)
        onSuccess()
      } else {
        setError(t('error'))
      }
    } catch {
      setError(t('error'))
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <p className="text-sm font-medium" style={{ color: 'var(--color-mint)' }}>
        {t('verified')}
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm" style={{ color: 'var(--color-navy)' }}>
        {t('otpSent')}
      </p>
      <form onSubmit={handleSubmit} className="flex flex-col gap-2">
        <Input
          type="text"
          inputMode="numeric"
          maxLength={6}
          placeholder={t('otpPlaceholder')}
          value={otp}
          onChange={(e) => setOtp(e.target.value)}
          required
        />
        <Button type="submit" disabled={loading || otp.length !== 6} size="sm">
          {t('verify')}
        </Button>
      </form>
      {error && (
        <p className="text-sm" style={{ color: 'var(--color-error)' }}>
          {error}
        </p>
      )}
    </div>
  )
}
