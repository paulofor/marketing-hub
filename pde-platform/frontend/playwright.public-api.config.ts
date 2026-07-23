import { defineConfig } from '@playwright/test';

const baseURL = process.env.PDE_PUBLIC_HEALTH_URL ?? 'http://127.0.0.1:57180';
const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL,
    launchOptions: chromiumExecutablePath ? { executablePath: chromiumExecutablePath } : undefined,
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'public-api',
    },
  ],
});
