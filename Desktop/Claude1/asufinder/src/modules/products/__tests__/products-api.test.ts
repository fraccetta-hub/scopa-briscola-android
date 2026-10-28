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

// Mock products queries
const mockGetPlaceTags = vi.fn()
const mockAddTag = vi.fn()
const mockGetUserTagsToday = vi.fn()
const mockGetPlaceTagsThisWeek = vi.fn()
const mockVoteTag = vi.fn()
const mockGetUserVoteForTag = vi.fn()

vi.mock('@/lib/supabase/queries/products', () => ({
  getPlaceTags: (id: unknown) => mockGetPlaceTags(id),
  addTag: (placeId: unknown, tag: unknown, userId: unknown, isOwner: unknown) => mockAddTag(placeId, tag, userId, isOwner),
  getUserTagsToday: (userId: unknown) => mockGetUserTagsToday(userId),
  getPlaceTagsThisWeek: (placeId: unknown) => mockGetPlaceTagsThisWeek(placeId),
  voteTag: (id: unknown, userId: unknown, type: unknown, isOwner: unknown) => mockVoteTag(id, userId, type, isOwner),
  getUserVoteForTag: (id: unknown, userId: unknown) => mockGetUserVoteForTag(id, userId),
}))

// Mock moderation
vi.mock('@/modules/moderation/moderation.service', () => ({
  checkContent: vi.fn(() => ({ passed: true })),
}))

// Mock profiles
vi.mock('@/lib/supabase/queries/profiles', () => ({
  updatePoints: vi.fn(),
  updatePointsAdmin: vi.fn(),
}))

vi.mock('@/config/settings', () => ({
  SETTINGS: {
    submissions: {
      maxTagsPerUserPerDay: 10,
      maxNewTagsPerPlacePerWeek: 20,
      minAccountAgeMinutes: 60,
    },
    points: { addTag: 1 },
  },
}))

import { createClient } from '@/lib/supabase/server'
import { GET, POST } from '@/app/api/places/[id]/products/route'
import { POST as VotePOST } from '@/app/api/products/[id]/vote/route'
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
    from: vi.fn(() => ({
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({ data: { id: 'p1', status: 'live' }, error: null }),
      maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
    })),
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
    from: vi.fn(() => ({
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({ data: null, error: { message: 'auth error' } }),
      maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
    })),
  }
}

function makeFromChain(responses: Record<string, unknown> = {}) {
  const chain = {
    select: vi.fn().mockReturnThis(),
    eq: vi.fn().mockReturnThis(),
    single: vi.fn().mockResolvedValue(responses['single'] ?? { data: { id: 'p1', status: 'live' }, error: null }),
    maybeSingle: vi.fn().mockResolvedValue(responses['maybeSingle'] ?? { data: null, error: null }),
  }
  return vi.fn(() => chain)
}

function makeRequest(body?: object, url = 'http://localhost/api/places/p1/products') {
  return new NextRequest(url, {
    body: body ? JSON.stringify(body) : undefined,
  })
}

describe('GET /api/places/[id]/products', () => {
  it('returns tags without auth', async () => {
    mockGetPlaceTags.mockResolvedValue([{ id: 't1', tag: 'zapatillas', status: 'confirmed' }])
    const req = makeRequest()
    const res = await GET(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(200)
    expect((res as unknown as { _data: unknown[] })._data).toHaveLength(1)
  })
})

describe('POST /api/places/[id]/products', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const client = makeAuthClient()
    client.from = makeFromChain()
    mockCreateClient.mockResolvedValue(client)
    mockGetUserTagsToday.mockResolvedValue(0)
    mockGetPlaceTagsThisWeek.mockResolvedValue(0)
    mockAddTag.mockResolvedValue({ id: 't1', tag: 'zapatillas', status: 'pending', place_id: 'p1', added_by: 'user-1', confirmed_count: 0, denied_count: 0, expires_at: '', created_at: '' })
  })

  it('returns 401 when unauthenticated', async () => {
    mockCreateClient.mockResolvedValue(makeAnonClient())
    const req = makeRequest({ tag: 'zapatillas' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(401)
  })

  it('POST tag as regular user → status=pending', async () => {
    mockAddTag.mockResolvedValue({ id: 't1', tag: 'zapatillas', status: 'pending', place_id: 'p1', added_by: 'user-1', confirmed_count: 0, denied_count: 0, expires_at: '', created_at: '' })
    const req = makeRequest({ tag: 'zapatillas' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(201)
    expect((res as unknown as { _data: { status: string } })._data.status).toBe('pending')
  })

  it('POST tag as owner → status=confirmed', async () => {
    // Simulate owner claim found
    const client = makeAuthClient()
    client.from = vi.fn((table: string) => {
      if (table === 'places') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: { id: 'p1', status: 'live' }, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      if (table === 'place_products') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: null, error: { message: 'not found' } }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      if (table === 'place_owners') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: { id: 'o1' }, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: { id: 'o1' }, error: null }),
        }
      }
      return {
        select: vi.fn().mockReturnThis(),
        eq: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: null, error: null }),
        maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
      }
    }) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)
    mockAddTag.mockResolvedValue({ id: 't1', tag: 'zapatillas', status: 'confirmed', place_id: 'p1', added_by: 'user-1', confirmed_count: 0, denied_count: 0, expires_at: '', created_at: '' })

    const req = makeRequest({ tag: 'zapatillas' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(201)
    expect((res as unknown as { _data: { status: string } })._data.status).toBe('confirmed')
    expect(mockAddTag).toHaveBeenCalledWith('p1', 'zapatillas', 'user-1', true)
  })

  it('Duplicate tag → 409', async () => {
    const client = makeAuthClient()
    client.from = vi.fn((table: string) => {
      if (table === 'places') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: { id: 'p1', status: 'live' }, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      if (table === 'place_products') {
        // duplicate found
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: null, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: { id: 'existing' }, error: null }),
        }
      }
      return {
        select: vi.fn().mockReturnThis(),
        eq: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: null, error: null }),
        maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
      }
    }) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)

    const req = makeRequest({ tag: 'zapatillas' })
    const res = await POST(req, { params: { id: 'p1' } })
    expect((res as unknown as { _status: number })._status).toBe(409)
  })
})

