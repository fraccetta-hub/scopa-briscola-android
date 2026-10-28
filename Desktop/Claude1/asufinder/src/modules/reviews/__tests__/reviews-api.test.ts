import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock next/server
vi.mock('next/server', () => ({
  NextRequest: class {
    private _body: unknown
    constructor(_url: string, init?: { body?: string }) {
      try { this._body = init?.body ? JSON.parse(init.body) : {} } catch { this._body = {} }
    }
    async json() { return this._body }
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

// Mock reviews queries
const mockGetReviews = vi.fn()
const mockGetUserReviewsToday = vi.fn()
const mockGetUserReviewForPlace = vi.fn()
const mockInsertReview = vi.fn()
vi.mock('@/lib/supabase/queries/reviews', () => ({
  getReviews: (placeId: unknown) => mockGetReviews(placeId),
  getUserReviewsToday: (userId: unknown) => mockGetUserReviewsToday(userId),
  getUserReviewForPlace: (placeId: unknown, userId: unknown) => mockGetUserReviewForPlace(placeId, userId),
  insertReview: (placeId: unknown, userId: unknown, body: unknown) => mockInsertReview(placeId, userId, body),
}))

// Mock profiles queries
vi.mock('@/lib/supabase/queries/profiles', () => ({
  updatePoints: vi.fn(),
}))

vi.mock('@/config/settings', () => ({
  SETTINGS: {
    submissions: { maxReviewsPerUserPerDay: 10, minAccountAgeMinutes: 60 },
    points: { writeReview: 1 },
  },
}))

import { createClient } from '@/lib/supabase/server'
import { GET, POST } from '@/app/api/places/[id]/reviews/route'
import { NextRequest } from 'next/server'

const mockCreateClient = createClient as ReturnType<typeof vi.fn>

function makeAuthClient(userId = 'user-1', createdAtOffset = -2 * 60 * 60 * 1000) {
  const createdAt = new Date(Date.now() + createdAtOffset).toISOString()
  return {
    auth: {
      getUser: vi.fn().mockResolvedValue({
        data: { user: { id: userId, created_at: createdAt } },
        error: null,
      }),
    },
  }
}

function makeAnonClient() {
  return {
    auth: {
      getUser: vi.fn().mockResolvedValue({
        data: { user: null },
        error: { message: 'Not authenticated' },
      }),
    },
  }
}

function makeRequest(body?: object) {
  return new NextRequest('http://localhost/api/places/p1/reviews', {
    body: body ? JSON.stringify(body) : undefined,
  })
}

describe('GET /api/places/[id]/reviews', () => {
  it('returns reviews without auth', async () => {
    mockGetReviews.mockResolvedValue([{ id: 'r1', body: 'Great!', place_id: 'p1', user_id: 'u1', created_at: '2024-01-01' }])
    const req = makeRequest()
    const res = await GET(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(200)
    expect((res as unknown as { _data: unknown[] })._data).toHaveLength(1)
  })
})

describe('POST /api/places/[id]/reviews', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCreateClient.mockResolvedValue(makeAuthClient())
    mockGetUserReviewForPlace.mockResolvedValue(null)
    mockGetUserReviewsToday.mockResolvedValue(0)
    mockInsertReview.mockResolvedValue({ id: 'r1', body: 'Good', place_id: 'p1', user_id: 'user-1', created_at: '2024-01-01' })
  })

  it('returns 401 when unauthenticated', async () => {
    mockCreateClient.mockResolvedValue(makeAnonClient())
    const req = makeRequest({ body: 'Nice place' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(401)
  })

  it('returns 409 when user already reviewed this place', async () => {
    mockGetUserReviewForPlace.mockResolvedValue({ id: 'r0', body: 'Old', place_id: 'p1', user_id: 'user-1', created_at: '2024-01-01' })
    const req = makeRequest({ body: 'Another review' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(409)
    expect((res as unknown as { _data: { error: string } })._data.error).toBe('AlreadyReviewed')
  })

  it('returns 429 when daily rate limit exceeded', async () => {
    mockGetUserReviewsToday.mockResolvedValue(10)
    const req = makeRequest({ body: 'Great spot' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(429)
    expect((res as unknown as { _data: { error: string } })._data.error).toBe('RateLimitExceeded')
  })

  it('inserts review and returns 201 on valid POST', async () => {
    const req = makeRequest({ body: 'Loved it!' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(201)
    expect(mockInsertReview).toHaveBeenCalledWith('p1', 'user-1', 'Loved it!')
    expect((res as unknown as { _data: { id: string } })._data.id).toBe('r1')
  })

  it('returns 400 on empty body', async () => {
    const req = makeRequest({ body: '   ' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(400)
  })
})
