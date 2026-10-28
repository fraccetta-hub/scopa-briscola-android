import { notFound } from 'next/navigation'
import { getPlace } from '@/lib/supabase/queries/places'
import { isAlreadyClaimed } from '@/lib/supabase/queries/claims'
import { createClient } from '@/lib/supabase/server'
import { PlaceDetail } from '@/modules/places/PlaceDetail'

interface PlacePageProps {
  params: { id: string }
}

export default async function PlacePage({ params }: PlacePageProps) {
  const place = await getPlace(params.id).catch(() => null)

  if (!place) {
    notFound()
  }

  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()
  const isClaimed = await isAlreadyClaimed(params.id).catch(() => false)

  return (
    <PlaceDetail
      place={place}
      isClaimed={isClaimed}
      userId={user?.id ?? null}
    />
  )
}
