import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;
const baseURL =
  process.env.MIRA_COMMERCIAL_PUBLIC_URL || "http://127.0.0.1:57181";
const shouldStartLocalServer = !process.env.MIRA_COMMERCIAL_PUBLIC_URL;

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: "line",
  use: {
    baseURL,
    launchOptions: chromiumExecutablePath
      ? { executablePath: chromiumExecutablePath }
      : {},
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    {
      name: "iphone",
      use: { ...devices["iPhone 15 Pro"], browserName: "chromium" },
    },
    { name: "pixel", use: { ...devices["Pixel 7"] } },
  ],
  webServer: shouldStartLocalServer
    ? {
        command: "npm run dev:mira-commercial -- --port 57181",
        url: baseURL,
        reuseExistingServer: false,
        timeout: 120_000,
      }
    : undefined,
});
