import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { OTPForm } from '../OTPForm'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

describe('OTPForm', () => {
  it('OTPForm renders input and submit button', () => {
    render(<OTPForm placeId="p1" onSuccess={vi.fn()} />)
    expect(screen.getByPlaceholderText(/otpPlaceholder|6/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /verificar|verify/i })).toBeInTheDocument()
  })
})
