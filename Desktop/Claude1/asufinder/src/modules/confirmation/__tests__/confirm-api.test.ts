import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock next/server
vi.mock('next/server', () => ({
  NextRequest: class {
    async json() { return {} }
  },
  NextResponse: {
    json: (data: unknown, init?: { status?: number }) => ({
      _data: data,
      _status: init?.status ?? 200,
    }),
  },
}))

// Mock supabase server
vi.mock('@/lib/supabase/server', () => ({
  createClient: vi.fn(),
}))

// Mock places queries
const mockGetPlace = vi.fn()
const mockUpdatePlace = vi.fn()
vi.mock('@/lib/supabase/queries/places', () => ({
  getPlace: (...args: unknown[]) => mockGetPlace(...args),
  updatePlace: (...args: unknown[]) => mockUpdatePlace(...args),
}))

// Mock confirmations queries
const mockAddConfirmation = vi.fn()
const mockGetConfirmationCount = vi.fn()
const mockGetUserConfirmationsToday = vi.fn()
vi.mock('@/lib/supabase/queries/confirmations', () => ({
  addConfirmation: (...args: unknown[]) => mockAddConfirmation(...args),
  getConfirmationCount: (...args: unknown[]) => mockGetConfirmationCount(...args),
  getUserConfirmationsToday: (...args: unknown[]) => mockGetUserConfirmationsToday(...args),
}))

// Mock profiles queries
vi.mock('@/lib/supabase/queries/profiles', () => ({
  updatePoints: vi.fn(),
}))

// Mock flags queries
const mockInsertFlag = vi.fn()
const mockGetFlagCount = vi.fn()
vi.mock('@/lib/supabase/queries/flags', () => ({
  insertFlag: (...args: unknown[]) => mockInsertFlag(...args),
  getFlagCount: (...args: unknown[]) => mockGetFlagCount(...args),
}))

vi.mock('@/config/settings', () => ({
  SETTINGS: {
    confirmations: { required: 3, maxPerUserPerDay: 5, minAccountAgeHours: 24 },
    flagging: { needsReviewThreshold: 3, closedThreshold: 5 },
  },
}))

import { createClient } from '@/lib/supabase/server'
import { POST as confirmPost } from '@/app/api/places/[id]/confirm/route'
import { POST as flagPost } from '@/app/api/places/[id]/flag/route'
import { NextRequest } from 'next/server'

const mockCreateClient = createClient as ReturnType<typeof vi.fn>

function makeAuthClient(userId = 'user-1', createdAt?: string) {
  const created = createdAt ?? new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString()
  return {
    auth: {
      getUser: vi.fn().mockResolvedValue({
        data: { user: { id: userId, created_at: created } },
        error: null,
      }),
    },
  }
}

const fakeRequest = new NextRequest()

describe('POST /api/places/[id]/confirm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCreateClient.mockResolvedValue(makeAuthClient())
    mockGetUserConfirmationsToday.mockResolvedValue(0)
    mockAddConfirmation.mockResolvedValue(undefined)
    mockUpdatePlace.mockResolvedValue({ status: 'live' })
  })

  it('returns 403 when confirmer is the submitter', async () => {
    mockGetPlace.mockResolvedValue({ id: 'p1', added_by: 'user-1', status: 'pending' })
    const res = await confirmPost(fakeRequest, { params: { id: 'p1' } })
    expect((res as { _status: number })._status).toBe(403)
  })

  it('status becomes live after required confirmations', async () => {
    mockGetPlace.mockResolvedValue({ id: 'p1', added_by: 'other-user', status: 'pending' })
    mockGetConfirmationCount.mockResolvedValue(3)
    mockUpdatePlace.mockResolvedValue({ status: 'live' })

    const res = await confirmPost(fakeRequest, { params: { id: 'p1' } })
    expect(mockUpdatePlace).toHaveBeenCalledWith('p1', { status: 'live' })
    expect((res as { _data: { placeStatus: string } })._data.placeStatus).toBe('live')
  })
})

describe('POST /api/places/[id]/flag', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCreateClient.mockResolvedValue(makeAuthClient())
    mockInsertFlag.mockResolvedValue(undefined)
    mockUpdatePlace.mockResolvedValue({ status: 'closed' })
  })

  it('status becomes closed after 5 flags', async () => {
    mockGetFlagCount.mockResolvedValue(5)
    const res = await flagPost(fakeRequest, { params: { id: 'p1' } })
    expect(mockUpdatePlace).toHaveBeenCalledWith('p1', { status: 'closed' })
    expect((res as { _data: { count: number } })._data.count).toBe(5)
  })
})
