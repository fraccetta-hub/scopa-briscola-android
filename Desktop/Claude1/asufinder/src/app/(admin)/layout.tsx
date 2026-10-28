// ASUFinder — Admin layout (server component)
// Gates on admin or superadmin role; redirects to / otherwise.

import { redirect } from 'next/navigation'
import { createClient } from '@/lib/supabase/server'
import { getTranslations } from 'next-intl/server'
import Link from 'next/link'

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const supabase = await createClient()
  const { data: { user }, error: authError } = await supabase.auth.getUser()

  if (authError || !user) {
    redirect('/')
  }

  // TS5.9 + @supabase/ssr: .select('role') resolves to never — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: profile } = await (supabase as any)
    .from('profiles')
    .select('role')
    .eq('id', user.id)
    .single() as { data: { role: string } | null; error: unknown }

  if (!profile || !['admin', 'superadmin'].includes(profile.role)) {
    redirect('/')
  }

  const t = await getTranslations('admin')

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      <header style={{ backgroundColor: 'var(--color-navy)', color: 'var(--color-white)' }} className="px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <h1 className="text-lg font-bold">{t('title')}</h1>
          <nav className="flex gap-4 text-sm">
            <Link href="/pending" className="hover:underline">{t('pending')}</Link>
            <Link href="/claims" className="hover:underline">{t('claims')}</Link>
            <Link href="/users" className="hover:underline">{t('users')}</Link>
          </nav>
        </div>
      </header>
      <main className="max-w-6xl mx-auto px-6 py-8">
        {children}
      </main>
    </div>
  )
}
