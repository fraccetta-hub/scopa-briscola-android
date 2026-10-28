import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { getPlace } from '@/lib/supabase/queries/places'
import { storeOTP, isAlreadyClaimed, hasRecentOTP } from '@/lib/supabase/queries/claims'
import { generateOTP, sendOTP } from '@/lib/whatsapp/otp'

export async function POST(
  _request: NextRequest,
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

  const placeId = params.id

  // 2. Rate limit — one OTP per 60 seconds per (place, user)
  const recentExists = await hasRecentOTP(placeId, user.id)
  if (recentExists) {
    return NextResponse.json(
      { error: 'Too many requests' },
      { status: 429, headers: { 'Retry-After': '60' } },
    )
  }

  // 3. Get place
  const place = await getPlace(placeId)
  if (!place) {
    return NextResponse.json({ error: 'Not found' }, { status: 404 })
  }

  // 4. Check already claimed
  const claimed = await isAlreadyClaimed(placeId)
  if (claimed) {
    return NextResponse.json({ error: 'Already claimed' }, { status: 409 })
  }

  // 5. Require WhatsApp
  if (!place.whatsapp) {
    return NextResponse.json({ error: 'no_whatsapp' }, { status: 422 })
  }

  // 6. Generate, hash-store and send OTP
  const otp = generateOTP()
  await storeOTP(placeId, user.id, otp)

  // 7. Send OTP via WhatsApp
  await sendOTP(place.whatsapp, otp)

  return NextResponse.json({ message: 'OTP sent' })
}
