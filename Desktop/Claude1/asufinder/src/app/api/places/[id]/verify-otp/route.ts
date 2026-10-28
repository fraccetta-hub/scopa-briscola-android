import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { verifyAndConsumeOTP, claimPlace } from '@/lib/supabase/queries/claims'

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

  // 3. Verify OTP against DB (checks expiry, hash, marks used)
  const valid = await verifyAndConsumeOTP(placeId, user.id, otp)
  if (!valid) {
    return NextResponse.json({ error: 'Invalid or expired OTP' }, { status: 400 })
  }

  // 4. Claim place
  await claimPlace(placeId, user.id)

  return NextResponse.json({ claimed: true })
}
