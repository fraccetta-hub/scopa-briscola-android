import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  getPendingPlaces,
  updatePlaceStatus,
  getPendingClaims,
  updateClaimStatus,
  getAllUsers,
  updateUserRole,
  toggleFeatured,
} from '../admin'

// ---------------------------------------------------------------------------
// Mock the admin client module
// ---------------------------------------------------------------------------
type MockBuilder = {
  select: ReturnType<typeof vi.fn>
  in: ReturnType<typeof vi.fn>
  order: ReturnType<typeof vi.fn>
  eq: ReturnType<typeof vi.fn>
  update: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
  single: ReturnType<typeof vi.fn>
  _resolvedValue: unknown
}

function makeBuilder(resolvedValue: unknown): MockBuilder {
  const b: MockBuilder = {
    select: vi.fn().mockReturnThis(),
    in: vi.fn().mockReturnThis(),
    order: vi.fn().mockImplementation(() => Promise.resolve(resolvedValue)),
    eq: vi.fn().mockImplementation(() => Promise.resolve(resolvedValue)),
    update: vi.fn().mockReturnThis(),
    delete: vi.fn().mockImplementation(() => Promise.resolve(resolvedValue)),
    single: vi.fn().mockImplementation(() => Promise.resolve(resolvedValue)),
    _resolvedValue: resolvedValue,
  }
  return b
}

let mockBuilder: MockBuilder
const mockFrom = vi.fn()

vi.mock('../../admin', () => ({
  createAdminClient: vi.fn(() => ({
    from: mockFrom,
  })),
}))

beforeEach(() => {
  vi.clearAllMocks()
})

// ---------------------------------------------------------------------------
// getPendingPlaces
// ---------------------------------------------------------------------------
describe('getPendingPlaces', () => {
  it('returns array of places with pending/needs_review status', async () => {
    const fakePlaces = [
      { id: '1', name: 'Tienda A', status: 'pending', barrio: 'Centro', categories: [] },
      { id: '2', name: 'Tienda B', status: 'needs_review', barrio: 'Villa', categories: [] },
    ]
    mockBuilder = makeBuilder({ data: fakePlaces, error: null })
    mockFrom.mockReturnValue(mockBuilder)

    const result = await getPendingPlaces()

    expect(mockFrom).toHaveBeenCalledWith('places')
    expect(mockBuilder.in).toHaveBeenCalledWith('status', ['pending', 'needs_review'])
    expect(result).toEqual(fakePlaces)
  })

  it('throws when supabase returns error', async () => {
    mockBuilder = makeBuilder({ data: null, error: { message: 'DB error' } })
    mockFrom.mockReturnValue(mockBuilder)

    await expect(getPendingPlaces()).rejects.toThrow('DB error')
  })
})

// ---------------------------------------------------------------------------
// updatePlaceStatus
// ---------------------------------------------------------------------------
describe('updatePlaceStatus', () => {
  it('calls update with status live on places table', async () => {
    const updateMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: null })
    mockFrom.mockReturnValue({ update: updateMock, eq: eqMock })
    updateMock.mockReturnValue({ eq: eqMock })

    await updatePlaceStatus('place-123', 'live')

    expect(mockFrom).toHaveBeenCalledWith('places')
    expect(updateMock).toHaveBeenCalled()
    const updateArg = updateMock.mock.calls[0][0] as Record<string, unknown>
    expect(updateArg.status).toBe('live')
    expect(eqMock).toHaveBeenCalledWith('id', 'place-123')
  })

  it('throws when update fails', async () => {
    const updateMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: { message: 'Update failed' } })
    mockFrom.mockReturnValue({ update: updateMock, eq: eqMock })
    updateMock.mockReturnValue({ eq: eqMock })

    await expect(updatePlaceStatus('id', 'rejected')).rejects.toThrow('Update failed')
  })
})

