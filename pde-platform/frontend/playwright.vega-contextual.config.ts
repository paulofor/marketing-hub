import { defineConfig, devices } from "@playwright/test";

// Usa a aplicação e o MySQL locais descritos em infra/testing/vega-contextual-style.
export default defineConfig({
  testDir: "./tests",
  testMatch: "vega-contextual-next-step.spec.ts",
  workers: 1,
  timeout: 60_000,
  use: {
    baseURL: "http://127.0.0.1:57188",
    launchOptions: {
      executablePath:
        process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || "/usr/bin/chromium",
    },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [
    { name: "desktop", use: { viewport: { width: 1440, height: 900 } } },
    {
      name: "iphone",
      use: { ...devices["iPhone 15 Pro"], browserName: "chromium" },
    },
    { name: "pixel", use: { ...devices["Pixel 7"], browserName: "chromium" } },
  ],
});
