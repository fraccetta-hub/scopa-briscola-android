import { getTopContributors } from '@/lib/supabase/queries/leaderboard'
import { Leaderboard } from '@/modules/leaderboard/Leaderboard'

export default async function LeaderboardPage() {
  const profiles = await getTopContributors(20)
  return (
    <main style={{ backgroundColor: 'var(--color-bg)', minHeight: '100vh' }}>
      <Leaderboard profiles={profiles} />
    </main>
  )
}
