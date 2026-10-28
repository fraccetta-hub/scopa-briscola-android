'use client'

import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { SearchBox } from '@/modules/search/SearchBox'
import { CategoryPills } from '@/modules/search/CategoryPills'

export default function HomePage() {
  const router = useRouter()
  const t = useTranslations('nav')

  function handleSearch(query: string) {
    router.push('/search?q=' + encodeURIComponent(query))
  }

  function handleCategorySelect(slug: string | undefined) {
    if (slug) {
      router.push('/search?category=' + encodeURIComponent(slug))
    }
  }

  return (
    <main>
      {/* Hero */}
      <section
        className="w-full flex flex-col items-center justify-center py-16 px-4 gap-8"
        style={{
          background: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%)',
        }}
      >
        <h1
          className="text-3xl md:text-5xl font-bold text-center"
          style={{ color: 'var(--color-white)' }}
        >
          ASUFinder
        </h1>
        <p
          className="text-lg md:text-xl text-center max-w-xl"
          style={{ color: 'var(--color-mint)', opacity: 0.9 }}
        >
          {t('tagline')}
        </p>
        <SearchBox onSearch={handleSearch} />
      </section>

      {/* Categories */}
      <section className="py-8" style={{ backgroundColor: 'var(--color-bg)' }}>
        <CategoryPills onSelect={handleCategorySelect} />
      </section>
    </main>
  )
}
