import { redirect } from 'next/navigation'
import { createClient } from '@/lib/supabase/server'
import { getUserPlaces, getUserProfile } from '@/lib/supabase/queries/user'
import { UserDashboard } from '@/modules/user/UserDashboard'

export default async function DashboardPage() {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()

  if (!user) {
    redirect('/login')
  }

  const [places, profile] = await Promise.all([
    getUserPlaces(user.id),
    getUserProfile(user.id),
  ])

  return <UserDashboard user={user} places={places} profile={profile} />
}
