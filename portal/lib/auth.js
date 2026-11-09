import { createNeonAuth } from '@neon/auth/next/server';

export function auth() {
  return createNeonAuth({
    baseUrl: process.env.NEON_AUTH_BASE_URL,
    cookies: { secret: process.env.NEON_AUTH_COOKIE_SECRET }
  });
}
