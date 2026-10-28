import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { ClaimsList } from '../ClaimsList'
import type { ClaimRow } from '@/lib/supabase/queries/admin'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

const fakeClaims: ClaimRow[] = [
  {
    id: 'c1',
    place_id: 'p1',
    user_id: 'u1',
    verified: false,
    verified_at: null,
    approved_by: null,
    created_at: '2026-01-01T00:00:00Z',
    places: { name: 'Tienda Test', barrio: 'Centro' },
    profiles: { username: 'alice' },
  },
]

describe('ClaimsList', () => {
  it('renders approve and reject buttons for each claim', () => {
    render(<ClaimsList claims={fakeClaims} />)
    expect(screen.getByText('approve')).toBeInTheDocument()
    expect(screen.getByText('reject')).toBeInTheDocument()
  })

  it('shows place name and username', () => {
    render(<ClaimsList claims={fakeClaims} />)
    expect(screen.getByText('Tienda Test')).toBeInTheDocument()
    expect(screen.getByText('alice')).toBeInTheDocument()
  })

  it('shows noItems when list is empty', () => {
    render(<ClaimsList claims={[]} />)
    expect(screen.getByText('noItems')).toBeInTheDocument()
  })
})
