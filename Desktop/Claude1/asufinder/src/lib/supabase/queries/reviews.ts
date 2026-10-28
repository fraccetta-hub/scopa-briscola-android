// ASUFinder — Review queries
// All direct Supabase calls for the `reviews` table go here.

import { createClient } from '../client'
import type { Database } from '../types'

type ReviewRow = Database['public']['Tables']['reviews']['Row']
type ReviewInsert = Database['public']['Tables']['reviews']['Insert']

export type { ReviewRow }

export async function getReviews(placeId: string): Promise<ReviewRow[]> {
  const supabase = createClient()
  const { data, error } = await supabase
    .from('reviews')
    .select('*')
    .eq('place_id', placeId)
    .order('created_at', { ascending: false })

  if (error) throw error
  return data ?? []
}

export async function getUserReviewsToday(userId: string): Promise<number> {
  const supabase = createClient()
  const startOfDay = new Date()
  startOfDay.setUTCHours(0, 0, 0, 0)

  const { count, error } = await supabase
    .from('reviews')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId)
    .gte('created_at', startOfDay.toISOString())

  if (error) throw error
  return count ?? 0
}

export async function getUserReviewForPlace(
  placeId: string,
  userId: string,
): Promise<ReviewRow | null> {
  const supabase = createClient()
  const { data, error } = await supabase
    .from('reviews')
    .select('*')
    .eq('place_id', placeId)
    .eq('user_id', userId)
    .maybeSingle()

  if (error) throw error
  return data
}

export async function insertReview(
  placeId: string,
  userId: string,
  body: string,
): Promise<ReviewRow> {
  const supabase = createClient()
  const row: ReviewInsert = { place_id: placeId, user_id: userId, body }
  // TS5.9 + @supabase/ssr: .insert() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, error } = await (supabase as any)
    .from('reviews')
    .insert(row as any)
    .select()
    .single() as { data: ReviewRow | null; error: { message: string } | null }

  if (error) throw error
  if (!data) throw new Error('Insert returned no data')
  return data
}
