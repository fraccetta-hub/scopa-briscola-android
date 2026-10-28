import { getTranslations } from 'next-intl/server'
import { RegisterForm } from '@/modules/auth/RegisterForm'
import Link from 'next/link'

export default async function RegisterPage() {
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
          {t('registerTitle')}
        </h1>
        <RegisterForm />
        <p className="mt-4 text-sm" style={{ color: 'var(--color-navy)' }}>
          {t('hasAccount')}{' '}
          <Link href="/login" style={{ color: 'var(--color-primary)' }}>
            {t('login')}
          </Link>
        </p>
      </div>
    </main>
  )
}
