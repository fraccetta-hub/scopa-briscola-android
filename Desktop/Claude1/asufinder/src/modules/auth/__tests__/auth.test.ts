import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import React from 'react'

// Mock next-intl
vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

// Mock supabase client
vi.mock('@/lib/supabase/client', () => ({
  createClient: () => ({
    auth: {
      signInWithPassword: vi.fn(),
      signInWithOAuth: vi.fn(),
      signUp: vi.fn(),
    },
  }),
}))

import { LoginForm } from '../LoginForm'
import { RegisterForm } from '../RegisterForm'

describe('LoginForm', () => {
  it('renders email and password fields', () => {
    render(React.createElement(LoginForm))
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
  })
})

describe('RegisterForm', () => {
  it('renders username field', () => {
    render(React.createElement(RegisterForm))
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
  })
})
