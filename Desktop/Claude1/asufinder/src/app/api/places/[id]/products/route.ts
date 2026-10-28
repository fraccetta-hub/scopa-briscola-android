import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import {
  getPlaceTags,
  addTag,
  getUserTagsToday,
  getPlaceTagsThisWeek,
} from '@/lib/supabase/queries/products'
import { checkContent } from '@/modules/moderation/moderation.service'
import { updatePoints } from '@/lib/supabase/queries/profiles'
import { SETTINGS } from '@/config/settings'

export async function GET(
  _request: NextRequest,
  { params }: { params: { id: string } },
) {
  const tags = await getPlaceTags(params.id)
  return NextResponse.json(tags)
}

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

  // 2. Account age check
  const createdAt = user.created_at ? new Date(user.created_at) : null
  if (!createdAt) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }
  const ageMinutes = (Date.now() - createdAt.getTime()) / 60_000
  if (ageMinutes < SETTINGS.submissions.minAccountAgeMinutes) {
    return NextResponse.json({ error: 'AccountTooNew' }, { status: 403 })
  }

  const placeId = params.id

  // 3. Fetch place, check status='live'
  // TS5.9 + @supabase/ssr: .single() resolves to never — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: place, error: placeError } = await (supabase as any)
    .from('places')
    .select('id, status')
    .eq('id', placeId)
    .single() as { data: { id: string; status: string } | null; error: { message: string } | null }

  if (placeError || !place) {
    return NextResponse.json({ error: 'PlaceNotFound' }, { status: 404 })
  }
  if (place.status !== 'live') {
    return NextResponse.json({ error: 'PlaceNotLive' }, { status: 422 })
  }

  // 4. Rate limit user
  const userTagsToday = await getUserTagsToday(user.id)
  if (userTagsToday >= SETTINGS.submissions.maxTagsPerUserPerDay) {
    return NextResponse.json({ error: 'RateLimitExceeded' }, { status: 429 })
  }

  // 5. Rate limit place
  const placeTagsThisWeek = await getPlaceTagsThisWeek(placeId)
  if (placeTagsThisWeek >= SETTINGS.submissions.maxNewTagsPerPlacePerWeek) {
    return NextResponse.json({ error: 'PlaceRateLimitExceeded' }, { status: 429 })
  }

  // 6. Parse body
  let tag: string
  try {
    const parsed = await request.json() as { tag?: unknown }
    if (typeof parsed.tag !== 'string' || parsed.tag.trim().length === 0) {
      return NextResponse.json({ error: 'Tag required' }, { status: 400 })
    }
    tag = parsed.tag.trim().toLowerCase().slice(0, 50)
  } catch {
    return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 })
  }

  // 7. Moderation
  const mod = checkContent(tag)
  if (!mod.passed) {
    return NextResponse.json({ error: 'Blocked' }, { status: 422 })
  }

  // 8. Check duplicate
  const { data: existing } = await supabase
    .from('place_products')
    .select('id')
    .eq('place_id', placeId)
    .eq('tag', tag)
    .maybeSingle()

  if (existing) {
    return NextResponse.json({ error: 'DuplicateTag' }, { status: 409 })
  }

  // 9. Detect isOwner
  const { data: claim } = await supabase
    .from('place_owners')
    .select('id')
    .eq('place_id', placeId)
    .eq('user_id', user.id)
    .eq('verified', true)
    .maybeSingle()

  const isOwner = claim !== null

  // 10. Insert tag
  const product = await addTag(placeId, tag, user.id, isOwner)

  // Award points (fire-and-forget)
  void updatePoints(user.id, SETTINGS.points.addTag)

  return NextResponse.json(product, { status: 201 })
}
