// ASUFinder — Flag queries
import { createClient } from '../client'
import type { Database } from '../types'

type FlagInsert = Database['public']['Tables']['flags']['Insert']

export async function insertFlag(placeId: string, userId: string, reason?: string): Promise<void> {
  const supabase = createClient()
  const row: FlagInsert = { place_id: placeId, user_id: userId, reason: reason ?? null }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await supabase.from('flags').insert(row as any)
  if (error) throw error
}

export async function getFlagCount(placeId: string): Promise<number> {
  const supabase = createClient()
  const { count, error } = await supabase
    .from('flags')
    .select('*', { count: 'exact', head: true })
    .eq('place_id', placeId)

  if (error) throw error
  return count ?? 0
}
