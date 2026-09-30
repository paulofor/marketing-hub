import { defineConfig, devices } from "@playwright/test";

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN;

/** Executa a matriz visual do protótipo Alcyone no container local ou na URL informada. */
export default defineConfig({
  testDir: "./tests",
  timeout: 90_000,
  retries: 0,
  workers: 1,
  use: {
    baseURL: process.env.ALCYONE_BASE_URL || "http://127.0.0.1:5184",
    browserName: "chromium",
    launchOptions: executablePath ? { executablePath } : undefined,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "desktop-1440", use: { viewport: { width: 1440, height: 900 } } },
    { name: "iphone-15-pro", use: { ...devices["iPhone 15 Pro"], browserName: "chromium" } },
    { name: "pixel-7", use: { ...devices["Pixel 7"], browserName: "chromium" } },
  ],
});
