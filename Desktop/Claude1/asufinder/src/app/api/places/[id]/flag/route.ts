import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { insertFlag, getFlagCount } from '@/lib/supabase/queries/flags'
import { updatePlace } from '@/lib/supabase/queries/places'
import { SETTINGS } from '@/config/settings'

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } },
) {
  // 1. Auth check
  const supabase = await createClient()
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const placeId = params.id
  let reason: string | undefined
  try {
    const body = await request.json() as { reason?: string }
    reason = body.reason
  } catch {
    // reason is optional
  }

  // 2. Insert flag
  await insertFlag(placeId, user.id, reason)

  // 3. Count flags
  const count = await getFlagCount(placeId)

  // 4. Update place status based on flag count
  if (count >= SETTINGS.flagging.closedThreshold) {
    await updatePlace(placeId, { status: 'closed' })
  } else if (count >= SETTINGS.flagging.needsReviewThreshold) {
    await updatePlace(placeId, { status: 'needs_review' })
  }

  return NextResponse.json({ flagged: true, count })
}
