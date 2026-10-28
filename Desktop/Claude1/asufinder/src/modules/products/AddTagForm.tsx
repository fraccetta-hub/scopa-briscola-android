'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'

interface AddTagFormProps {
  placeId: string
  onSuccess: () => void
}

export function AddTagForm({ placeId, onSuccess }: AddTagFormProps) {
  const t = useTranslations('products')
  const tErrors = useTranslations('errors')
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!value.trim()) return
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`/api/places/${placeId}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tag: value }),
      })
      if (res.status === 409) {
        setError(t('duplicateTag'))
      } else if (res.status === 429) {
        setError(t('rateLimit'))
      } else if (res.status === 422) {
        setError(t('blocked'))
      } else if (res.ok) {
        setValue('')
        onSuccess()
      } else {
        setError(tErrors('generic'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 mt-2">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        maxLength={50}
        placeholder={t('placeholder')}
        className="flex-1 border rounded px-2 py-1 text-sm"
        style={{ borderColor: 'var(--color-navy)', color: 'var(--color-navy)' }}
        disabled={loading}
      />
      <button
        type="submit"
        disabled={loading || !value.trim()}
        className="px-3 py-1 rounded text-sm font-medium"
        style={{ background: 'var(--color-primary)', color: 'var(--color-white)' }}
      >
        {t('submit')}
      </button>
      {error && (
        <span className="text-sm self-center" style={{ color: 'var(--color-error)' }}>
          {error}
        </span>
      )}
    </form>
  )
}
