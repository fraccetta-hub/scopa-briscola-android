// ASUFinder — Claim queries (owner verification)

import { createClient } from '../server'

// In-memory OTP store — intentionally ephemeral per spec ("in-memory Map for now").
// Acceptable for MVP single-instance deploys; replace with Supabase table before multi-instance.
// Key: placeId, Value: { userId, otp, expiresAt }
const otpStore = new Map<string, { userId: string; otp: string; expiresAt: number }>()

export function storeOTP(placeId: string, userId: string, otp: string): void {
  const expiresAt = Date.now() + 10 * 60 * 1000 // 10 minutes
  otpStore.set(placeId, { userId, otp, expiresAt })
}

export function getStoredOTP(
  placeId: string,
): { userId: string; otp: string; expiresAt: number } | undefined {
  return otpStore.get(placeId)
}

export function deleteOTP(placeId: string): void {
  otpStore.delete(placeId)
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
