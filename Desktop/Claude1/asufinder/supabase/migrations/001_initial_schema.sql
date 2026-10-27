-- ASUFinder — Initial Schema
-- All thresholds are documented in src/config/settings.ts

-- Enable pg_trgm for duplicate detection and fuzzy tag search
create extension if not exists pg_trgm;

-- Users extended profile (auth.users is the base)
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null,
  role text not null default 'user' check (role in ('user','admin','superadmin')),
  points integer not null default 0,
  created_at timestamptz default now()
);

-- Places
create table public.places (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  categories text[] not null default '{}',
  tags text[] not null default '{}',
  barrio text not null,
  address text,
  lat double precision,
  lng double precision,
  phone text,
  whatsapp text,
  website text,
  maps_url text,
  hours text,
  photo_url text,
  status text not null default 'pending'
    check (status in ('pending','live','needs_review','closed','rejected')),
  source text not null default 'user' check (source in ('user','osm')),
  is_featured boolean not null default false,
  featured_until timestamptz,
  added_by uuid references public.profiles(id),
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- Community confirmations
-- Threshold: SETTINGS.confirmations.required = 3 (see src/config/settings.ts)
create table public.confirmations (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  created_at timestamptz default now(),
  unique(place_id, user_id)
);

-- Community flags
-- Thresholds: SETTINGS.flagging.needsReviewThreshold = 3, closedThreshold = 5
create table public.flags (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  reason text,
  created_at timestamptz default now(),
  unique(place_id, user_id)
);

-- Product tags on places (core differentiator)
create table public.place_products (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  tag text not null,                        -- normalized: lowercase, unaccented
  added_by uuid references public.profiles(id),
  status text not null default 'pending'
    check (status in ('pending','confirmed','uncertain','expired','denied')),
  confirmed_count integer not null default 0,
  denied_count integer not null default 0,
  expires_at timestamptz not null default (now() + interval '90 days'),
  created_at timestamptz default now(),
  unique(place_id, tag)
);

-- Votes on product tags (confirm or deny)
create table public.product_votes (
  id uuid primary key default gen_random_uuid(),
  place_product_id uuid not null references public.place_products(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  type text not null check (type in ('confirm','deny')),
  created_at timestamptz default now(),
  unique(place_product_id, user_id)
);

-- Synonym pairs for phase 2 (links equivalent tags cross-language)
create table public.tag_synonyms (
  id uuid primary key default gen_random_uuid(),
  tag_a text not null,
  tag_b text not null,
  unique(tag_a, tag_b)
);

-- Reviews
create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  body text not null,
  created_at timestamptz default now()
);

-- Business ownership claims
create table public.place_owners (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  verified boolean not null default false,
  verified_at timestamptz,
  approved_by uuid references public.profiles(id),
  created_at timestamptz default now(),
  unique(place_id, user_id)
);

-- WhatsApp OTP tokens
-- Expiry: SETTINGS.whatsapp.otpExpiryMinutes = 10 (enforced in application layer)
create table public.otp_tokens (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  code text not null,
  used boolean not null default false,
  expires_at timestamptz not null,
  created_at timestamptz default now()
);

-- Pending edits (non-owner edits on claimed places)
create table public.place_edits (
  id uuid primary key default gen_random_uuid(),
  place_id uuid not null references public.places(id) on delete cascade,
  submitted_by uuid not null references public.profiles(id),
  patch jsonb not null,
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  created_at timestamptz default now()
);

-- Indexes
create index places_status_idx on public.places(status);
create index places_categories_idx on public.places using gin(categories);
create index places_name_trgm_idx on public.places using gin(name gin_trgm_ops);
create index places_location_idx on public.places(lat, lng) where lat is not null;

-- Indexes for fuzzy tag search
create index place_products_tag_trgm_idx on public.place_products using gin(tag gin_trgm_ops);
create index place_products_status_idx on public.place_products(status);
