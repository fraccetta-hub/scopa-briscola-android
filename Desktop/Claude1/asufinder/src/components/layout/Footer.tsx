'use client'

import Link from 'next/link'
import { useTranslations } from 'next-intl'

export function Footer() {
  const t = useTranslations('nav')

  return (
    <footer
      className="mt-auto py-8 px-4 border-t"
      style={{
        backgroundColor: 'var(--color-surface)',
        borderColor: 'var(--color-mint)',
      }}
    >
      <div
        className="max-w-4xl mx-auto flex flex-wrap items-center justify-between gap-4 text-sm"
        style={{ color: 'var(--color-navy)' }}
      >
        <span style={{ fontWeight: 600 }}>ASUFinder</span>
        <nav className="flex flex-wrap gap-4" aria-label="Footer">
          <Link href="/" style={{ color: 'var(--color-navy)', opacity: 0.75 }}>
            {t('home')}
          </Link>
          <Link href="/search" style={{ color: 'var(--color-navy)', opacity: 0.75 }}>
            {t('search')}
          </Link>
          <Link
            href="/como-funciona"
            style={{ color: 'var(--color-primary)', fontWeight: 500 }}
          >
            {t('howItWorks')}
          </Link>
          <Link href="/leaderboard" style={{ color: 'var(--color-navy)', opacity: 0.75 }}>
            {t('leaderboard')}
          </Link>
        </nav>
        <span style={{ opacity: 0.5, fontSize: '0.75rem' }}>
          © {new Date().getFullYear()} ASUFinder
        </span>
      </div>
    </footer>
  )
}
