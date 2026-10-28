// Tests for products query logic — voteTag status transitions and addTag status setting.
// Uses mocked supabase clients to verify the actual patch applied to place_products.

import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock supabase clients before importing the module under test
vi.mock('@/lib/supabase/client', () => ({ createClient: vi.fn() }))
vi.mock('@/lib/supabase/admin', () => ({ createAdminClient: vi.fn() }))
vi.mock('@/lib/supabase/queries/profiles', () => ({
  updatePoints: vi.fn(),
  updatePointsAdmin: vi.fn(),
}))
vi.mock('@/config/settings', () => ({
  SETTINGS: {
    tags: { confirmThreshold: 2, denyThreshold: 3, expiryDays: 90, denyPointDeduction: -2 },
  },
}))

import { createClient } from '@/lib/supabase/client'
import { createAdminClient } from '@/lib/supabase/admin'
import { voteTag, addTag } from '../products'

const mockCreateClient = createClient as ReturnType<typeof vi.fn>
const mockCreateAdminClient = createAdminClient as ReturnType<typeof vi.fn>

function makeInsertChain(result = { data: {}, error: null }) {
  const chain = {
    insert: vi.fn().mockReturnThis(),
    select: vi.fn().mockReturnThis(),
    single: vi.fn().mockResolvedValue(result),
  }
  return chain
}

function makeAdminClient(tagState: Record<string, unknown>, captureUpdate?: { patch: unknown }) {
  let updateFn: ReturnType<typeof vi.fn>
  if (captureUpdate) {
    updateFn = vi.fn((patch: unknown) => {
      captureUpdate.patch = patch
      return {
        eq: vi.fn().mockReturnThis(),
        select: vi.fn().mockReturnThis(),
        single: vi.fn().mockResolvedValue({ data: { ...tagState, ...patch as object }, error: null }),
      }
    })
  } else {
    updateFn = vi.fn().mockReturnValue({
      eq: vi.fn().mockReturnThis(),
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({ data: tagState, error: null }),
    })
  }

  return {
    from: vi.fn((table: string) => {
      if (table === 'place_products') {
        return {
          select: vi.fn().mockReturnThis(),
          eq: vi.fn().mockReturnThis(),
          single: vi.fn().mockResolvedValue({ data: tagState, error: null }),
          update: updateFn,
        }
      }
      return makeInsertChain()
    }),
  }
}

function makeRegularClient() {
  return {
    from: vi.fn(() => makeInsertChain()),
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('addTag', () => {
  it('inserts with status=confirmed when isOwner=true', async () => {
    let insertedRow: unknown
    const mockClient = {
      from: vi.fn(() => ({
        insert: vi.fn((row: unknown) => {
          insertedRow = row
          return {
            select: vi.fn().mockReturnThis(),
            single: vi.fn().mockResolvedValue({
              data: { id: 't1', ...(row as object) },
              error: null,
            }),
          }
        }),
      })),
    }
    mockCreateClient.mockReturnValue(mockClient)

    const result = await addTag('place-1', 'zapatillas', 'user-1', true)
    expect((insertedRow as Record<string, unknown>).status).toBe('confirmed')
    expect(result.status).toBe('confirmed')
  })

  it('inserts with status=pending when isOwner=false', async () => {
    let insertedRow: unknown
    const mockClient = {
      from: vi.fn(() => ({
        insert: vi.fn((row: unknown) => {
          insertedRow = row
          return {
            select: vi.fn().mockReturnThis(),
            single: vi.fn().mockResolvedValue({
              data: { id: 't2', ...(row as object) },
              error: null,
            }),
          }
        }),
      })),
    }
    mockCreateClient.mockReturnValue(mockClient)

    const result = await addTag('place-1', 'zapatillas', 'user-1', false)
    expect((insertedRow as Record<string, unknown>).status).toBe('pending')
    expect(result.status).toBe('pending')
  })
})

describe('voteTag — confirm transitions', () => {
  it('2 confirm votes on pending tag → patch has status=confirmed', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'pending', confirmed_count: 1, denied_count: 0,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'confirm', false)

    expect((captured.patch as Record<string, unknown>).status).toBe('confirmed')
    expect((captured.patch as Record<string, unknown>).confirmed_count).toBe(2)
  })

  it('1 confirm vote on pending tag → status stays pending', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'pending', confirmed_count: 0, denied_count: 0,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'confirm', false)

    expect((captured.patch as Record<string, unknown>).status).toBeUndefined()
    expect((captured.patch as Record<string, unknown>).confirmed_count).toBe(1)
  })

  it('confirm on uncertain tag → patch has status=confirmed', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'uncertain', confirmed_count: 1, denied_count: 0,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'confirm', false)

    expect((captured.patch as Record<string, unknown>).status).toBe('confirmed')
  })
})

describe('voteTag — deny transitions', () => {
  it('3 deny votes → patch has status=denied', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'pending', confirmed_count: 0, denied_count: 2,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'deny', false)

    expect((captured.patch as Record<string, unknown>).status).toBe('denied')
    expect((captured.patch as Record<string, unknown>).denied_count).toBe(3)
  })

  it('owner deny on pending tag → patch has status=denied regardless of count', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'pending', confirmed_count: 0, denied_count: 0,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'deny', true)

    expect((captured.patch as Record<string, unknown>).status).toBe('denied')
  })

  it('deny on already-denied tag → status NOT updated again (no double transition)', async () => {
    const tagState = {
      id: 'pp1', place_id: 'p1', tag: 'zapatillas',
      status: 'denied', confirmed_count: 0, denied_count: 3,
      added_by: 'other', expires_at: null,
    }
    const captured: { patch: unknown } = { patch: null }
    mockCreateClient.mockReturnValue(makeRegularClient())
    mockCreateAdminClient.mockReturnValue(makeAdminClient(tagState, captured))

    await voteTag('pp1', 'user-1', 'deny', false)

    expect((captured.patch as Record<string, unknown>).status).toBeUndefined()
  })
})
