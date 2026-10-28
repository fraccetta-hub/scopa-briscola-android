import { getLocale } from 'next-intl/server'
import Link from 'next/link'
import { howItWorksEs } from '@/i18n/pages/how-it-works.es'
import { howItWorksEn } from '@/i18n/pages/how-it-works.en'
import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Cómo funciona – ASUFinder',
  description:
    'Aprende a buscar, agregar y confirmar lugares en el directorio comunitario de Gran Asunción.',
}

const sectionIcons: Record<string, string> = {
  buscar: '🔍',
  agregar: '➕',
  confirmar: '✅',
  reclamar: '🏪',
  puntos: '⭐',
  faq: '❓',
}

export default async function ComoFuncionaPage() {
  const locale = await getLocale()
  const content = locale === 'en' ? howItWorksEn : howItWorksEs
  const { sections } = content

  const orderedSections = [
    sections.search,
    sections.add,
    sections.confirm,
    sections.claim,
    sections.points,
    sections.faq,
  ]

  return (
    <main
      className="min-h-screen py-12 px-4"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      {/* Page header */}
      <div className="max-w-2xl mx-auto mb-12">
        <h1
          className="text-3xl md:text-4xl font-bold mb-4"
          style={{ color: 'var(--color-navy)' }}
        >
          {content.title}
        </h1>
        <p
          className="text-base md:text-lg leading-relaxed"
          style={{ color: 'var(--color-navy)', opacity: 0.8 }}
        >
          {content.intro}
        </p>
      </div>

      {/* Section anchor nav */}
      <nav
        className="max-w-2xl mx-auto mb-10 flex flex-wrap gap-2"
        aria-label={locale === 'en' ? 'Page sections' : 'Secciones'}
      >
        {orderedSections.map((section) => (
          <a
            key={section.id}
            href={`#${section.id}`}
            className="text-sm px-3 py-1 rounded-full transition-colors"
            style={{
              backgroundColor: 'var(--color-mint)',
              color: 'var(--color-primary)',
              textDecoration: 'none',
            }}
          >
            {sectionIcons[section.id]} {section.title}
          </a>
        ))}
      </nav>

      {/* Sections */}
      <div className="max-w-2xl mx-auto flex flex-col gap-12">
        {/* Search */}
        <section id={sections.search.id}>
          <SectionHeading icon={sectionIcons[sections.search.id]} title={sections.search.title} />
          <p className="leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.85 }}>
            {sections.search.body}
          </p>
        </section>

        {/* Add */}
        <section id={sections.add.id}>
          <SectionHeading icon={sectionIcons[sections.add.id]} title={sections.add.title} />
          <p className="leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.85 }}>
            {sections.add.body}
          </p>
        </section>

        {/* Confirm */}
        <section id={sections.confirm.id}>
          <SectionHeading icon={sectionIcons[sections.confirm.id]} title={sections.confirm.title} />
          <p className="leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.85 }}>
            {sections.confirm.body}
          </p>
        </section>

        {/* Claim */}
        <section id={sections.claim.id}>
          <SectionHeading icon={sectionIcons[sections.claim.id]} title={sections.claim.title} />
          <p className="leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.85 }}>
            {sections.claim.body}
          </p>
        </section>

        {/* Points */}
        <section id={sections.points.id}>
          <SectionHeading icon={sectionIcons[sections.points.id]} title={sections.points.title} />
          <p className="mb-4 leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.85 }}>
            {sections.points.intro}
          </p>
          <div
            className="rounded-xl overflow-hidden border mb-4"
            style={{ borderColor: 'var(--color-mint)' }}
          >
            <table className="w-full text-sm">
              <thead>
                <tr style={{ backgroundColor: 'var(--color-mint)' }}>
                  {sections.points.table.headers.map((h) => (
                    <th
                      key={h}
                      className="px-4 py-2 text-left font-semibold"
                      style={{ color: 'var(--color-navy)' }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {sections.points.table.rows.map(([action, pts]) => (
                  <tr
                    key={action}
                    className="border-t"
                    style={{ borderColor: 'var(--color-mint)' }}
                  >
                    <td
                      className="px-4 py-2"
                      style={{ color: 'var(--color-navy)', backgroundColor: 'var(--color-surface)' }}
                    >
                      {action}
                    </td>
                    <td
                      className="px-4 py-2 font-mono font-medium"
                      style={{
                        color: pts.startsWith('+') ? 'var(--color-primary)' : 'var(--color-error)',
                        backgroundColor: 'var(--color-surface)',
                      }}
                    >
                      {pts}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--color-navy)', opacity: 0.7 }}>
            {sections.points.leaderboard}
          </p>
          <div className="mt-4">
            <Link
              href="/leaderboard"
              className="text-sm font-medium"
              style={{ color: 'var(--color-primary)' }}
            >
              {locale === 'en' ? 'View leaderboard →' : 'Ver ranking →'}
            </Link>
          </div>
        </section>

        {/* FAQ */}
        <section id={sections.faq.id}>
          <SectionHeading icon={sectionIcons[sections.faq.id]} title={sections.faq.title} />
          <dl className="flex flex-col gap-6">
            {sections.faq.items.map((item) => (
              <div key={item.q}>
                <dt className="font-semibold mb-1" style={{ color: 'var(--color-navy)' }}>
                  {item.q}
                </dt>
                <dd
                  className="leading-relaxed text-sm"
                  style={{ color: 'var(--color-navy)', opacity: 0.8 }}
                >
                  {item.a}
                </dd>
              </div>
            ))}
          </dl>
        </section>

        {/* Bottom CTA */}
        <div
          className="rounded-xl p-6 flex flex-col gap-3 items-start"
          style={{ backgroundColor: 'var(--color-mint)' }}
        >
          <p className="font-semibold" style={{ color: 'var(--color-navy)' }}>
            {locale === 'en'
              ? 'Ready to contribute?'
              : '¿Listo para contribuir?'}
          </p>
          <div className="flex flex-wrap gap-3">
            <Link
              href="/register"
              className="px-4 py-2 rounded-lg text-sm font-medium"
              style={{
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-white)',
                textDecoration: 'none',
              }}
            >
              {locale === 'en' ? 'Create account' : 'Crear cuenta'}
            </Link>
            <Link
              href="/"
              className="px-4 py-2 rounded-lg text-sm font-medium"
              style={{
                backgroundColor: 'var(--color-surface)',
                color: 'var(--color-primary)',
                textDecoration: 'none',
              }}
            >
              {locale === 'en' ? 'Search places' : 'Buscar lugares'}
            </Link>
          </div>
        </div>
      </div>
    </main>
  )
}

function SectionHeading({ icon, title }: { icon: string; title: string }) {
  return (
    <h2
      className="text-xl md:text-2xl font-bold mb-3 flex items-center gap-2"
      style={{ color: 'var(--color-navy)' }}
    >
      <span aria-hidden="true">{icon}</span>
      {title}
    </h2>
  )
}
