// ASUFinder — Admin claims API route
// PATCH /api/admin/claims/[id] — approve or reject a claim
// Requires admin or superadmin role.

import { NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { updateClaimStatus } from '@/lib/supabase/queries/admin'

export async function PATCH(
  request: Request,
  { params }: { params: { id: string } },
) {
  // Auth check
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

  const body = await request.json() as { approved: boolean }

  if (typeof body.approved !== 'boolean') {
    return NextResponse.json({ error: 'approved must be a boolean' }, { status: 400 })
  }

  try {
    await updateClaimStatus(params.id, body.approved, user.id)
    return NextResponse.json({ ok: true })
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error'
    return NextResponse.json({ error: message }, { status: 500 })
  }
}
