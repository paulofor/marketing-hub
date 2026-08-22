import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  testMatch: /assisted-service-local\.spec\.ts/,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  workers: 1,
  use: {
    baseURL:
      process.env.PDE_ASSISTED_FRONTEND_URL ??
      "http://pde-platform-frontend-kit-validation",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "desktop-chromium",
      use: { ...devices["Desktop Chrome"], browserName: "chromium" },
    },
    {
      name: "iphone-15-pro",
      use: { ...devices["iPhone 15 Pro"], browserName: "chromium" },
    },
    {
      name: "pixel-7",
      use: { ...devices["Pixel 7"], browserName: "chromium" },
    },
  ],
});
