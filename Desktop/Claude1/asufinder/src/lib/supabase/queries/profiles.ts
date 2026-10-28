// ASUFinder — Profile queries
// All direct Supabase calls for the `profiles` table go here.

import { createClient } from '../server'
import { createAdminClient } from '../admin'
import type { Profile } from '../types'

export async function getProfile(userId: string): Promise<Profile> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single()

  if (error) throw error
  return data
}

export async function updatePoints(userId: string, delta: number): Promise<void> {
  const supabase = await createClient()

  // Read current points first (RPC increment would be cleaner with a live DB)
  const profile = await getProfile(userId)
  const newPoints = profile.points + delta

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('profiles')
    .update({ points: newPoints })
    .eq('id', userId) as { error: { message: string } | null }

  if (error) throw error
}

// Cross-user points update — uses service-role client to bypass profiles_update_own RLS.
// ONLY for server-side operations (denial deductions, admin adjustments).
export async function updatePointsAdmin(userId: string, delta: number): Promise<void> {
  const adminSupabase = createAdminClient()

  // TS5.9 + @supabase/ssr: .select() resolves to never — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: profile, error: fetchError } = await (adminSupabase as any)
    .from('profiles')
    .select('points')
    .eq('id', userId)
    .single() as { data: { points: number } | null; error: { message: string } | null }

  if (fetchError || !profile) throw new Error(fetchError?.message ?? 'Profile not found')

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (adminSupabase as any)
    .from('profiles')
    .update({ points: profile.points + delta })
    .eq('id', userId) as { error: { message: string } | null }

  if (error) throw new Error(error.message)
}
