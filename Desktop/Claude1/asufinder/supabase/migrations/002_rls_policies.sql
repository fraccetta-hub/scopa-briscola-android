-- ASUFinder — Row Level Security Policies

-- Enable RLS on all tables
alter table public.profiles enable row level security;
alter table public.places enable row level security;
alter table public.confirmations enable row level security;
alter table public.flags enable row level security;
alter table public.place_products enable row level security;
alter table public.product_votes enable row level security;
alter table public.tag_synonyms enable row level security;
alter table public.reviews enable row level security;
alter table public.place_owners enable row level security;
alter table public.otp_tokens enable row level security;
alter table public.place_edits enable row level security;

-- profiles
create policy "profiles_select_all"
  on public.profiles for select
  using (true);

create policy "profiles_insert_own"
  on public.profiles for insert
  with check (auth.uid() = id);

create policy "profiles_update_own"
  on public.profiles for update
  using (auth.uid() = id);

-- places
create policy "places_select_live"
  on public.places for select
  using (status = 'live');

create policy "places_insert_authenticated"
  on public.places for insert
  with check (auth.role() = 'authenticated');

create policy "places_update_owner_or_admin"
  on public.places for update
  using (
    auth.uid() = added_by
    or exists (
      select 1 from public.profiles
      where id = auth.uid() and role in ('admin', 'superadmin')
    )
    or exists (
      select 1 from public.place_owners
      where place_id = places.id and user_id = auth.uid() and verified = true
    )
  );

-- confirmations
create policy "confirmations_select_all"
  on public.confirmations for select
  using (true);

create policy "confirmations_insert_authenticated"
  on public.confirmations for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- flags
create policy "flags_select_all"
  on public.flags for select
  using (true);

create policy "flags_insert_authenticated"
  on public.flags for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- place_products
create policy "place_products_select_all"
  on public.place_products for select
  using (true);

create policy "place_products_insert_authenticated"
  on public.place_products for insert
  with check (auth.role() = 'authenticated');

-- product_votes
create policy "product_votes_select_all"
  on public.product_votes for select
  using (true);

create policy "product_votes_insert_authenticated"
  on public.product_votes for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- tag_synonyms
create policy "tag_synonyms_select_all"
  on public.tag_synonyms for select
  using (true);

-- reviews
create policy "reviews_select_all"
  on public.reviews for select
  using (true);

create policy "reviews_insert_authenticated"
  on public.reviews for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- place_owners (claims visible to all, managed by admins)
create policy "place_owners_select_all"
  on public.place_owners for select
  using (true);

create policy "place_owners_insert_authenticated"
  on public.place_owners for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- otp_tokens (only owner can read their own tokens)
create policy "otp_tokens_select_own"
  on public.otp_tokens for select
  using (auth.uid() = user_id);

create policy "otp_tokens_insert_authenticated"
  on public.otp_tokens for insert
  with check (auth.role() = 'authenticated' and auth.uid() = user_id);

-- place_edits
create policy "place_edits_select_all"
  on public.place_edits for select
  using (true);

create policy "place_edits_insert_authenticated"
  on public.place_edits for insert
  with check (auth.role() = 'authenticated' and auth.uid() = submitted_by);
