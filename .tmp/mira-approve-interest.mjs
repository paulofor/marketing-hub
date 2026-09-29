import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(30_000);
  await page.goto("http://191.252.181.168:5173/niches/34#niche-targeting", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const card = page.locator(".card").filter({ has: page.getByRole("heading", { name: "Skin care", exact: true }) });
  await card.getByText("664130153728886", { exact: true }).waitFor();
  await card.getByText("257137942", { exact: false }).waitFor().catch(() => {});
  const [response] = await Promise.all([
    page.waitForResponse((candidate) =>
      candidate.request().method() === "PATCH" &&
      candidate.url().includes("/api/targeting-elements/397")),
    card.getByRole("button", { name: "Aprovar" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Aprovação falhou: HTTP ${response.status()} ${body}`);
  const payload = JSON.parse(body);
  console.log(JSON.stringify({
    id: payload.id,
    status: payload.status,
    metaId: payload.metaId,
    metaKey: payload.metaKey,
    reach: [payload.metaAudienceSizeLowerBound, payload.metaAudienceSizeUpperBound],
  }, null, 2));
} finally {
  await browser.close();
}
