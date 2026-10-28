import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import React from 'react'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

import { Leaderboard } from '../Leaderboard'

describe('Leaderboard', () => {
  const profiles = [
    { id: '1', username: 'Alice', points: 100 },
    { id: '2', username: 'Bob', points: 50 },
  ]

  it('renders top contributors in order', () => {
    render(React.createElement(Leaderboard, { profiles }))
    const items = screen.getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Alice')
    expect(items[1]).toHaveTextContent('Bob')
  })

  it('shows medal for top 3', () => {
    const threeProfiles = [
      { id: '1', username: 'Alice', points: 100 },
      { id: '2', username: 'Bob', points: 80 },
      { id: '3', username: 'Carol', points: 60 },
    ]
    render(React.createElement(Leaderboard, { profiles: threeProfiles }))
    expect(screen.getByText('🥇')).toBeInTheDocument()
    expect(screen.getByText('🥈')).toBeInTheDocument()
    expect(screen.getByText('🥉')).toBeInTheDocument()
  })
})
