// ASUFinder — WhatsApp OTP Service

/**
 * Generate a random 6-digit numeric OTP string.
 */
export function generateOTP(): string {
  return Math.floor(100000 + Math.random() * 900000).toString()
}

/**
 * Send an OTP to the given phone number via WhatsApp Cloud API.
 * Reads WHATSAPP_TOKEN and WHATSAPP_PHONE_ID from environment variables.
 */
export async function sendOTP(phoneNumber: string, otp: string): Promise<void> {
  const token = process.env.WHATSAPP_TOKEN
  const phoneId = process.env.WHATSAPP_PHONE_ID

  if (!token || !phoneId) {
    throw new Error('WhatsApp not configured')
  }

  const cleanedPhone = phoneNumber.replace(/[\s+]/g, '')

  const response = await fetch(
    `https://graph.facebook.com/v18.0/${phoneId}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        messaging_product: 'whatsapp',
        to: cleanedPhone,
        type: 'text',
        text: { body: `Tu código ASUFinder: ${otp}` },
      }),
    },
  )

  if (!response.ok) {
    throw new Error(`WhatsApp API error: ${response.status}`)
  }
}

/**
 * Verify a provided OTP against the stored record.
 * Returns true if the code matches and has not expired.
 */
export function verifyOTP(
  stored: { otp: string; expiresAt: number },
  provided: string,
): boolean {
  return provided === stored.otp && Date.now() < stored.expiresAt
}
