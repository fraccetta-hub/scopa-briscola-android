// ASUFinder — Admin places API route
// PATCH /api/admin/places/[id] — update status or featured flag
// Requires admin or superadmin role.

import { NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { updatePlaceStatus, toggleFeatured } from '@/lib/supabase/queries/admin'
import type { PlaceStatus } from '@/lib/supabase/queries/admin'

const VALID_STATUSES: PlaceStatus[] = ['pending', 'live', 'needs_review', 'closed', 'rejected']

export async function PATCH(
  request: Request,
  { params }: { params: { id: string } },
) {
  // Auth check (standard client — anon key + cookies)
  const supabase = await createClient()
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  // Role check
  const { data: profile } = await supabase.from('profiles').select('role').eq('id', user.id).single()
  if (!profile || !['admin', 'superadmin'].includes(profile.role)) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  const body = await request.json() as {
    status?: string
    is_featured?: boolean
    featured_until?: string
  }

  try {
    if (body.status !== undefined) {
      if (!VALID_STATUSES.includes(body.status as PlaceStatus)) {
        return NextResponse.json({ error: 'Invalid status' }, { status: 400 })
      }
      await updatePlaceStatus(params.id, body.status as PlaceStatus)
    }

    if (body.is_featured !== undefined) {
      await toggleFeatured(params.id, body.is_featured, body.featured_until)
    }

    return NextResponse.json({ ok: true })
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error'
    return NextResponse.json({ error: message }, { status: 500 })
  }
}
