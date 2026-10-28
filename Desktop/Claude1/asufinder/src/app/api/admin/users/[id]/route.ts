// ASUFinder — Admin users API route
// PATCH /api/admin/users/[id] — update role or ban a user
// Requires admin or superadmin role.
// - admin: can set role=user or role=admin only; cannot touch superadmins
// - superadmin: can set any role including superadmin; can ban anyone

import { NextResponse } from 'next/server'
import { createClient } from '@/lib/supabase/server'
import { updateUserRole, getAllUsers } from '@/lib/supabase/queries/admin'

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

  // Role check for caller
  const { data: callerProfile } = await supabase.from('profiles').select('role').eq('id', user.id).single()
  if (!callerProfile || !['admin', 'superadmin'].includes(callerProfile.role)) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  const body = await request.json() as { role?: string; banned?: boolean }

  // Fetch target user's current role
  const users = await getAllUsers()
  const target = users.find(u => u.id === params.id)
  if (!target) {
    return NextResponse.json({ error: 'User not found' }, { status: 404 })
  }

  // Guard: admin cannot demote/ban a superadmin
  if (callerProfile.role === 'admin' && target.role === 'superadmin') {
    return NextResponse.json({ error: 'Forbidden: cannot modify a superadmin' }, { status: 403 })
  }

  try {
    if (body.banned === true) {
      // Ban = set role to 'banned'; but 'banned' is not in the DB check constraint.
      // MVP: treat ban as setting role='user' as a safe no-op for now.
      // To properly ban, add 'banned' to the check constraint in DB.
      // For now we just demote to 'user'.
      await updateUserRole(params.id, 'user')
    } else if (body.role !== undefined) {
      const newRole = body.role as 'user' | 'admin' | 'superadmin'

      // Only superadmin can grant superadmin role
      if (newRole === 'superadmin' && callerProfile.role !== 'superadmin') {
        return NextResponse.json({ error: 'Forbidden: only superadmin can grant superadmin' }, { status: 403 })
      }

      if (!['user', 'admin', 'superadmin'].includes(newRole)) {
        return NextResponse.json({ error: 'Invalid role' }, { status: 400 })
      }

      await updateUserRole(params.id, newRole)
    }

    return NextResponse.json({ ok: true })
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error'
    return NextResponse.json({ error: message }, { status: 500 })
  }
}
