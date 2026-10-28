'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { CATEGORIES } from '@/config/categories'
import { Input } from '@/ui/Input'
import { Button } from '@/ui/Button'
import { DuplicateAlert } from './DuplicateAlert'
import type { Place } from '@/lib/supabase/types'

interface FormData {
  name: string
  categories: string[]
  barrio: string
  address: string
  phone: string
  whatsapp: string
  website: string
  maps_url: string
  hours: string
  products: string
}

interface FormErrors {
  name?: string
  categories?: string
  barrio?: string
}

export function AddPlaceForm() {
  const t = useTranslations('forms.addPlace')
  const tErr = useTranslations('errors')
  const router = useRouter()

  const [formData, setFormData] = useState<FormData>({
    name: '',
    categories: [],
    barrio: '',
    address: '',
    phone: '',
    whatsapp: '',
    website: '',
    maps_url: '',
    hours: '',
    products: '',
  })
  const [errors, setErrors] = useState<FormErrors>({})
  const [submitting, setSubmitting] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const [duplicates, setDuplicates] = useState<Place[] | null>(null)
  const [pendingSubmit, setPendingSubmit] = useState(false)

  function validate(): boolean {
    const newErrors: FormErrors = {}
    if (formData.name.trim().length < 2) {
      newErrors.name = tErr('tooShort', { min: 2 })
    }
    if (formData.categories.length === 0) {
      newErrors.categories = tErr('required')
    }
    if (!formData.barrio.trim()) {
      newErrors.barrio = tErr('required')
    }
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  async function submitToApi(force = false) {
    setSubmitting(true)
    setServerError(null)

    try {
      const res = await fetch('/api/places', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...formData, force }),
      })

      if (res.status === 409 && !force) {
        const body = await res.json() as { matches: Place[] }
        setDuplicates(body.matches)
        setPendingSubmit(true)
        setSubmitting(false)
        return
      }

      if (!res.ok) {
        const body = await res.json() as { error?: string }
        setServerError(body.error ?? tErr('generic'))
        setSubmitting(false)
        return
      }

      const created = await res.json() as Place
      router.push(`/place/${created.id}`)
    } catch {
      setServerError(tErr('generic'))
      setSubmitting(false)
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    await submitToApi(false)
  }

  function handleCategoryToggle(slug: string) {
    setFormData((prev) => ({
      ...prev,
      categories: prev.categories.includes(slug)
        ? prev.categories.filter((c) => c !== slug)
        : [...prev.categories, slug],
    }))
  }

  function handleContinueAnyway() {
    setDuplicates(null)
    setPendingSubmit(false)
    void submitToApi(true)
  }

  function handleCancelDuplicate() {
    setDuplicates(null)
    setPendingSubmit(false)
    setSubmitting(false)
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 max-w-xl mx-auto p-4">
      <h1 className="text-2xl font-bold" style={{ color: 'var(--color-navy)' }}>
        {t('title')}
      </h1>

      {duplicates && pendingSubmit && (
        <DuplicateAlert
          matches={duplicates}
          onContinue={handleContinueAnyway}
          onCancel={handleCancelDuplicate}
        />
      )}

      {serverError && (
        <p className="text-sm" style={{ color: 'var(--color-error)' }}>{serverError}</p>
      )}

      <Input
        label={t('name')}
        value={formData.name}
        onChange={(e) => setFormData((p) => ({ ...p, name: e.target.value }))}
        error={errors.name}
        required
      />

      <div>
        <p className="text-sm font-medium mb-2" style={{ color: 'var(--color-navy)' }}>
          {t('category')} *
        </p>
        <div className="grid grid-cols-2 gap-2">
          {CATEGORIES.map((cat) => (
            <label key={cat.slug} className="flex items-center gap-2 cursor-pointer text-sm">
              <input
                type="checkbox"
                checked={formData.categories.includes(cat.slug)}
                onChange={() => handleCategoryToggle(cat.slug)}
              />
              {cat.icon} {cat.label.es}
            </label>
          ))}
        </div>
        {errors.categories && (
          <p className="text-xs mt-1" style={{ color: 'var(--color-error)' }}>{errors.categories}</p>
        )}
      </div>

      <Input
        label={t('barrio')}
        value={formData.barrio}
        onChange={(e) => setFormData((p) => ({ ...p, barrio: e.target.value }))}
        error={errors.barrio}
        required
      />

      <Input
        label={t('address')}
        value={formData.address}
        onChange={(e) => setFormData((p) => ({ ...p, address: e.target.value }))}
      />

      <Input
        label={t('phone')}
        value={formData.phone}
        onChange={(e) => setFormData((p) => ({ ...p, phone: e.target.value }))}
      />

      <Input
        label={t('whatsapp')}
        value={formData.whatsapp}
        onChange={(e) => setFormData((p) => ({ ...p, whatsapp: e.target.value }))}
      />

      <Input
        label={t('website')}
        value={formData.website}
        onChange={(e) => setFormData((p) => ({ ...p, website: e.target.value }))}
      />

      <Input
        label="Google Maps URL (opcional)"
        value={formData.maps_url}
        onChange={(e) => setFormData((p) => ({ ...p, maps_url: e.target.value }))}
      />

      <Input
        label="Horario (opcional)"
        value={formData.hours}
        onChange={(e) => setFormData((p) => ({ ...p, hours: e.target.value }))}
      />

      <div className="flex flex-col gap-1">
        <label className="text-sm font-medium" style={{ color: 'var(--color-navy)' }}>
          {t('products')}
        </label>
        <p className="text-xs" style={{ color: 'var(--color-navy)', opacity: 0.6 }}>
          {t('productsHint')}
        </p>
        <textarea
          value={formData.products}
          onChange={(e) => setFormData((p) => ({ ...p, products: e.target.value }))}
          rows={3}
          className="rounded-md p-2 text-sm resize-none"
          style={{
            border: '1.5px solid color-mix(in srgb, var(--color-navy) 20%, transparent)',
            backgroundColor: 'var(--color-surface)',
            color: 'var(--color-navy)',
          }}
        />
      </div>

      <div className="flex gap-3">
        <Button type="submit" variant="primary" disabled={submitting}>
          {submitting ? '...' : t('submit')}
        </Button>
        <Button type="button" variant="secondary" onClick={() => router.back()}>
          {t('cancel')}
        </Button>
      </div>
    </form>
  )
}