describe('POST /api/products/[id]/vote', () => {
  const pendingTag = { id: 'pp1', place_id: 'p1', tag: 'zapatillas', status: 'pending', confirmed_count: 0, denied_count: 0, added_by: 'other-user', expires_at: '', created_at: '' }

  beforeEach(() => {
    vi.clearAllMocks()
    const client = makeAuthClient()
    client.from = vi.fn((table: string) => {
      if (table === 'place_products') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: pendingTag, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      return {
        select: vi.fn().mockReturnThis(),
        eq: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: null, error: null }),
        maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
      }
    }) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)
    mockGetUserVoteForTag.mockResolvedValue(null)
    mockVoteTag.mockResolvedValue(undefined)
  })

  it('returns 401 when unauthenticated', async () => {
    mockCreateClient.mockResolvedValue(makeAnonClient())
    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'confirm' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(401)
  })

  it('returns 200 on valid confirm vote', async () => {
    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'confirm' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(200)
    expect(mockVoteTag).toHaveBeenCalledWith('pp1', 'user-1', 'confirm', false)
  })

  it('returns 409 when already voted', async () => {
    mockGetUserVoteForTag.mockResolvedValue({ id: 'v1', type: 'confirm' })
    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'confirm' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(409)
  })

  it('returns 404 when tag not found', async () => {
    const client = makeAuthClient()
    client.from = vi.fn(() => ({
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({ data: null, error: { message: 'not found', code: 'PGRST116' } }),
      maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
    })) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)

    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'confirm' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(404)
  })
})

describe('voteTag logic (via products queries)', () => {
  // These tests verify the voting logic in query layer by testing the route
  // with controlled mock states

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetUserVoteForTag.mockResolvedValue(null)
    mockVoteTag.mockResolvedValue(undefined)
  })

  it('2 confirm votes on pending tag → voteTag called with confirm', async () => {
    const client = makeAuthClient()
    const tag = { id: 'pp1', place_id: 'p1', tag: 'termos', status: 'pending', confirmed_count: 1, denied_count: 0, added_by: 'other', expires_at: '', created_at: '' }
    client.from = vi.fn((table: string) => {
      if (table === 'place_products') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: tag, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      return {
        select: vi.fn().mockReturnThis(),
        eq: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: null, error: null }),
        maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
      }
    }) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)

    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'confirm' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(200)
    expect(mockVoteTag).toHaveBeenCalledWith('pp1', 'user-1', 'confirm', false)
  })

  it('owner deny → voteTag called with isOwner=true', async () => {
    const client = makeAuthClient()
    const tag = { id: 'pp1', place_id: 'p1', tag: 'termos', status: 'pending', confirmed_count: 0, denied_count: 0, added_by: 'other', expires_at: '', created_at: '' }
    client.from = vi.fn((table: string) => {
      if (table === 'place_products') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: tag, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
        }
      }
      if (table === 'place_owners') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: { id: 'o1' }, error: null }),
          maybeSingle: vi.fn().mockResolvedValue({ data: { id: 'o1' }, error: null }),
        }
      }
      return {
        select: vi.fn().mockReturnThis(),
        eq: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: null, error: null }),
        maybeSingle: vi.fn().mockResolvedValue({ data: null, error: null }),
      }
    }) as unknown as typeof client.from
    mockCreateClient.mockResolvedValue(client)

    const req = new NextRequest('http://localhost/api/products/pp1/vote', { body: JSON.stringify({ type: 'deny' }) })
    const res = await VotePOST(req, { params: { id: 'pp1' } })
    expect((res as unknown as { _status: number })._status).toBe(200)
    expect(mockVoteTag).toHaveBeenCalledWith('pp1', 'user-1', 'deny', true)
  })
})
