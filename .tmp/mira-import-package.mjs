import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;

const archive = "/tmp/aihub-mira-commercial-approved-v4-20260928.zip";
const browser = await chromium.launch({
  headless: true,
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});

try {
  const page = await browser.newPage({ locale: "pt-BR" });
  await page.goto("http://191.252.181.168:5173/planning/8", {
    waitUntil: "domcontentloaded",
    timeout: 60_000,
  });
  await page.getByLabel("Pacote ZIP aprovado *").setInputFiles(archive);
  await page.getByLabel("Selecionei este pacote para uso no plano *").check();
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.url().includes("/api/planning/commercial-plans/8/visual-assets/approved-package") &&
        candidate.request().method() === "POST",
      { timeout: 120_000 },
    ),
    page.getByRole("button", { name: "Importar pacote" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`Importação falhou: HTTP ${response.status()} ${body}`);
  }
  await page.getByRole("status").filter({ hasText: "Pacote importado" }).waitFor({ timeout: 30_000 });
  console.log(JSON.stringify({ status: response.status(), body: JSON.parse(body) }, null, 2));
} finally {
  await browser.close();
}
