// ASUFinder — Admin claims page (server component)

import { getPendingClaims } from '@/lib/supabase/queries/admin'
import { ClaimsList } from '@/modules/admin/ClaimsList'

export default async function ClaimsPage() {
  const claims = await getPendingClaims()
  return <ClaimsList claims={claims} />
}
