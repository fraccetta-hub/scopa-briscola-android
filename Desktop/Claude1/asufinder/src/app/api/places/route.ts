import { NextRequest, NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { checkContent } from '@/modules/moderation/moderation.service'
import { checkDuplicates, insertPlace } from '@/lib/supabase/queries/places'
import { updatePoints } from '@/lib/supabase/queries/profiles'

export async function POST(request: NextRequest) {
  // 1. Auth check
  const supabase = await createClient()
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  // 2. Parse body
  let body: {
    name?: string
    categories?: string[]
    barrio?: string
    address?: string
    phone?: string
    whatsapp?: string
    website?: string
    maps_url?: string
    hours?: string
    products?: string
    force?: boolean
  }
  try {
    body = await request.json() as typeof body
  } catch {
    return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 })
  }

  const { name, categories, barrio, address, phone, whatsapp, website, maps_url, hours, force } = body

  if (!name || !barrio || !categories || categories.length === 0) {
    return NextResponse.json({ error: 'Missing required fields' }, { status: 400 })
  }

  // 3. Content moderation
  const modResult = checkContent(`${name} ${address ?? ''}`)
  if (!modResult.passed) {
    return NextResponse.json({ error: `Content blocked: ${modResult.reason}` }, { status: 400 })
  }

  // 4. Duplicate check (skip if force=true)
  if (!force) {
    const dupes = await checkDuplicates(name, barrio)
    if (dupes.length > 0) {
      return NextResponse.json({ matches: dupes }, { status: 409 })
    }
  }

  // 5. Determine status
  const status: 'live' | 'pending' = maps_url ? 'live' : 'pending'

  // 6. Insert place
  const place = await insertPlace({
    name,
    categories: categories ?? [],
    barrio,
    address: address || null,
    phone: phone || null,
    whatsapp: whatsapp || null,
    website: website || null,
    maps_url: maps_url || null,
    hours: hours || null,
    status,
    source: 'user',
    added_by: user.id,
  })

  // 7. Award points if live
  if (status === 'live') {
    void updatePoints(user.id, 5)
  }

  // 8. Return created place
  return NextResponse.json(place, { status: 201 })
}
