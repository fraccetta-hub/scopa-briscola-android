import { searchPlaces } from '@/lib/supabase/queries/places'
import { ConfirmList } from '@/modules/confirmation/ConfirmList'
import type { Place } from '@/lib/supabase/types'

export default async function ConfirmPage() {
  let places: Place[] = []
  try {
    const all = await searchPlaces('*')
    places = all.filter((p) => p.status === 'pending')
  } catch {
    // fallback to empty list if Supabase not configured
    places = []
  }

  return (
    <main className="max-w-xl mx-auto p-4 space-y-4">
      <ConfirmList places={places} />
    </main>
  )
}
