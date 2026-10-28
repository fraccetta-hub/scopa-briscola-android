import { describe, it, expect } from 'vitest'
import { checkContent } from '../moderation.service'

describe('checkContent', () => {
  it('passes clean content', () => {
    expect(checkContent('Great pharmacy in Centro').passed).toBe(true)
  })

  it('blocks blacklisted content', () => {
    const result = checkContent('SPAM BUY NOW deal')
    expect(result.passed).toBe(false)
    expect(result.reason).toContain('spam')
  })

  it('is case-insensitive', () => {
    expect(checkContent('FREE MONEY here').passed).toBe(false)
  })
})
