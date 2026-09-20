import { createRequire } from "node:module";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:18095/api/financial-plans/v1";
const out =
  process.env.FINANCIAL_PLAN_ARTIFACTS || "/tmp/financial-preparation-browser";
await fs.mkdir(out, { recursive: true });
let checks = 0;
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [label, device, id] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }, 95116],
    ["iphone", devices["iPhone 15 Pro"], 95117],
    ["pixel", devices["Pixel 7"], 95118],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", (route) =>
      route.request().url().includes("/api/financial-plans/v1/")
        ? route.continue()
        : route.fulfill({ status: 200, json: [] }),
    );
    await page.goto(
      `http://127.0.0.1:15175/financial/plans?productId=${id}&environment=TEST`,
      { waitUntil: "networkidle" },
    );
    await page
      .getByRole("button", { name: "Criar plano do produto", exact: true })
      .click();
    await expect(
      page.getByLabel("Período de suporte (dias)", { exact: true }),
    ).toHaveValue("7");
    await expect(
      page.getByLabel("Geração personalizada com IA", { exact: true }),
    ).toHaveValue("true");
    assert.equal(
      await page.locator("form input, form select, form textarea").count(),
      2,
    );
    checks += 3;
    for (const invalid of ["", "0", "-1", "1.5", "3661"]) {
      await page
        .getByLabel("Período de suporte (dias)", { exact: true })
        .fill(invalid);
      assert.equal(
        await page
          .getByLabel("Período de suporte (dias)", { exact: true })
          .evaluate((e) => e.checkValidity()),
        false,
      );
      checks++;
    }
    await page
      .getByLabel("Período de suporte (dias)", { exact: true })
      .fill("14");
    await page
      .getByLabel("Geração personalizada com IA", { exact: true })
      .selectOption("false");
    const path = `${base}/products/${id}/preparation?environment=TEST`;
    const preview = await (await page.request.get(path)).json();
    await page.route("**/preparation?environment=TEST", async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      await new Promise((resolve) => setTimeout(resolve, 600));
      await route.fulfill({
        status: 503,
        json: { detail: "Falha sintética recuperável" },
      });
    });
    await page
      .getByRole("button", { name: "Salvar preparação e calcular" })
      .click();
    await expect(
      page.getByRole("button", { name: "Salvando..." }),
    ).toBeDisabled();
    await expect(
      page
        .getByRole("alert")
        .filter({ hasText: "Falha sintética recuperável" }),
    ).toHaveText("Falha sintética recuperável");
    await expect(
      page.getByLabel("Período de suporte (dias)", { exact: true }),
    ).toHaveValue("14");
    await page.unroute("**/preparation?environment=TEST");
    checks += 3;
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 2,
      ),
      false,
    );
    await page.screenshot({
      path: `${out}/${label}-two-choices.png`,
      fullPage: true,
    });
    await page
      .getByRole("button", { name: "Salvar preparação e calcular" })
      .click();
    await expect(
      page.getByText("Preparação salva.", { exact: false }),
    ).toBeVisible();
    await page.reload({ waitUntil: "networkidle" });
    await expect(
      page.getByText("Suporte: 14 dias · geração personalizada com IA: Não."),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Solicitar parecer de Plutus" }),
    ).toBeDisabled();
    const history = await (
      await page.request.get(`${base}/products/${id}?environment=TEST`)
    ).json();
    assert.equal(history.length, 1);
    assert.equal(history[0].assumptions.preparation.supportDays, 14);
    assert.equal(history[0].assumptions.periodDays, 30);
    assert.equal(history[0].assumptions.ai.perAttempt, 0);
    assert.equal(history[0].assumptions.costs.initialAiBrl, null);
    assert.equal(history[0].evaluation.status, "MISSING_INPUTS");
    assert.deepEqual(
      await (
        await page.request.get(`${base}/products/${id}?environment=LIVE`)
      ).json(),
      [],
    );
    checks += 10;
    const stale = await page.request.post(path, {
      data: { ...preview, supportDays: 7, personalizedAi: true },
    });
    assert.equal(stale.status(), 409);
    await page.getByRole("button", { name: "Criar nova revisão" }).click();
    await expect(
      page.getByLabel("Geração personalizada com IA", { exact: true }),
    ).toHaveValue("false");
    await page
      .getByLabel("Geração personalizada com IA", { exact: true })
      .selectOption("true");
    await page
      .getByRole("button", { name: "Salvar preparação e calcular" })
      .click();
    await expect(
      page.getByText("Suporte: 14 dias · geração personalizada com IA: Sim."),
    ).toBeVisible();
    const changed = await (
      await page.request.get(`${base}/products/${id}?environment=TEST`)
    ).json();
    assert.equal(changed.length, 2);
    assert.equal(changed[0].assumptions.ai.perAttempt, null);
    assert.equal(changed[1].assumptions.preparation.personalizedAi, false);
    assert.deepEqual(errors, []);
    checks += 6;
    await context.close();
  }
  console.log(
    JSON.stringify({
      result: "PASS",
      checks,
      devices: ["desktop", "iPhone 15 Pro", "Pixel 7"],
      integration: "API + MySQL 5.7 locais; dados TEST",
    }),
  );
} finally {
  await browser.close();
}
