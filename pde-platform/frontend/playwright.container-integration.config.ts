import { defineConfig, devices } from "@playwright/test";

/** Executa a homologação PDE dentro da rede Compose isolada da sandbox. */
export default defineConfig({
  testDir: "./tests",
  testMatch: /(musa-local-integration|mira-private-prototype)\.spec\.ts/,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL:
      process.env.PDE_TEST_FRONTEND_URL ?? "http://pde-platform-frontend",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "local-integration-chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "local-integration-iphone-15-pro",
      use: { ...devices["iPhone 15 Pro"] },
    },
    {
      name: "local-integration-pixel-7",
      use: { ...devices["Pixel 7"] },
    },
  ],
});
