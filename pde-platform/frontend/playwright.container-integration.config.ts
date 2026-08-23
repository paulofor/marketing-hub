import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  testMatch: /musa-local-integration\.spec\.ts/,
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL: process.env.PDE_TEST_FRONTEND_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'container-desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'container-iphone-15-pro', use: { ...devices['iPhone 15 Pro'] } },
    { name: 'container-pixel-7', use: { ...devices['Pixel 7'] } },
  ],
});
