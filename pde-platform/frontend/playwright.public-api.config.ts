import { defineConfig } from '@playwright/test';

const baseURL = process.env.PDE_PUBLIC_HEALTH_URL ?? 'http://127.0.0.1:57180';

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'public-api',
    },
  ],
});
