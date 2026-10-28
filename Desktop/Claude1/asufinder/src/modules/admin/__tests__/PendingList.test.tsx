import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { PendingList } from '../PendingList'
import type { PlaceRow } from '@/lib/supabase/queries/admin'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

const fakePlaces: PlaceRow[] = [
  {
    id: 'p1',
    name: 'Tienda Alpha',
    barrio: 'Centro',
    categories: [],
    tags: [],
    status: 'pending',
    source: 'user',
    is_featured: false,
    featured_until: null,
    added_by: null,
    address: null,
    lat: null,
    lng: null,
    phone: null,
    whatsapp: null,
    website: null,
    maps_url: null,
    hours: null,
    photo_url: null,
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
]

describe('PendingList', () => {
  it('renders approve and reject buttons', () => {
    render(<PendingList places={fakePlaces} />)
    expect(screen.getByText('approve')).toBeInTheDocument()
    expect(screen.getByText('reject')).toBeInTheDocument()
  })

  it('renders the place name', () => {
    render(<PendingList places={fakePlaces} />)
    expect(screen.getByText('Tienda Alpha')).toBeInTheDocument()
  })

  it('shows noItems when list is empty', () => {
    render(<PendingList places={[]} />)
    expect(screen.getByText('noItems')).toBeInTheDocument()
  })
})
