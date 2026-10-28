import { NextRequest, NextResponse } from 'next/server'
import { createAdminClient } from '@/lib/supabase/admin'

export async function GET(request: NextRequest) {
  // Validate cron secret
  const authHeader = request.headers.get('authorization')
  const expected = `Bearer ${process.env.CRON_SECRET ?? ''}`
  if (!process.env.CRON_SECRET || authHeader !== expected) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const supabase = createAdminClient()
  const now = new Date().toISOString()
  const ninetyDaysAgo = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString()

  // Step 1: confirmed → uncertain: expires_at < now()
  // TS5.9 + @supabase/ssr: .update() resolves to never[] — cast required
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: data1, error: error1 } = await (supabase as any)
    .from('place_products')
    .update({ status: 'uncertain' } as any)
    .eq('status', 'confirmed')
    .lt('expires_at', now)
    .select() as { data: unknown[] | null; error: { message: string } | null }

  if (error1) {
    return NextResponse.json({ error: error1.message }, { status: 500 })
  }

  // Step 2: uncertain → expired: expires_at < now() - 90 days
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: data2, error: error2 } = await (supabase as any)
    .from('place_products')
    .update({ status: 'expired' } as any)
    .eq('status', 'uncertain')
    .lt('expires_at', ninetyDaysAgo)
    .select() as { data: unknown[] | null; error: { message: string } | null }

  if (error2) {
    return NextResponse.json({ error: error2.message }, { status: 500 })
  }

  return NextResponse.json({
    updated1: data1?.length ?? 0,
    updated2: data2?.length ?? 0,
  })
}
