// ASUFinder — Admin pending places page (server component)

import { getPendingPlaces } from '@/lib/supabase/queries/admin'
import { PendingList } from '@/modules/admin/PendingList'

export default async function PendingPage() {
  const places = await getPendingPlaces()
  return <PendingList places={places} />
}
