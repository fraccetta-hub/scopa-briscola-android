// ASUFinder — Admin DB queries (service-role client, bypasses RLS)
// All admin data access goes through this file.
// NEVER use createAdminClient() in components — only in server-side admin API routes / server components.

import { createAdminClient } from '../admin'
import type { Database } from '../types'

export type PlaceRow = Database['public']['Tables']['places']['Row']
export type PlaceStatus = PlaceRow['status']
export type ProfileRow = Database['public']['Tables']['profiles']['Row']
export type PlaceOwnerRow = Database['public']['Tables']['place_owners']['Row']

export type ClaimRow = PlaceOwnerRow & {
  places?: { name: string; barrio: string } | null
  profiles?: { username: string } | null
}

// ---------------------------------------------------------------------------
// Places
// ---------------------------------------------------------------------------

/** Returns all places with status pending or needs_review, ordered by created_at asc. */
export async function getPendingPlaces(): Promise<PlaceRow[]> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('places')
    .select('*')
    .in('status', ['pending', 'needs_review'])
    .order('created_at', { ascending: true })

  if (error) throw new Error(error.message)
  return (data ?? []) as PlaceRow[]
}

/** Updates the status of a place. */
export async function updatePlaceStatus(id: string, status: PlaceStatus): Promise<void> {
  const supabase = createAdminClient()
  // TS5.9 + @supabase/ssr incompatibility: .update() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('places')
    .update({ status, updated_at: new Date().toISOString() } as any)
    .eq('id', id) as { data: unknown; error: { message: string } | null }

  if (error) throw new Error(error.message)
}

/** Toggles featured status for a place. */
export async function toggleFeatured(
  id: string,
  isFeatured: boolean,
  featuredUntil?: string,
): Promise<void> {
  const supabase = createAdminClient()
  // TS5.9 + @supabase/ssr incompatibility: .update() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('places')
    .update({ is_featured: isFeatured, featured_until: featuredUntil ?? null } as any)
    .eq('id', id) as { data: unknown; error: { message: string } | null }

  if (error) throw new Error(error.message)
}

// ---------------------------------------------------------------------------
// Claims
// ---------------------------------------------------------------------------

/** Returns all place_owners where verified=false (pending claims). */
export async function getPendingClaims(): Promise<ClaimRow[]> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('place_owners')
    .select('*, places(name, barrio), profiles(username)')
    .eq('verified', false)
    .order('created_at', { ascending: true })

  if (error) throw new Error(error.message)
  return (data ?? []) as ClaimRow[]
}

/** Approves or rejects a claim. If approved: marks verified. If rejected: deletes the row. */
export async function updateClaimStatus(
  id: string,
  approved: boolean,
  adminId: string,
): Promise<void> {
  const supabase = createAdminClient()
  if (approved) {
    // TS5.9 + @supabase/ssr incompatibility: .update() resolves to never[] — cast required
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const { error } = await (supabase as any)
      .from('place_owners')
      .update({
        verified: true,
        verified_at: new Date().toISOString(),
        approved_by: adminId,
      } as any)
      .eq('id', id) as { data: unknown; error: { message: string } | null }
    if (error) throw new Error(error.message)
  } else {
    const { error } = await supabase.from('place_owners').delete().eq('id', id)
    if (error) throw new Error(error.message)
  }
}

// ---------------------------------------------------------------------------
// Users
// ---------------------------------------------------------------------------

/** Returns all users from profiles, ordered by created_at desc. */
export async function getAllUsers(): Promise<ProfileRow[]> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .order('created_at', { ascending: false })

  if (error) throw new Error(error.message)
  return (data ?? []) as ProfileRow[]
}

/** Updates a user's role. */
export async function updateUserRole(
  id: string,
  role: 'user' | 'admin' | 'superadmin',
): Promise<void> {
  const supabase = createAdminClient()
  // TS5.9 + @supabase/ssr incompatibility: .update() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('profiles')
    .update({ role } as any)
    .eq('id', id) as { data: unknown; error: { message: string } | null }

  if (error) throw new Error(error.message)
}