// ---------------------------------------------------------------------------
// getPendingClaims
// ---------------------------------------------------------------------------
describe('getPendingClaims', () => {
  it('returns unverified claims with place and profile data', async () => {
    const fakeClaims = [
      { id: 'c1', place_id: 'p1', user_id: 'u1', verified: false, places: { name: 'Shop', barrio: 'Centro' }, profiles: { username: 'alice' } },
    ]
    const selectMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockReturnThis()
    const orderMock = vi.fn().mockResolvedValue({ data: fakeClaims, error: null })
    mockFrom.mockReturnValue({ select: selectMock, eq: eqMock, order: orderMock })
    selectMock.mockReturnValue({ eq: eqMock })
    eqMock.mockReturnValue({ order: orderMock })

    const result = await getPendingClaims()

    expect(mockFrom).toHaveBeenCalledWith('place_owners')
    expect(result).toEqual(fakeClaims)
  })
})

// ---------------------------------------------------------------------------
// updateClaimStatus — approve
// ---------------------------------------------------------------------------
describe('updateClaimStatus', () => {
  it('updates verified=true when approved', async () => {
    const updateMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: null })
    mockFrom.mockReturnValue({ update: updateMock, eq: eqMock })
    updateMock.mockReturnValue({ eq: eqMock })

    await updateClaimStatus('claim-1', true, 'admin-user-id')

    expect(mockFrom).toHaveBeenCalledWith('place_owners')
    const updateArg = updateMock.mock.calls[0][0] as Record<string, unknown>
    expect(updateArg.verified).toBe(true)
    expect(updateArg.approved_by).toBe('admin-user-id')
  })

  it('deletes the row when rejected', async () => {
    const deleteMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: null })
    mockFrom.mockReturnValue({ delete: deleteMock, eq: eqMock })
    deleteMock.mockReturnValue({ eq: eqMock })

    await updateClaimStatus('claim-2', false, 'admin-user-id')

    expect(deleteMock).toHaveBeenCalled()
    expect(eqMock).toHaveBeenCalledWith('id', 'claim-2')
  })
})

// ---------------------------------------------------------------------------
// getAllUsers
// ---------------------------------------------------------------------------
describe('getAllUsers', () => {
  it('returns all profiles', async () => {
    const fakeUsers = [{ id: 'u1', username: 'alice', role: 'user', points: 10 }]
    const selectMock = vi.fn().mockReturnThis()
    const orderMock = vi.fn().mockResolvedValue({ data: fakeUsers, error: null })
    mockFrom.mockReturnValue({ select: selectMock, order: orderMock })
    selectMock.mockReturnValue({ order: orderMock })

    const result = await getAllUsers()

    expect(mockFrom).toHaveBeenCalledWith('profiles')
    expect(result).toEqual(fakeUsers)
  })
})

// ---------------------------------------------------------------------------
// updateUserRole
// ---------------------------------------------------------------------------
describe('updateUserRole', () => {
  it('calls update with the given role', async () => {
    const updateMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: null })
    mockFrom.mockReturnValue({ update: updateMock, eq: eqMock })
    updateMock.mockReturnValue({ eq: eqMock })

    await updateUserRole('user-id', 'admin')

    const updateArg = updateMock.mock.calls[0][0] as Record<string, unknown>
    expect(updateArg.role).toBe('admin')
    expect(eqMock).toHaveBeenCalledWith('id', 'user-id')
  })
})

// ---------------------------------------------------------------------------
// toggleFeatured
// ---------------------------------------------------------------------------
describe('toggleFeatured', () => {
  it('sets is_featured and featured_until', async () => {
    const updateMock = vi.fn().mockReturnThis()
    const eqMock = vi.fn().mockResolvedValue({ data: null, error: null })
    mockFrom.mockReturnValue({ update: updateMock, eq: eqMock })
    updateMock.mockReturnValue({ eq: eqMock })

    await toggleFeatured('place-1', true, '2026-12-31')

    const updateArg = updateMock.mock.calls[0][0] as Record<string, unknown>
    expect(updateArg.is_featured).toBe(true)
    expect(updateArg.featured_until).toBe('2026-12-31')
  })
})
