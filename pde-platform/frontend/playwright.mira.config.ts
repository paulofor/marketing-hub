import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;

/** Homologa a superfície exclusiva de Mira nos dispositivos relevantes. */
export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  expect: { timeout: 5_000 },
  use: {
    baseURL: "http://127.0.0.1:57180",
    launchOptions: chromiumExecutablePath
      ? { executablePath: chromiumExecutablePath }
      : {},
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      "npm run build:mira && npm run preview:mira -- --host 127.0.0.1 --port 57180 --strictPort",
    url: "http://127.0.0.1:57180/mira-private",
    reuseExistingServer: false,
    timeout: 120_000,
  },
  projects: [
    { name: "desktop-chrome", use: { ...devices["Desktop Chrome"] } },
    {
      name: "iphone-15-pro",
      use: { ...devices["iPhone 15 Pro"], browserName: "chromium" },
    },
    { name: "pixel-7", use: { ...devices["Pixel 7"] } },
  ],
});
