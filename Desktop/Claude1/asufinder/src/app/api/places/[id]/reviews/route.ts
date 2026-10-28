import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import {
  getReviews,
  getUserReviewsToday,
  getUserReviewForPlace,
  insertReview,
} from '@/lib/supabase/queries/reviews'
import { updatePoints } from '@/lib/supabase/queries/profiles'
import { SETTINGS } from '@/config/settings'

export async function GET(
  _request: NextRequest,
  { params }: { params: { id: string } },
) {
  const reviews = await getReviews(params.id)
  return NextResponse.json(reviews)
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

  // 2. Account age check (≥ minAccountAgeMinutes)
  const createdAt = user.created_at ? new Date(user.created_at) : null
  if (!createdAt) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }
  const ageMinutes = (Date.now() - createdAt.getTime()) / 60_000
  if (ageMinutes < SETTINGS.submissions.minAccountAgeMinutes) {
    return NextResponse.json({ error: 'AccountTooNew' }, { status: 403 })
  }

  const placeId = params.id

  // 3. Duplicate check — one review per user per place
  const existing = await getUserReviewForPlace(placeId, user.id)
  if (existing) {
    return NextResponse.json({ error: 'AlreadyReviewed' }, { status: 409 })
  }

  // 4. Daily rate limit
  const todayCount = await getUserReviewsToday(user.id)
  if (todayCount >= SETTINGS.submissions.maxReviewsPerUserPerDay) {
    return NextResponse.json({ error: 'RateLimitExceeded' }, { status: 429 })
  }

  // 5. Parse and validate body
  let body: string
  try {
    const parsed = await request.json() as { body?: unknown }
    if (typeof parsed.body !== 'string' || parsed.body.trim().length === 0) {
      return NextResponse.json({ error: 'Body required' }, { status: 400 })
    }
    if (parsed.body.trim().length > 1000) {
      return NextResponse.json({ error: 'Body too long' }, { status: 400 })
    }
    body = parsed.body.trim()
  } catch {
    return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 })
  }

  // 6. Insert review
  const review = await insertReview(placeId, user.id, body)

  // 7. Award points (fire-and-forget)
  void updatePoints(user.id, SETTINGS.points.writeReview)

  return NextResponse.json(review, { status: 201 })
}
