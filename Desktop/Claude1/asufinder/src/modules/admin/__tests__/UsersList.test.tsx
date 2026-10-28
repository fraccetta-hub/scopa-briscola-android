import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { UsersList } from '../UsersList'
import type { ProfileRow } from '@/lib/supabase/queries/admin'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

const fakeUsers: ProfileRow[] = [
  { id: 'u1', username: 'alice', role: 'user', points: 10, created_at: '2026-01-01T00:00:00Z' },
  { id: 'u2', username: 'bob', role: 'superadmin', points: 50, created_at: '2026-01-02T00:00:00Z' },
]

describe('UsersList', () => {
  it('renders usernames', () => {
    render(<UsersList users={fakeUsers} callerRole="superadmin" />)
    expect(screen.getByText('alice')).toBeInTheDocument()
    expect(screen.getByText('bob')).toBeInTheDocument()
  })

  it('does not show action controls for superadmin target when caller is admin', () => {
    render(<UsersList users={fakeUsers} callerRole="admin" />)
    // alice can be modified; bob (superadmin) cannot
    const selects = screen.queryAllByRole('combobox')
    // Only alice should have a select (bob is hidden behind —)
    expect(selects.length).toBe(1)
  })

  it('shows noItems for empty list', () => {
    render(<UsersList users={[]} />)
    expect(screen.getByText('noItems')).toBeInTheDocument()
  })
})
