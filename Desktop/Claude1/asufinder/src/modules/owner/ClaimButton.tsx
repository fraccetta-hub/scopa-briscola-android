'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Button } from '@/ui/Button'
import { OTPForm } from './OTPForm'

interface ClaimButtonProps {
  placeId: string
  hasWhatsapp: boolean
}

export function ClaimButton({ placeId, hasWhatsapp }: ClaimButtonProps) {
  const t = useTranslations('owner')
  const [state, setState] = useState<'idle' | 'loading' | 'otp' | 'no_whatsapp' | 'error' | 'claimed'>('idle')

  async function handleClaim() {
    setState('loading')
    try {
      const res = await fetch(`/api/places/${placeId}/claim`, { method: 'POST' })
      if (res.ok) {
        setState('otp')
      } else {
        const body = await res.json() as { error?: string }
        if (body.error === 'no_whatsapp') {
          setState('no_whatsapp')
        } else {
          setState('error')
        }
      }
    } catch {
      setState('error')
    }
  }

  if (state === 'claimed') {
    return (
      <p className="text-sm font-medium" style={{ color: 'var(--color-mint)' }}>
        {t('verified')}
      </p>
    )
  }

  if (state === 'otp') {
    return <OTPForm placeId={placeId} onSuccess={() => setState('claimed')} />
  }

  return (
    <div className="flex flex-col gap-2">
      <Button
        onClick={handleClaim}
        disabled={state === 'loading' || !hasWhatsapp}
        variant="secondary"
        size="sm"
      >
        {t('claim')}
      </Button>
      {state === 'no_whatsapp' && (
        <p className="text-sm" style={{ color: 'var(--color-error)' }}>
          {t('noWhatsapp')}
        </p>
      )}
      {state === 'error' && (
        <p className="text-sm" style={{ color: 'var(--color-error)' }}>
          {t('error')}
        </p>
      )}
    </div>
  )
}
