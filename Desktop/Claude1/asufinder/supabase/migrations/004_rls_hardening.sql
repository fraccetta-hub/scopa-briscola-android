-- ASUFinder — RLS Hardening
-- Prevents privilege escalation via profiles UPDATE and enforces status constraints
-- on INSERT for places, place_products, and place_owners.

-- ============================================================
-- profiles: prevent role / points escalation via trigger
-- ============================================================

-- Recreate update policy with WITH CHECK (belt-and-suspenders with trigger below)
DROP POLICY IF EXISTS profiles_update_own ON public.profiles;
CREATE POLICY profiles_update_own ON public.profiles
  FOR UPDATE USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

CREATE OR REPLACE FUNCTION prevent_privilege_escalation()
RETURNS TRIGGER AS $$
BEGIN
  -- service_role (admin client) is exempt — triggers fire for all roles in PG
  IF current_setting('role', true) = 'service_role' THEN
    RETURN NEW;
  END IF;
  IF OLD.role IS DISTINCT FROM NEW.role THEN
    RAISE EXCEPTION 'role changes not allowed via client';
  END IF;
  IF OLD.points IS DISTINCT FROM NEW.points THEN
    RAISE EXCEPTION 'points changes not allowed via client';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS check_privilege_escalation ON public.profiles;
CREATE TRIGGER check_privilege_escalation
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW
  WHEN (pg_trigger_depth() = 0)
  EXECUTE FUNCTION prevent_privilege_escalation();

-- ============================================================
-- places: cannot insert with status other than 'pending'
-- ============================================================
DROP POLICY IF EXISTS places_insert_authenticated ON public.places;
CREATE POLICY places_insert_authenticated ON public.places
  FOR INSERT WITH CHECK (auth.role() = 'authenticated' AND status = 'pending');

-- ============================================================
-- place_products: must own the add_by field, must be pending
-- ============================================================
DROP POLICY IF EXISTS place_products_insert_authenticated ON public.place_products;
CREATE POLICY place_products_insert_authenticated ON public.place_products
  FOR INSERT WITH CHECK (
    auth.role() = 'authenticated'
    AND added_by = auth.uid()
    AND status = 'pending'
  );

-- ============================================================
-- place_owners: cannot insert as verified
-- ============================================================
DROP POLICY IF EXISTS place_owners_insert_authenticated ON public.place_owners;
CREATE POLICY place_owners_insert_authenticated ON public.place_owners
  FOR INSERT WITH CHECK (
    auth.role() = 'authenticated'
    AND user_id = auth.uid()
    AND verified = false
  );
