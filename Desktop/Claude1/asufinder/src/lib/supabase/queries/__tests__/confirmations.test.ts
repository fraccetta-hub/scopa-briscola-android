import { describe, it, expect, vi, beforeEach } from 'vitest'
import { addConfirmation, getConfirmationCount, getUserConfirmationsToday } from '../confirmations'

vi.mock('@/config/settings', () => ({
  SETTINGS: {
    confirmations: { required: 3, maxPerUserPerDay: 5, minAccountAgeHours: 24 },
  },
}))

type BuilderResult = { data?: unknown; count?: number | null; error: unknown }

function makeBuilder(resolved: BuilderResult) {
  let res = resolved
  // Make the builder thenable so that `await builder` resolves to `res`.
  // Individual terminal methods (insert, gte) also resolve to `res`.
  // Chainable methods (select, eq) return `this` BUT the builder itself is
  // thenable, so awaiting the chain after any of these also works.
  const b: Record<string, unknown> & { _resolve: (v: BuilderResult) => void } = {
    then: (onfulfilled: (v: BuilderResult) => unknown) => Promise.resolve(res).then(onfulfilled),
    select: vi.fn().mockReturnThis(),
    insert: vi.fn().mockImplementation(() => Promise.resolve(res)),
    eq: vi.fn().mockReturnThis(),
    gte: vi.fn().mockImplementation(() => Promise.resolve(res)),
    _resolve: (v: BuilderResult) => { res = v },
  }
  return b
}

let mockBuilder: ReturnType<typeof makeBuilder>

vi.mock('../../server', () => ({
  createClient: vi.fn(async () => ({
    from: vi.fn(() => mockBuilder),
  })),
}))

describe('addConfirmation', () => {
  beforeEach(() => {
    mockBuilder = makeBuilder({ error: null })
  })

  it('calls .insert() with the correct shape', async () => {
    await addConfirmation('place-1', 'user-1')
    expect(mockBuilder.insert).toHaveBeenCalledWith({
      place_id: 'place-1',
      user_id: 'user-1',
    })
  })

  it('throws when Supabase returns an error', async () => {
    mockBuilder._resolve({ error: new Error('constraint') })
    await expect(addConfirmation('place-1', 'user-1')).rejects.toThrow('constraint')
  })
})

describe('getConfirmationCount', () => {
  beforeEach(() => {
    mockBuilder = makeBuilder({ count: 3, error: null })
  })

  it('returns the count from Supabase', async () => {
    const count = await getConfirmationCount('place-1')
    expect(count).toBe(3)
  })

  it('returns 0 when count is null', async () => {
    mockBuilder._resolve({ count: null, error: null })
    const count = await getConfirmationCount('place-1')
    expect(count).toBe(0)
  })
})

describe('getUserConfirmationsToday', () => {
  beforeEach(() => {
    mockBuilder = makeBuilder({ count: 2, error: null })
  })

  it('returns the count', async () => {
    const count = await getUserConfirmationsToday('user-1')
    expect(count).toBe(2)
  })
})
