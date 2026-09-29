import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

const targets = [
  {
    id: 538,
    selector: 'img[src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/09/29/misc/473edffa7774-mira-commercial-control-v4.png"]',
  },
  {
    id: 539,
    selector: 'video[src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/sales-videos/2026/09/28/misc/70697339beb7-mira-commercial-demo-v3.mp4"]',
  },
];

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(30_000);
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=creatives", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const results = [];
  for (const target of targets) {
    const card = page.locator("article.creative-card").filter({ has: page.locator(target.selector) });
    await card.getByText("Agente: aprovado", { exact: true }).waitFor();
    const approveButton = card.getByRole("button", { name: "Aprovar" });
    if (!(await approveButton.isVisible().catch(() => false))) {
      results.push({ id: target.id, status: "READY", agentReviewStatus: "APPROVED", alreadyReady: true });
      continue;
    }
    const [response] = await Promise.all([
      page.waitForResponse((candidate) =>
        candidate.request().method() === "PATCH" &&
        candidate.url().includes(`/api/creatives/${target.id}/status`)),
      approveButton.click(),
    ]);
    const body = await response.text();
    if (!response.ok()) throw new Error(`Aprovação #${target.id} falhou: HTTP ${response.status()} ${body}`);
    const payload = JSON.parse(body);
    results.push({ id: payload.id, status: payload.status, agentReviewStatus: payload.agentReviewStatus });
  }
  console.log(JSON.stringify(results, null, 2));
} finally {
  await browser.close();
}
