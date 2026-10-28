import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import {
  addConfirmation,
  getConfirmationCount,
  getUserConfirmationsToday,
} from '@/lib/supabase/queries/confirmations'
import { getPlaceAdmin, updatePlaceAdmin } from '@/lib/supabase/queries/places'
import { updatePoints } from '@/lib/supabase/queries/profiles'
import { SETTINGS } from '@/config/settings'

export async function POST(
  _request: NextRequest,
  { params }: { params: { id: string } },
) {
  // 1. Auth check
  const supabase = await createClient()
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  // 2. Account age check (≥ 24h)
  const { data: { user: fullUser } } = await supabase.auth.getUser()
  const createdAt = fullUser?.created_at ? new Date(fullUser.created_at) : null
  if (!createdAt) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }
  const accountAgeMs = Date.now() - createdAt.getTime()
  const minAgeMs = SETTINGS.confirmations.minAccountAgeHours * 60 * 60 * 1000
  if (accountAgeMs < minAgeMs) {
    return NextResponse.json({ error: 'Account too new' }, { status: 403 })
  }

  const placeId = params.id

  // 3. Get place (admin client — pending places are invisible to RLS select_live)
  const place = await getPlaceAdmin(placeId)
  if (!place) {
    return NextResponse.json({ error: 'Not found' }, { status: 404 })
  }
  if (place.added_by === user.id) {
    return NextResponse.json({ error: 'Cannot confirm own place' }, { status: 403 })
  }

  // 4. Daily limit check
  const todayCount = await getUserConfirmationsToday(user.id)
  if (todayCount >= SETTINGS.confirmations.maxPerUserPerDay) {
    return NextResponse.json({ error: 'Daily limit reached' }, { status: 429 })
  }

  // 5. Add confirmation
  try {
    await addConfirmation(placeId, user.id)
  } catch (err: unknown) {
    const e = err as { code?: string }
    if (e?.code === '23505') {
      return NextResponse.json({ error: 'Already confirmed' }, { status: 409 })
    }
    throw err
  }

  // 6. Get count
  const count = await getConfirmationCount(placeId)

  // 7. Promote to live if threshold reached
  let placeStatus = place.status
  if (count >= SETTINGS.confirmations.required) {
    const updated = await updatePlaceAdmin(placeId, { status: 'live' })
    placeStatus = updated.status
  }

  // 8. Award points
  void updatePoints(user.id, 2)

  return NextResponse.json({ confirmed: true, count, placeStatus })
}
