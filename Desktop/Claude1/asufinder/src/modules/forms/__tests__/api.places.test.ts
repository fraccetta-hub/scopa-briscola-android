import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock next/server
vi.mock('next/server', () => ({
  NextRequest: class {
    _body: string
    constructor(_url: string, init: { method: string; body: string }) {
      this._body = init?.body ?? '{}'
    }
    async json() { return JSON.parse(this._body) }
  },
  NextResponse: {
    json: (data: unknown, init?: { status?: number }) => ({
      _data: data,
      _status: init?.status ?? 200,
      json: async () => data,
      status: init?.status ?? 200,
    }),
  },
}))

// Mock supabase server
vi.mock('@/lib/supabase/server', () => ({
  createClient: vi.fn(),
}))

// Mock moderation
vi.mock('@/modules/moderation/moderation.service', () => ({
  checkContent: vi.fn(() => ({ passed: true })),
}))

// Mock places queries
const mockInsertPlace = vi.fn()
const mockCheckDuplicates = vi.fn((_name: unknown, _barrio: unknown) => Promise.resolve([]))
vi.mock('@/lib/supabase/queries/places', () => ({
  insertPlace: (data: unknown) => mockInsertPlace(data),
  checkDuplicates: (name: unknown, barrio: unknown) => mockCheckDuplicates(name, barrio),
}))

// Mock profiles queries
vi.mock('@/lib/supabase/queries/profiles', () => ({
  updatePoints: vi.fn(),
}))

import { createClient } from '@/lib/supabase/server'
import { checkContent } from '@/modules/moderation/moderation.service'
import { POST } from '@/app/api/places/route'
import { NextRequest } from 'next/server'

const mockCreateClient = createClient as ReturnType<typeof vi.fn>
const mockCheckContent = checkContent as ReturnType<typeof vi.fn>

function makeAuthClient(userId = 'user-1') {
  return {
    auth: {
      getUser: vi.fn().mockResolvedValue({ data: { user: { id: userId } }, error: null }),
    },
  }
}

function makeRequest(body: object) {
  return new NextRequest('http://localhost/api/places', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

describe('POST /api/places', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCheckContent.mockReturnValue({ passed: true })
    mockCheckDuplicates.mockResolvedValue([])
    mockCreateClient.mockResolvedValue(makeAuthClient())
  })

  it('sets status live when maps_url provided', async () => {
    const place = { id: 'p1', name: 'Test', status: 'live' }
    mockInsertPlace.mockResolvedValue(place)

    const req = makeRequest({
      name: 'Test Place',
      categories: ['farmacia'],
      barrio: 'Centro',
      maps_url: 'https://maps.google.com/test',
    })

    await POST(req)

    expect(mockInsertPlace).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'live' }),
    )
  })

  it('sets status pending when no maps_url', async () => {
    const place = { id: 'p2', name: 'Test', status: 'pending' }
    mockInsertPlace.mockResolvedValue(place)

    const req = makeRequest({
      name: 'Test Place',
      categories: ['farmacia'],
      barrio: 'Centro',
    })

    await POST(req)

    expect(mockInsertPlace).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'pending' }),
    )
  })

  it('returns 400 for blacklisted content', async () => {
    mockCheckContent.mockReturnValue({ passed: false, reason: 'blacklisted: spam' })

    const req = makeRequest({
      name: 'spam place',
      categories: ['farmacia'],
      barrio: 'Centro',
    })

    const res = await POST(req)
    expect((res as unknown as { _status: number })._status).toBe(400)
  })
})
