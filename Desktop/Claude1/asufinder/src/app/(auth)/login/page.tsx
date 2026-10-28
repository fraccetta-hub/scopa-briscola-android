import { getTranslations } from 'next-intl/server'
import { LoginForm } from '@/modules/auth/LoginForm'
import Link from 'next/link'

export default async function LoginPage() {
  const t = await getTranslations('auth')

  return (
    <main
      className="min-h-screen flex items-center justify-center"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      <div
        className="w-full max-w-md p-8 rounded-xl shadow-sm"
        style={{ backgroundColor: 'var(--color-surface)' }}
      >
        <h1
          className="text-2xl font-bold mb-6"
          style={{ color: 'var(--color-navy)' }}
        >
          {t('loginTitle')}
        </h1>
        <LoginForm />
        <p className="mt-4 text-sm" style={{ color: 'var(--color-navy)' }}>
          {t('noAccount')}{' '}
          <Link href="/register" style={{ color: 'var(--color-primary)' }}>
            {t('register')}
          </Link>
        </p>
      </div>
    </main>
  )
}
