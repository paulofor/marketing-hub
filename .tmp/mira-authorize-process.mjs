import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1200 },
    locale: "pt-BR",
  });
  page.setDefaultTimeout(60_000);
  await page.goto(
    "http://191.252.181.168:5173/products/10/value-chain-history/processes/96/activities?chainId=23&sourceReference=experiment%3A93",
    { waitUntil: "networkidle", timeout: 60_000 },
  );

  const button = page.getByRole("button", { name: "Li, entendi e autorizo" });
  if (await button.isDisabled()) {
    throw new Error("A autorização continua bloqueada pelos gates do Processo 5.");
  }
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" &&
        candidate.url().includes(
          "/api/business-processes/96/products/10/activities/authorization/execution-requests",
        ),
    ),
    button.click(),
  ]);
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`Autorização falhou: HTTP ${response.status()} ${body}`);
  }
  console.log(
    JSON.stringify(
      { status: response.status(), authorization: JSON.parse(body) },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
