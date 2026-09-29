import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const pageUrl = "http://191.252.181.168:5173/niches/34#niche-targeting";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.setDefaultTimeout(20_000);
  await page.goto(pageUrl, { waitUntil: "networkidle", timeout: 45_000 });
  const card = page.locator(".border.rounded-3.p-3.h-100").filter({
    has: page.getByText("Interesses", { exact: true }),
  });
  await card.waitFor({ state: "visible" });
  const form = card.locator("form");
  await form.locator('input[type="number"]').fill("1");
  await form.locator("select").selectOption("gpt-5.6-sol");
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes("/api/niches/34/interests-to-generate") &&
      response.request().method() === "PATCH",
  );
  await form.getByRole("button", { name: "Gerar interesses" }).click();
  const response = await responsePromise;
  if (!response.ok()) {
    throw new Error(`Solicitação falhou: HTTP ${response.status()} ${await response.text()}`);
  }
  const payload = await response.json();
  await form.getByText(/1 pendente\(s\)/).waitFor();
  process.stdout.write(
    `${JSON.stringify(
      {
        nicheId: payload.id,
        interestsToGenerate: payload.interestsToGenerate,
        interestModel: payload.interestModel,
        requestStatus: response.status(),
      },
      null,
      2,
    )}\n`,
  );
} finally {
  await browser.close();
}
