// ASUFinder — Content moderation service
import { BLACKLIST } from './blacklist'

export function checkContent(text: string): { passed: boolean; reason?: string } {
  const lower = text.toLowerCase()
  for (const term of BLACKLIST) {
    if (lower.includes(term)) {
      return { passed: false, reason: `blacklisted: ${term}` }
    }
  }
  return { passed: true }
}
