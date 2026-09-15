import { createRequire } from "node:module";
import fs from "node:fs/promises";
import assert from "node:assert/strict";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const out =
  process.env.FINANCIAL_PLAN_ARTIFACTS || "/tmp/financial-plan-browser";
await fs.mkdir(out, { recursive: true });
const seed = JSON.parse(
  await fs.readFile(
    "backend/ads-service/src/test/resources/financial-plan/assumptions.json",
    "utf8",
  ),
);
seed.validUntil = new Date(Date.now() + 30 * 86400000)
  .toISOString()
  .slice(0, 10);
const browser = await chromium.launch({
  headless: true,
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
let checks = 0;
let activePage;
async function fillForm(p, input) {
  for (const [name, value] of Object.entries(input)) {
    const f = p.locator(`[name="${name}"]`);
    if ((await f.count()) === 0) continue;
    if ((await f.evaluate((e) => e.tagName)) === "SELECT")
      await f.selectOption(String(value));
    else await f.fill(String(value));
  }
}
try {
  for (const [label, device, id] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }, 95110],
    ["iphone", devices["iPhone 15 Pro"], 95111],
    ["pixel", devices["Pixel 7"], 95112],
  ]) {
    const c = await browser.newContext(device);
    const p = await c.newPage();
    activePage = p;
    const errors = [];
    p.on("pageerror", (e) => errors.push(e.message));
    await p.route("**/api/**", (route) =>
      route.request().url().includes("/api/financial-plans/v1/")
        ? route.continue()
        : route.fulfill({ status: 200, json: [] }),
    );
    await p.goto(`http://127.0.0.1:15175/financial/plans?productId=${id}`, {
      waitUntil: "networkidle",
    });
    await p
      .getByRole("button", { name: "Criar plano do produto", exact: true })
      .click();
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    assert.equal(
      await p
        .locator("input[name=name]")
        .evaluate((e) => e.validity.valueMissing),
      true,
    );
    checks++;
    const input = {
      name: `Plano local ${label}`,
      createdBy: "Operador de homologação",
      commercialPlanId: id,
      ...Object.fromEntries(
        Object.entries(seed).filter(([k, v]) => typeof v !== "object"),
      ),
      ...seed.ai,
      ...seed.costs,
    };
    for (const s of seed.scenarios)
      for (const [k, v] of Object.entries(s))
        if (k !== "code") input[`${s.code}-${k}`] = v;
    await fillForm(p, input);
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    await expect(
      p.getByRole("heading", { name: "Cenários de viabilidade" }),
    ).toBeVisible();
    checks++;
    await expect(
      p.getByText("Uso intenso (limite contratado)", { exact: true }),
    ).toBeVisible();
    await expect(
      p.getByText("Viável em projeção · requer parecer e gates", {
        exact: true,
      }),
    ).toBeVisible();
    checks++;
    await p.reload({ waitUntil: "networkidle" });
    await expect(
      p.getByRole("heading", { name: `Plano local ${label}`, exact: true }),
    ).toBeVisible();
    checks++;
    const overflow = await p.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth + 2,
    );
    assert.equal(overflow, false, `overflow ${label}`);
    checks++;
    if (device.isMobile) {
      const table = p.getByRole("region", {
        name: "Comparação financeira dos cenários",
      });
      await table.scrollIntoViewIfNeeded();
      await table.evaluate((e) => {
        e.scrollLeft = e.scrollWidth;
      });
      assert.ok(await table.evaluate((e) => e.scrollLeft > 0));
      const header = p.getByRole("columnheader", {
        name: "Uso intenso (limite contratado)",
      });
      await header.scrollIntoViewIfNeeded();
      await expect(header).toBeInViewport();
      await table.evaluate((e) => {
        e.scrollLeft = 0;
      });
      checks++;
    }
    await p.evaluate(() => window.scrollTo(0, 0));
    await p.screenshot({ path: `${out}/${label}-plan.png`, fullPage: true });
    await p
      .getByRole("button", { name: "Solicitar parecer de Plutus" })
      .click();
    await expect(p.getByText(/Parecer #\d+ · PENDING/)).toBeVisible();
    await expect(
      p.getByRole("button", { name: "Solicitar parecer de Plutus" }),
    ).toBeDisabled();
    checks++;
    const list = await (
      await p.request.get(
        `http://127.0.0.1:18095/api/financial-plans/v1/products/${id}`,
      )
    ).json();
    await p.request.post(
      `http://127.0.0.1:18095/fixture/reviews/${list[0].analysis.executionId}/COMPLETED`,
    );
    await expect(p.getByText(/Parecer #\d+ · COMPLETED/)).toBeVisible({
      timeout: 12000,
    });
    checks++;
    await expect(
      p.getByText(/Parecer simulado: premissas revisadas/),
    ).toBeVisible();
    await expect(
      p.getByRole("heading", { name: "Parar novos compromissos" }),
    ).toBeVisible();
    await expect(
      p.getByText("Teto sugerido por ciclo", { exact: true }),
    ).toBeVisible();
    await expect(
      p.getByText(
        "Parar novos compromissos se a contribuição após CAC ficar não positiva.",
        { exact: true },
      ),
    ).toBeVisible();
    await p
      .getByText("Premissas e projeções de Plutus por cenário", { exact: true })
      .click();
    await expect(
      p.getByText(/Uso intenso: oito tentativas por cliente/),
    ).toBeVisible();
    checks += 4;
    await p.getByRole("button", { name: "Criar nova revisão" }).click();
    await p
      .getByLabel("Preço por cliente/pacote (R$)", { exact: true })
      .fill("1");
    await p
      .getByLabel("Responsável pelo registro")
      .fill("Operador de homologação");
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    await expect(
      p.getByText("Revisar viabilidade", { exact: true }),
    ).toBeVisible();
    checks++;
    const revisions = await (
      await p.request.get(
        `http://127.0.0.1:18095/api/financial-plans/v1/products/${id}`,
      )
    ).json();
    await p
      .getByLabel("Histórico de revisões")
      .selectOption(String(revisions[1].id));
    await expect(p.getByText(/Parecer #\d+ · COMPLETED/)).toBeVisible();
    checks++;
    await p.getByLabel("Dados", { exact: true }).selectOption("TEST");
    await expect(
      p.getByRole("button", { name: "Criar plano do produto", exact: true }),
    ).toBeVisible();
    checks++;
    assert.equal(
      await p.getByRole("heading", { name: "Cenários de viabilidade" }).count(),
      0,
    );
    checks++;
    await p.goto(
      "http://127.0.0.1:15175/financial/plans?typeId=951&environment=TEST",
      { waitUntil: "networkidle" },
    );
    await p
      .getByRole("button", { name: /^Criar (modelo do tipo|nova revisão)$/ })
      .click();
    await fillForm(p, {
      ...input,
      name: `Modelo local ${label}`,
      evidence: `${seed.evidence} Modelo ${label}.`,
    });
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    await expect(
      p.getByRole("heading", { name: `Modelo local ${label}`, exact: true }),
    ).toBeVisible();
    checks++;
    assert.equal(
      await p
        .getByRole("button", { name: "Solicitar parecer de Plutus" })
        .count(),
      0,
    );
    checks++;
    await p.goto(
      `http://127.0.0.1:15175/financial/plans?productId=${id}&environment=TEST`,
      { waitUntil: "networkidle" },
    );
    await p
      .getByRole("button", {
        name: new RegExp(`^Usar modelo: Modelo local ${label} · revisão`),
      })
      .click();
    await expect(
      p.getByLabel("Versão do produto", { exact: true }),
    ).toHaveValue("");
    await expect(
      p.getByLabel("Preço por cliente/pacote (R$)", { exact: true }),
    ).toHaveValue(String(seed.priceBrl));
    checks++;
    await fillForm(p, {
      name: `Cópia ${label}`,
      createdBy: "Homologação",
      productVersion: `pacote-${label}-v1`,
      commercialPlanId: id,
    });
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    await expect(p.getByText(/Modelo de origem: #\d+/)).toBeVisible();
    checks++;
    await expect(
      p.getByRole("button", { name: "Solicitar parecer de Plutus" }),
    ).toBeDisabled();
    checks++;
    await p.getByLabel("Produto", { exact: true }).selectOption("95102");
    await expect(
      p.getByRole("button", { name: "Criar plano do produto", exact: true }),
    ).toBeVisible();
    checks++;
    await p.route("**/api/financial-plans/v1/products/*", (r) =>
      r.fulfill({
        status: 503,
        json: { detail: "Falha sintética temporária" },
      }),
    );
    await p.reload({ waitUntil: "networkidle" });
    await expect(
      p
        .getByRole("alert")
        .filter({ hasText: "Não foi possível carregar o plano financeiro" }),
    ).toBeVisible({ timeout: 20000 });
    checks++;
    assert.deepEqual(errors, []);
    await c.close();
  }
  console.log(
    JSON.stringify({ checks, devices: 3, result: "BROWSER_MATRIX_PASS" }),
  );
} catch (error) {
  if (activePage && !activePage.isClosed()) {
    await activePage.screenshot({ path: `${out}/failure.png`, fullPage: true });
    console.error(await activePage.locator("body").innerText());
  }
  throw error;
} finally {
  await browser.close();
}
