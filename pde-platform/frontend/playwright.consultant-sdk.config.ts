import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  "/usr/bin/chromium";

export default defineConfig({
  testDir: "./tests",
  testMatch: "consultant-sdk-v1.visual.spec.ts",
  fullyParallel: false,
  workers: 1,
  timeout: 30_000,
  expect: { timeout: 5_000 },
  use: {
    baseURL: "http://127.0.0.1:57183",
    browserName: "chromium",
    launchOptions: {
      executablePath: chromiumExecutablePath,
      args: ["--no-sandbox"],
    },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 57183 --strictPort",
    url: "http://127.0.0.1:57183/__qa/consultant-sdk-v1",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
  projects: [
    {
      name: "desktop",
      use: { ...devices["Desktop Chrome"], browserName: "chromium" },
    },
    {
      name: "iPhone 15 Pro",
      use: { ...devices["iPhone 15 Pro"], browserName: "chromium" },
    },
    {
      name: "Pixel 7",
      use: { ...devices["Pixel 7"], browserName: "chromium" },
    },
  ],
});
