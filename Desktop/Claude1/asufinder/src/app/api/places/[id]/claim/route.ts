import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { getPlace } from '@/lib/supabase/queries/places'
import { storeOTP, isAlreadyClaimed } from '@/lib/supabase/queries/claims'
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

  // 2. Get place
  const place = await getPlace(placeId)
  if (!place) {
    return NextResponse.json({ error: 'Not found' }, { status: 404 })
  }

  // 3. Check already claimed
  const claimed = await isAlreadyClaimed(placeId)
  if (claimed) {
    return NextResponse.json({ error: 'Already claimed' }, { status: 409 })
  }

  // 4. Require WhatsApp
  if (!place.whatsapp) {
    return NextResponse.json({ error: 'no_whatsapp' }, { status: 422 })
  }

  // 5. Generate and store OTP
  const otp = generateOTP()
  storeOTP(placeId, user.id, otp)

  // 6. Send OTP via WhatsApp
  await sendOTP(place.whatsapp, otp)

  return NextResponse.json({ message: 'OTP sent' })
}
