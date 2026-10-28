// ASUFinder — Product tag queries
// All direct Supabase calls for place_products and product_votes tables go here.

import { createClient } from '../client'
import { createAdminClient } from '../admin'
import type { Database, Place } from '../types'

export type PlaceProduct = Database['public']['Tables']['place_products']['Row']
export type ProductVote = Database['public']['Tables']['product_votes']['Row']

export async function getPlaceTags(placeId: string): Promise<PlaceProduct[]> {
  const supabase = createClient()
  const { data, error } = await supabase
    .from('place_products')
    .select('*')
    .eq('place_id', placeId)
    .in('status', ['confirmed', 'uncertain'])
    .order('created_at', { ascending: false })

  if (error) throw error
  return data ?? []
}

export async function addTag(
  placeId: string,
  tag: string,
  userId: string,
  isOwner: boolean,
): Promise<PlaceProduct> {
  const supabase = createClient()
  const status = isOwner ? 'confirmed' : 'pending'
  const row = { place_id: placeId, tag, added_by: userId, status }

  // TS5.9 + @supabase/ssr: .insert() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, error } = await (supabase as any)
    .from('place_products')
    .insert(row as any)
    .select()
    .single() as { data: PlaceProduct | null; error: { message: string } | null }

  if (error) throw new Error(error.message)
  if (!data) throw new Error('Insert returned no data')
  return data
}

export async function voteTag(
  placeProductId: string,
  userId: string,
  type: 'confirm' | 'deny',
  isOwner: boolean,
): Promise<void> {
  const supabase = createClient()

  // Insert vote
  const voteRow = { place_product_id: placeProductId, user_id: userId, type }
  // TS5.9 + @supabase/ssr: .insert() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error: voteError } = await (supabase as any)
    .from('product_votes')
    .insert(voteRow as any)
    .select()
    .single() as { data: ProductVote | null; error: { message: string } | null }

  if (voteError) throw new Error(voteError.message)

  // Fetch current tag state
  // TS5.9 + @supabase/ssr: .single() resolves to never — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: tag, error: fetchError } = await (supabase as any)
    .from('place_products')
    .select('*')
    .eq('id', placeProductId)
    .single() as { data: PlaceProduct | null; error: { message: string } | null }

  if (fetchError) throw new Error(fetchError.message)
  if (!tag) throw new Error('Tag not found')

  let patch: Partial<PlaceProduct> = {}

  if (type === 'confirm') {
    const newCount = (tag.confirmed_count ?? 0) + 1
    patch = { confirmed_count: newCount }
    if (newCount >= 2 && tag.status === 'pending') {
      const expiresAt = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString()
      patch = { ...patch, status: 'confirmed', expires_at: expiresAt }
    }
  } else {
    const newCount = (tag.denied_count ?? 0) + 1
    patch = { denied_count: newCount }
    if (newCount >= 3 || isOwner) {
      patch = { ...patch, status: 'denied' }
      // Deduct 2 points from added_by (fire-and-forget)
      if (tag.added_by) {
        const { updatePoints } = await import('./profiles')
        void updatePoints(tag.added_by, -2)
      }
    }
  }

  // TS5.9 + @supabase/ssr: .update() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error: updateError } = await (supabase as any)
    .from('place_products')
    .update(patch as any)
    .eq('id', placeProductId)
    .select()
    .single() as { data: PlaceProduct | null; error: { message: string } | null }

  if (updateError) throw new Error(updateError.message)
}

export async function getUserTagsToday(userId: string): Promise<number> {
  const supabase = createClient()
  const startOfDay = new Date()
  startOfDay.setUTCHours(0, 0, 0, 0)

  const { count, error } = await supabase
    .from('place_products')
    .select('*', { count: 'exact', head: true })
    .eq('added_by', userId)
    .gte('created_at', startOfDay.toISOString())

  if (error) throw error
  return count ?? 0
}

export async function getPlaceTagsThisWeek(placeId: string): Promise<number> {
  const supabase = createClient()
  const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()

  const { count, error } = await supabase
    .from('place_products')
    .select('*', { count: 'exact', head: true })
    .eq('place_id', placeId)
    .gte('created_at', weekAgo)

  if (error) throw error
  return count ?? 0
}

export async function getUserVoteForTag(
  placeProductId: string,
  userId: string,
): Promise<ProductVote | null> {
  const supabase = createClient()
  const { data, error } = await supabase
    .from('product_votes')
    .select('*')
    .eq('place_product_id', placeProductId)
    .eq('user_id', userId)
    .maybeSingle()

  if (error) throw error
  return data
}

export async function searchByTag(tag: string, limit = 20): Promise<Place[]> {
  const supabase = createAdminClient()
  const { data, error } = await supabase
    .from('place_products')
    .select('places(*)')
    .eq('status', 'confirmed')
    .ilike('tag', '%' + tag + '%')
    .limit(limit)

  if (error) throw error

  // Deduplicate by place id
  const seen = new Set<string>()
  const places: Place[] = []
  for (const row of data ?? []) {
    const place = (row as unknown as { places: Place | null }).places
    if (place && !seen.has(place.id)) {
      seen.add(place.id)
      places.push(place)
    }
  }
  return places
}
