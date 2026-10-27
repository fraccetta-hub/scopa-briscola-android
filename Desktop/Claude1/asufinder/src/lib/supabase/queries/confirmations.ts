// ASUFinder — Confirmation queries
// All direct Supabase calls for the `confirmations` table go here.

import { createClient } from '../client'
import { SETTINGS } from '@/config/settings'

export async function getConfirmationCount(placeId: string): Promise<number> {
  const supabase = createClient()
  const { count, error } = await supabase
    .from('confirmations')
    .select('*', { count: 'exact', head: true })
    .eq('place_id', placeId)

  if (error) throw error
  return count ?? 0
}

export async function addConfirmation(placeId: string, userId: string): Promise<void> {
  const supabase = createClient()
  const { error } = await supabase.from('confirmations').insert({
    place_id: placeId,
    user_id: userId,
  })

  if (error) throw error
}

export async function getUserConfirmationsToday(userId: string): Promise<number> {
  const supabase = createClient()
  const startOfDay = new Date()
  startOfDay.setHours(0, 0, 0, 0)

  const { count, error } = await supabase
    .from('confirmations')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId)
    .gte('created_at', startOfDay.toISOString())

  if (error) throw error

  // Reference the limit from settings so callers can compare against it:
  // SETTINGS.confirmations.maxPerUserPerDay
  void SETTINGS.confirmations.maxPerUserPerDay

  return count ?? 0
}
