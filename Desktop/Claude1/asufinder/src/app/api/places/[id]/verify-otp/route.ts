import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { getStoredOTP, claimPlace, deleteOTP } from '@/lib/supabase/queries/claims'
import { verifyOTP } from '@/lib/whatsapp/otp'

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } },
) {
  // 1. Auth check
  const supabase = await createClient()
  const {
    data: { user },
    error: authError,
  } = await supabase.auth.getUser()
  if (authError || !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  // 2. Parse body
  const body = await request.json() as { otp?: string }
  const { otp } = body

  if (!otp || typeof otp !== 'string') {
    return NextResponse.json({ error: 'Missing otp' }, { status: 400 })
  }

  const placeId = params.id

  // 3. Get stored OTP
  const stored = getStoredOTP(placeId)
  if (!stored) {
    return NextResponse.json({ error: 'No OTP found' }, { status: 404 })
  }

  // 4. Check user matches before revealing OTP validity (prevents timing oracle)
  if (stored.userId !== user.id) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  // 5. Verify OTP
  const valid = verifyOTP(stored, otp)
  if (!valid) {
    return NextResponse.json({ error: 'Invalid or expired OTP' }, { status: 400 })
  }

  // 6. Delete OTP to prevent replay attacks, then claim place
  deleteOTP(placeId)
  await claimPlace(placeId, user.id)

  return NextResponse.json({ claimed: true })
}
