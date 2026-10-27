// ASUFinder — Profile queries
// All direct Supabase calls for the `profiles` table go here.

import { createClient } from '../client'
import type { Profile } from '../types'

export async function getProfile(userId: string): Promise<Profile> {
  const supabase = createClient()
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single()

  if (error) throw error
  return data
}

export async function updatePoints(userId: string, delta: number): Promise<void> {
  const supabase = createClient()

  // Read current points first (RPC increment would be cleaner with a live DB)
  const profile = await getProfile(userId)
  const newPoints = profile.points + delta

  const { error } = await supabase
    .from('profiles')
    .update({ points: newPoints })
    .eq('id', userId)

  if (error) throw error
}
