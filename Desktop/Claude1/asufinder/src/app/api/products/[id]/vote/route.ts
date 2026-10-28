import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import {
  voteTag,
  getUserVoteForTag,
} from '@/lib/supabase/queries/products'
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

  // 2. Account age check
  const createdAt = user.created_at ? new Date(user.created_at) : null
  if (!createdAt) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }
  const ageMinutes = (Date.now() - createdAt.getTime()) / 60_000
  if (ageMinutes < SETTINGS.submissions.minAccountAgeMinutes) {
    return NextResponse.json({ error: 'AccountTooNew' }, { status: 403 })
  }

  const placeProductId = params.id

  // 3. Parse body
  let type: 'confirm' | 'deny'
  try {
    const parsed = await request.json() as { type?: unknown }
    if (parsed.type !== 'confirm' && parsed.type !== 'deny') {
      return NextResponse.json({ error: 'Invalid type' }, { status: 400 })
    }
    type = parsed.type
  } catch {
    return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 })
  }

  // 4. Fetch product tag
  // TS5.9 + @supabase/ssr: .single() resolves to never — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: tag, error: tagError } = await (supabase as any)
    .from('place_products')
    .select('*')
    .eq('id', placeProductId)
    .single() as { data: { id: string; place_id: string; status: string } | null; error: { message: string } | null }

  if (tagError || !tag) {
    return NextResponse.json({ error: 'TagNotFound' }, { status: 404 })
  }
  if (tag.status === 'denied' || tag.status === 'expired') {
    return NextResponse.json({ error: 'TagNotVotable' }, { status: 422 })
  }

  // 5. Check already voted
  const existingVote = await getUserVoteForTag(placeProductId, user.id)
  if (existingVote) {
    return NextResponse.json({ error: 'AlreadyVoted' }, { status: 409 })
  }

  // 6. Detect isOwner
  const { data: claim } = await supabase
    .from('place_owners')
    .select('id')
    .eq('place_id', tag.place_id)
    .eq('user_id', user.id)
    .eq('verified', true)
    .maybeSingle()

  const isOwner = claim !== null

  // 7. Cast vote
  await voteTag(placeProductId, user.id, type, isOwner)

  return NextResponse.json({ ok: true })
}
