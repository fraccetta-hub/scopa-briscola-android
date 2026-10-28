import { describe, it, expect } from 'vitest'
import { generateOTP, verifyOTP } from '../otp'

describe('generateOTP', () => {
  it('generateOTP returns 6 digits', () => {
    const otp = generateOTP()
    expect(otp).toMatch(/^\d{6}$/)
  })
})

describe('verifyOTP', () => {
  it('verifyOTP returns true for valid unexpired code', () => {
    const stored = { otp: '123456', expiresAt: Date.now() + 60000 }
    expect(verifyOTP(stored, '123456')).toBe(true)
  })

  it('verifyOTP returns false for expired code', () => {
    const stored = { otp: '123456', expiresAt: Date.now() - 1000 }
    expect(verifyOTP(stored, '123456')).toBe(false)
  })

  it('verifyOTP returns false for wrong code', () => {
    const stored = { otp: '123456', expiresAt: Date.now() + 60000 }
    expect(verifyOTP(stored, '000000')).toBe(false)
  })
})
