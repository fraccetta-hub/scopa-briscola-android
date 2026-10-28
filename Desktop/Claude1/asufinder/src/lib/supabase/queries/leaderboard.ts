import { createClient } from '@/lib/supabase/server'
import type { Profile } from '@/lib/supabase/types'

export async function getTopContributors(limit = 20): Promise<Profile[]> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .order('points', { ascending: false })
    .limit(limit)
  if (error) throw new Error(error.message)
  return data ?? []
}
