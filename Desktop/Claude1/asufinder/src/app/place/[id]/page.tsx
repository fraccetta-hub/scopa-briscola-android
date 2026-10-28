import { notFound } from 'next/navigation'
import { getPlace } from '@/lib/supabase/queries/places'
import { PlaceDetail } from '@/modules/places/PlaceDetail'

interface PlacePageProps {
  params: { id: string }
}

export default async function PlacePage({ params }: PlacePageProps) {
  const place = await getPlace(params.id).catch(() => null)

  if (!place) {
    notFound()
  }

  return <PlaceDetail place={place} />
}
