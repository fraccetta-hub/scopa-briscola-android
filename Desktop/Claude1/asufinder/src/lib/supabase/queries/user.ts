import { createClient } from '@/lib/supabase/server'
import type { Place, Profile } from '@/lib/supabase/types'

export async function getUserPlaces(userId: string): Promise<Place[]> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('places')
    .select('*')
    .eq('added_by', userId)
    .order('created_at', { ascending: false })
  if (error) throw new Error(error.message)
  return data ?? []
}

export async function getUserProfile(userId: string): Promise<Profile | null> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single()
  if (error) return null
  return data
}
