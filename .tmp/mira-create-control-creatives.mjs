import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const copy = {
  headline: "Sua rotina, finalmente em ordem",
  primaryText:
    "Envie os produtos que você já tem e receba uma rotina individualizada, clara e consultável. Pagamento único de R$ 49.",
  description: "Organize sem comprar mais",
};
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

async function submitModal(page, endpointPart, labels) {
  await page.getByLabel(labels.headline).fill(copy.headline);
  await page.getByLabel(labels.primaryText).fill(copy.primaryText);
  await page.getByLabel(labels.description).fill(copy.description);
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" && candidate.url().includes(endpointPart),
      { timeout: 120_000 },
    ),
    page.getByRole("button", { name: "Cadastrar e enviar para revisão" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Cadastro falhou: HTTP ${response.status()} ${body}`);
  return JSON.parse(body);
}

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 }, locale: "pt-BR" });
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=creatives", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const staticCard = page.locator("article").filter({ hasText: "Ativo #322" });
  await staticCard.getByRole("button", { name: "Usar como controle estático" }).click();
  const staticCreative = await submitModal(
    page,
    "/api/experiments/93/commercial-plan-visual-assets/322/creative",
    {
      headline: "Título do controle estático",
      primaryText: "Texto principal do controle estático",
      description: "Descrição do controle estático",
    },
  );

  await page.goto("http://191.252.181.168:5173/experiments/93?tab=video", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const videoRow = page.locator("tbody tr").filter({ has: page.locator("td:first-child", { hasText: /^49$/ }) });
  await videoRow.getByRole("button", { name: "Usar vídeo em anúncio" }).click();
  const videoCreative = await submitModal(page, "/api/experiments/93/video-assets/49/creative", {
    headline: "Título do anúncio",
    primaryText: "Texto principal",
    description: "Descrição curta (opcional)",
  });
  console.log(JSON.stringify({ copy, staticCreative, videoCreative }, null, 2));
} finally {
  await browser.close();
}
