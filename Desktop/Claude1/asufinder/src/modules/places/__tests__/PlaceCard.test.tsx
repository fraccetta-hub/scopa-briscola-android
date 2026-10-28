import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { PlaceCard } from '../PlaceCard'
import type { Place } from '@/lib/supabase/types'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  ),
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

describe('PlaceCard', () => {
  it('renders place name and barrio', () => {
    render(<PlaceCard place={mockPlace} />)
    expect(screen.getByText('Farmacia Central')).toBeInTheDocument()
    expect(screen.getByText(/Centro/)).toBeInTheDocument()
  })

  it('shows WhatsApp button only when whatsapp prop is set', () => {
    const { rerender } = render(<PlaceCard place={{ ...mockPlace, whatsapp: null }} />)
    expect(screen.queryByText(/whatsapp/i)).not.toBeInTheDocument()
    rerender(<PlaceCard place={{ ...mockPlace, whatsapp: '595981234567' }} />)
    expect(screen.getByRole('link', { name: /whatsapp/i })).toBeInTheDocument()
  })
})
