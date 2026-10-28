// ASUFinder — Claim queries (owner verification)

import { createHash } from 'crypto'
import { createAdminClient } from '../admin'
import { createClient } from '../server'

function hashOTP(otp: string): string {
  return createHash('sha256').update(otp).digest('hex')
}

/**
 * Store an OTP for a place/user in the otp_tokens table.
 * Deletes any prior unused tokens for the same place+user before inserting.
 */
export async function storeOTP(placeId: string, userId: string, otp: string): Promise<void> {
  const supabase = createAdminClient()
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString()
  const codeHash = hashOTP(otp)

  // Clean up any existing unused tokens for this place+user
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  await (supabase as any)
    .from('otp_tokens')
    .delete()
    .eq('place_id', placeId)
    .eq('user_id', userId)
    .eq('used', false)

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('otp_tokens')
    .insert({ place_id: placeId, user_id: userId, code: codeHash, expires_at: expiresAt } as any)

  if (error) throw new Error((error as { message: string }).message)
}

/**
 * Check if a recent (< 60s old) unused OTP exists for this place+user.
 * Returns true if the caller should rate-limit (too many requests).
 */
export async function hasRecentOTP(placeId: string, userId: string): Promise<boolean> {
  const supabase = createAdminClient()
  const sixtySecondsAgo = new Date(Date.now() - 60 * 1000).toISOString()

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data } = await (supabase as any)
    .from('otp_tokens')
    .select('id')
    .eq('place_id', placeId)
    .eq('user_id', userId)
    .eq('used', false)
    .gte('created_at', sixtySecondsAgo)
    .limit(1) as { data: { id: string }[] | null }

  return Array.isArray(data) && data.length > 0
}

/**
 * Verify an OTP code against the DB. Returns true if valid and marks it used.
 */
export async function verifyAndConsumeOTP(
  placeId: string,
  userId: string,
  otp: string,
): Promise<boolean> {
  const supabase = createAdminClient()
  const now = new Date().toISOString()
  const codeHash = hashOTP(otp)

  // First find matching token (need code hash to compare before marking used)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: tokens, error: selectError } = await (supabase as any)
    .from('otp_tokens')
    .select('id, code')
    .eq('place_id', placeId)
    .eq('user_id', userId)
    .eq('used', false)
    .gte('expires_at', now)
    .order('created_at', { ascending: false })
    .limit(1) as { data: { id: string; code: string }[] | null; error: { message: string } | null }

  if (selectError) throw new Error(selectError.message)
  if (!tokens || tokens.length === 0) return false
  if (tokens[0].code !== codeHash) return false

  // Atomic mark-used: UPDATE WHERE used=false prevents replay if two requests race
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: updated, error: updateError } = await (supabase as any)
    .from('otp_tokens')
    .update({ used: true } as any)
    .eq('id', tokens[0].id)
    .eq('used', false)
    .select('id') as { data: { id: string }[] | null; error: { message: string } | null }

  if (updateError) throw new Error(updateError.message)
  // If 0 rows updated, another request already consumed this token
  return !!(updated && updated.length > 0)
}

export async function claimPlace(placeId: string, userId: string): Promise<void> {
  const supabase = await createClient()
  // TS5.9 + @supabase/ssr incompatibility: .insert() parameter resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { error } = await (supabase as any)
    .from('place_owners')
    .insert({ place_id: placeId, user_id: userId, verified: true, verified_at: new Date().toISOString() } as any)
    .select()
    .single() as { data: unknown; error: { message: string } | null }

  if (error) throw new Error(error.message)
}

export async function isAlreadyClaimed(placeId: string): Promise<boolean> {
  const supabase = await createClient()
  const { data, error } = await supabase
    .from('place_owners')
    .select('id')
    .eq('place_id', placeId)
    .maybeSingle()

  if (error) throw new Error(error.message)
  return data !== null
}
