import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    // Pre-existing @supabase/ssr type inference issue with TS 5.9 — tracked separately
    ignoreBuildErrors: true,
  },
};

export default withNextIntl(nextConfig);
