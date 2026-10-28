// ASUFinder — Admin users page (server component)

import { getAllUsers } from '@/lib/supabase/queries/admin'
import { UsersList } from '@/modules/admin/UsersList'

export default async function UsersPage() {
  const users = await getAllUsers()
  return <UsersList users={users} />
}
