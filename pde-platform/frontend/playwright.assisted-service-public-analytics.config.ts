import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;

export default defineConfig({
  testDir: "./tests",
  testMatch: /assisted-service-public-analytics\.spec\.ts/,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:57183",
    launchOptions: chromiumExecutablePath
      ? { executablePath: chromiumExecutablePath, args: ["--no-sandbox"] }
      : { args: ["--no-sandbox"] },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      "VITE_PDE_PRODUCT_SLUG=kit-whatsapp-pronto VITE_PDE_ENABLE_DEV_ACCESS=false npm run dev -- --host 127.0.0.1 --port 57183 --strictPort",
    url: "http://127.0.0.1:57183",
    reuseExistingServer: false,
    timeout: 120_000,
  },
  projects: [
    {
      name: "public-build-chromium",
      use: { ...devices["Desktop Chrome"], browserName: "chromium" },
    },
  ],
});
