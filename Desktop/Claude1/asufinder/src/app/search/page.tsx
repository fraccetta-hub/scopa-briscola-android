import { getTranslations } from 'next-intl/server'
import { searchPlaces } from '@/lib/supabase/queries/places'
import { SearchResults } from '@/modules/search/SearchResults'

interface SearchPageProps {
  searchParams: { q?: string; category?: string }
}

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const t = await getTranslations('search')
  const q = searchParams.q ?? ''
  const category = searchParams.category

  const places = await searchPlaces(q || '*', category).catch(() => [])

  return (
    <main className="max-w-4xl mx-auto px-4 py-8">
      <p className="text-sm mb-6" style={{ color: 'var(--color-navy)', opacity: 0.6 }}>
        {t('resultsCount', { count: places.length })}
      </p>
      <SearchResults places={places} />
    </main>
  )
}
