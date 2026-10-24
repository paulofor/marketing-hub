import assert from "node:assert/strict";
import { readFile, mkdir } from "node:fs/promises";
import { chromium, devices, expect as baseExpect } from "@playwright/test";

const expect = baseExpect.configure({ timeout: 30000 });

const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:4173";
const output = process.env.EVIDENCE_DIR || "../artifacts/products-in-play";
const fixture = JSON.parse(
  await readFile(
    "../backend/ads-service/target/products-in-play-fixture.json",
    "utf8",
  ),
);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox", "--disable-dev-shm-usage"],
});
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1100 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const mutations = [],
      errors = [];
    let mode = "ok";
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", async (route) => {
      const path = new URL(route.request().url()).pathname;
      if (!path.startsWith("/api/")) {
        await route.continue();
        return;
      }
      if (route.request().method() !== "GET") {
        mutations.push(route.request().url());
        await route.abort();
        return;
      }
      if (path.endsWith("/operations/v1/in-play")) {
        if (mode === "failure") {
          await route.fulfill({
            status: 503,
            json: { message: "Falha local simulada" },
          });
          return;
        }
        const data =
          mode === "empty"
            ? {
                ...fixture,
                total: 0,
                executing: 0,
                waiting: 0,
                paused: 0,
                blocked: 0,
                products: [],
              }
            : fixture;
        await route.fulfill({ json: data });
        return;
      }
      if (path === "/api/facebook/configuration-status") {
        await route.fulfill({
          json: {
            hasConfiguredPages: true,
            worker: { ready: true, hasAccount: true, messages: [] },
            tokenRenewal: {
              enabledAccounts: 1,
              eligibleAccounts: 1,
              accounts: [],
            },
          },
        });
        return;
      }
      await route.fulfill({ json: [] });
    });
    await page.goto(`${base}/products/in-play`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Produtos em play" }),
    ).toBeVisible();
    await expect(page.locator(".products-play-card")).toHaveCount(4);
    await expect(page.getByText("Executando", { exact: true })).toBeVisible();
    for (const product of fixture.products) {
      await expect(
        page.getByRole("link", {
          name: `Ver atividade de ${product.productName}`,
        }),
      ).toHaveAttribute("href", product.activityUrl);
    }
    await page.locator(".products-play-details summary").first().click();
    await expect(page.locator(".products-play-details[open] p")).toBeVisible();
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({ path: `${output}/${name}.png`, fullPage: true });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth,
      ),
      false,
      "Sem rolagem horizontal",
    );
    await expect(
      page.getByRole("link", { name: "Produtos em play", exact: true }),
    ).toHaveAttribute("href", "/products/in-play");
    mode = "failure";
    await page.getByRole("button", { name: "Atualizar", exact: true }).click();
    await expect(
      page.locator(".products-play-page").getByRole("alert"),
    ).toContainText("última consulta disponível", { timeout: 10000 });
    await expect(page.locator(".products-play-card")).toHaveCount(4);
    mode = "empty";
    await page.getByRole("button", { name: "Atualizar", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "Nenhum produto em play" }),
    ).toBeVisible();
    assert.deepEqual(mutations, [], "Leituras não disparam comandos");
    assert.deepEqual(errors, [], "Sem erros no navegador");
    console.log(
      `${name}: cartões, contexto, detalhes, falha, recuperação, vazio e ausência de mutações aprovados`,
    );
    await context.close();
  }
} finally {
  await browser.close();
}
