import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;
const mysqlPort = process.env.PDE_LOCAL_MYSQL_PORT ?? "33068";
const mysqlHost = process.env.PDE_LOCAL_MYSQL_HOST ?? "127.0.0.1";
const chromiumLaunchOptions = chromiumExecutablePath
  ? { executablePath: chromiumExecutablePath, args: ["--no-sandbox"] }
  : { args: ["--no-sandbox"] };

export default defineConfig({
  testDir: "./tests",
  testMatch: /assisted-service-local\.spec\.ts/,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:57182",
    launchOptions: chromiumLaunchOptions,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: [
        "env",
        "PDE_MARKETING_HUB_BASE_URL=http://191.252.181.168",
        "PDE_DEV_ACCESS_ENABLED=true",
        "PDE_ASSISTED_OPERATION_ENABLED=true",
        "PDE_ASSISTED_OPERATION_TOKEN=pde-local-operation-test",
        `PDE_ACCESS_JDBC_URL="jdbc:mysql://${mysqlHost}:${mysqlPort}/pde_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"`,
        "PDE_ACCESS_JDBC_USERNAME=pde",
        "PDE_ACCESS_JDBC_PASSWORD=pde",
        "PDE_ACCESS_REQUIRE_JDBC=true",
        "PDE_ACCESS_STORAGE_PATH=/tmp/pde-kit-whatsapp-access-grants.json",
        "PDE_AI_STORAGE_PATH=/tmp/pde-kit-whatsapp-ai-guidance.json",
        "PDE_APP_BASE_URL=http://127.0.0.1:57182",
        "PDE_MAIL_TRANSPORT=smtp",
        "PDE_SMTP_HOST=sandbox-mail",
        "PDE_SMTP_PORT=1025",
        "mvn -f ../backend/pom.xml spring-boot:run",
      ].join(" "),
      url: "http://127.0.0.1:8096/actuator/health",
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command:
        "VITE_PDE_PRODUCT_SLUG=kit-whatsapp-pronto VITE_PDE_ENABLE_DEV_ACCESS=true npm run dev -- --host 127.0.0.1 --port 57182 --strictPort",
      url: "http://127.0.0.1:57182",
      reuseExistingServer: false,
      timeout: 120_000,
    },
  ],
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
