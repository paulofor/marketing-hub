import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PDE_PUBLIC_HEALTH_URL ?? 'http://127.0.0.1:57180';
const shouldStartLocalServer = !process.env.PDE_PUBLIC_HEALTH_URL;

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: shouldStartLocalServer
    ? {
        command: 'npm run dev -- --host 127.0.0.1 --port 57180 --strictPort',
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      }
    : undefined,
  projects: [
    {
      name: 'public-desktop',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'public-mobile',
      use: { ...devices['Pixel 5'] },
    },
  ],
});
