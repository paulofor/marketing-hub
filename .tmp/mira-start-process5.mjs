import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(45_000);
  await page.goto("http://191.252.181.168:5173/products/10/value-chain-history/processes/96/activities?chainId=23&sourceReference=experiment%3A93", { waitUntil: "networkidle", timeout: 60_000 });
  const [response] = await Promise.all([
    page.waitForResponse((candidate) => candidate.request().method() === "POST" && candidate.url().includes("/api/business-processes/96/products/10/automation/v1")),
    page.getByRole("button", { name: "Executar processo" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Início do Processo 5 falhou: HTTP ${response.status()} ${body}`);
  const payload = JSON.parse(body);
  console.log(JSON.stringify({
    id: payload.id,
    status: payload.status,
    currentActivityId: payload.currentActivityId,
    currentActivityName: payload.currentActivityName,
    completedActivities: payload.completedActivities,
    remainingActivities: payload.remainingActivities,
    childRunId: payload.childRunId,
    navigationUrl: payload.navigationUrl,
    reason: payload.reason,
  }, null, 2));
} finally {
  await browser.close();
}
