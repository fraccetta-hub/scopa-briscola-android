'use client'

import { useLocale } from 'next-intl'
import { CATEGORIES } from '@/config/categories'

interface CategoryPillsProps {
  selected?: string
  onSelect: (slug: string | undefined) => void
}

export function CategoryPills({ selected, onSelect }: CategoryPillsProps) {
  const locale = useLocale() as 'es' | 'en'

  function handleClick(slug: string) {
    onSelect(selected === slug ? undefined : slug)
  }

  return (
    <div className="flex flex-wrap gap-2 justify-center px-4">
      {CATEGORIES.map((cat) => {
        const isSelected = selected === cat.slug
        return (
          <button
            key={cat.slug}
            onClick={() => handleClick(cat.slug)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
            style={
              isSelected
                ? {
                    backgroundColor: 'var(--color-primary)',
                    color: 'var(--color-white)',
                    border: '1.5px solid var(--color-primary)',
                  }
                : {
                    backgroundColor: 'var(--color-surface)',
                    color: 'var(--color-navy)',
                    border: '1.5px solid color-mix(in srgb, var(--color-navy) 20%, transparent)',
                  }
            }
            aria-pressed={isSelected}
          >
            <span aria-hidden="true">{cat.icon}</span>
            <span>{cat.label[locale] ?? cat.label.es}</span>
          </button>
        )
      })}
    </div>
  )
}
