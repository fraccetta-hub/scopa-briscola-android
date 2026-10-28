import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { PlaceDetail } from '../PlaceDetail'
import type { Place } from '@/lib/supabase/types'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

vi.mock('next/dynamic', () => ({
  default: () => null,
}))

vi.mock('@/modules/reviews/ReviewList', () => ({
  ReviewList: () => null,
}))

vi.mock('@/modules/products/TagList', () => ({
  TagList: () => null,
}))

const mockPlace: Place = {
  id: 'test-id',
  name: 'Farmacia Central',
  categories: ['farmacia'],
  tags: [],
  barrio: 'Centro',
  address: 'Av. España 123',
  lat: null,
  lng: null,
  phone: null,
  whatsapp: null,
  website: null,
  maps_url: null,
  hours: null,
  photo_url: null,
  status: 'live',
  source: 'user',
  is_featured: false,
  featured_until: null,
  added_by: null,
  created_at: '2024-01-01T00:00:00Z',
  updated_at: '2024-01-01T00:00:00Z',
}

describe('PlaceDetail', () => {
  it('renders place name and details', () => {
    render(<PlaceDetail place={mockPlace} />)
    expect(screen.getByRole('heading', { name: 'Farmacia Central' })).toBeInTheDocument()
    expect(screen.getByText('Centro')).toBeInTheDocument()
  })

  it('does not render map when lat/lng absent', () => {
    render(<PlaceDetail place={{ ...mockPlace, lat: null, lng: null }} />)
    expect(screen.queryByTestId('place-map')).not.toBeInTheDocument()
  })
})
