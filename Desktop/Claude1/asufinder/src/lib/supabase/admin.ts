// ASUFinder — Admin Supabase client (service-role, bypasses RLS)
// CRITICAL: Use ONLY in server-side admin API routes — never in components or client code.
// SUPABASE_SERVICE_ROLE_KEY must NEVER be committed to git.

import { createClient as createSupabaseClient } from '@supabase/supabase-js'
import type { Database } from './types'

export function createAdminClient() {
  return createSupabaseClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!, // bypasses RLS — admin use only
  )
}
