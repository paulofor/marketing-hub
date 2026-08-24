import { defineConfig, devices } from "@playwright/test";

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  undefined;
const launchOptions = {
  ...(chromiumExecutablePath ? { executablePath: chromiumExecutablePath } : {}),
  args: [
    "--host-resolver-rules=MAP v5.clubemusa.com.br 127.0.0.1,MAP v6.clubemusa.com.br 127.0.0.1,MAP v7.clubemusa.com.br 127.0.0.1",
    "--autoplay-policy=no-user-gesture-required",
  ],
};
const mysqlPort = process.env.PDE_LOCAL_MYSQL_PORT ?? "33067";

export default defineConfig({
  testDir: "./tests",
  testMatch: /musa-local-integration\.spec\.ts/,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL: "http://127.0.0.1:57180",
    launchOptions,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: "node tests/marketing-hub-contract-server.mjs",
      url: "http://127.0.0.1:57181/api/products/public/metodo-musa-7-dias/pde-experience?slotCode=v6",
      reuseExistingServer: !process.env.CI,
      timeout: 30_000,
    },
    {
      command: [
        "env",
        "PDE_MARKETING_HUB_BASE_URL=http://127.0.0.1:57181",
        `PDE_ACCESS_JDBC_URL="jdbc:mysql://127.0.0.1:${mysqlPort}/pde_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"`,
        "PDE_ACCESS_JDBC_USERNAME=pde",
        "PDE_ACCESS_JDBC_PASSWORD=pde",
        "PDE_ACCESS_REQUIRE_JDBC=true",
        "PDE_PEPPER_API_BASE_URL=http://127.0.0.1:57181/public/v1",
        "PDE_PEPPER_API_TOKEN=pde-local-pepper-token",
        "PDE_PEPPER_OFFER_HASHES=owm6x",
        "PDE_PEPPER_EXPECTED_PAID_AMOUNT_CENTS=6700",
        "PDE_PEPPER_EXPECTED_CURRENCY=BRL",
        "PDE_PEPPER_FALLBACK_EXPERIENCE_VERSION=musa-pde-entry-v7-espelho-antes-de-sair",
        "PDE_ACCESS_STORAGE_PATH=/tmp/pde-local-access-grants.json",
        "PDE_AI_STORAGE_PATH=/tmp/pde-local-ai-guidance-requests.json",
        "PDE_APP_BASE_URL=http://localhost:57180",
        "PDE_MAIL_TRANSPORT=smtp",
        "PDE_SMTP_HOST=sandbox-mail",
        "PDE_SMTP_PORT=1025",
        "mvn -f ../backend/pom.xml spring-boot:run",
      ].join(" "),
      url: "http://127.0.0.1:8096/actuator/health",
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: "npm run dev -- --host 127.0.0.1 --port 57180 --strictPort",
      url: "http://127.0.0.1:57180",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
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
