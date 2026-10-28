// ASUFinder — Admin users page (server component)

import { redirect } from 'next/navigation'
import { createClient } from '@/lib/supabase/server'
import { getAllUsers } from '@/lib/supabase/queries/admin'
import { UsersList } from '@/modules/admin/UsersList'

export default async function UsersPage() {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) redirect('/')

  const { data: profile } = await supabase
    .from('profiles')
    .select('role')
    .eq('id', user.id)
    .single()

  if (!profile || !['admin', 'superadmin'].includes(profile.role)) redirect('/')
  const callerRole = (profile.role === 'superadmin' ? 'superadmin' : 'admin') as 'admin' | 'superadmin'
  const users = await getAllUsers()
  return <UsersList users={users} callerRole={callerRole} />
}
