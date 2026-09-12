import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173";
const api = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.VIDEO_FINANCE_EVIDENCE_DIR || "artifacts/video-finance/browser";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1050 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const cycle = JSON.parse(
      execFileSync(
        "python3",
        ["infra/testing/video-finance/validate.py", "--prepare"],
        { encoding: "utf8" },
      ),
    );
    const context = await browser.newContext({ ...profile, timezoneId: "UTC" });
    const page = await context.newPage();
    const errors = [],
      external = [],
      posts = [];
    let delayed = false;
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (request) => {
      if (request.method() === "POST") posts.push(request.url());
    });
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.href);
        return route.abort();
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(api) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      if (
        delayed &&
        route.request().method() === "POST" &&
        url.pathname.endsWith("/video-budget")
      )
        await new Promise((resolve) => setTimeout(resolve, 600));
      return route.continue();
    });
    const cycleUrl = `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`;
    await page.goto(cycleUrl);
    await page
      .getByRole("link", { name: "Financeiro dos vídeos", exact: true })
      .click();
    await expect(
      page.getByRole("heading", { name: "Financeiro de vídeos", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: `Voltar ao ciclo #${cycle.id}` }),
    ).toHaveAttribute(
      "href",
      `/business-process-chains/learning-cycles?chainId=91002&productId=91001&cycleId=${cycle.id}`,
    );
    const form = () =>
      page.getByRole("form", { name: "Autorizar teto dos vídeos" });
    await expect(form().locator('[name="budgetLimitUsd"]')).toHaveValue("");
    await expect(form().getByRole("checkbox")).not.toBeChecked();
    const values = {
      budgetLimitUsd: "0",
      operatorName: "Homologação local",
      justification:
        "Demonstrar o primeiro ajuste e testar vendas sem dados produtivos.",
    };
    for (const [key, value] of Object.entries(values))
      await form().locator(`[name="${key}"]`).fill(value);
    await form().getByRole("checkbox").check();
    await form()
      .getByRole("button", { name: "Registrar teto autorizado" })
      .click();
    await expect(form().getByRole("alert")).toContainText("teto positivo");
    assert.equal(posts.length, 0);
    await form().locator('[name="budgetLimitUsd"]').fill("20,50");
    delayed = true;
    await form()
      .getByRole("button", { name: "Registrar teto autorizado" })
      .click();
    await expect(
      form().getByRole("button", { name: "Registrar teto autorizado" }),
    ).toBeDisabled();
    await expect(
      page.getByText("Teto disponível para avaliação financeira."),
    ).toBeVisible();
    assert.equal(posts.length, 1);
    await page.reload();
    await expect(page.getByText(/Teto registrado:.*20,50/)).toBeVisible();
    await expect(
      page.getByText("Vigente nesta versão", { exact: false }),
    ).toBeVisible();
    const width = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      view: innerWidth,
    }));
    assert(
      width.scroll <= width.view + 1,
      `${name}: overflow ${JSON.stringify(width)}`,
    );
    await page.screenshot({
      path: `${output}/${name}-finance.png`,
      fullPage: true,
    });
    await page
      .getByRole("link", { name: "Continuar no ciclo com este teto" })
      .click();
    const reference = page.locator('input[name="productionBudgetReference"]');
    await expect(reference).toHaveValue(
      new RegExp(`internal://learning-cycles/${cycle.id}/video-budget/`),
    );
    await expect(reference).toHaveAttribute("readonly", "");
    const parent = page.getByRole("link", {
      name: /Voltar à atividade 4 do Processo 6/,
    });
    await expect(parent).toHaveAttribute(
      "href",
      new RegExp(`learningCycleId=${cycle.id}`),
    );
    const persisted = await (
      await fetch(
        `http://127.0.0.1:18091${api}/products/91001/${cycle.id}/video-budget?chainId=91002`,
      )
    ).json();
    assert.equal(persisted.history.length, 1);
    assert.equal(persisted.currentAuthorization.budgetLimitUsd, 20.5);
    assert.equal(persisted.stageLabel, "Definir os dois vídeos");
    await page.goto(
      `${base}/financial/videos?productId=91001&chainId=91001&cycleId=${cycle.id}`,
    );
    await expect(
      page.locator(".video-finance-page").getByRole("alert"),
    ).toContainText("cadeia informada", { timeout: 12000 });
    await expect(page.getByRole("form")).toHaveCount(0);
    await page.goto(`${base}/financial/videos`);
    await page
      .getByRole("combobox", { name: "Produto *", exact: true })
      .selectOption("91002");
    await expect(page.getByText(/Nenhum ciclo registrado/)).toBeVisible();
    await expect(page.getByRole("form")).toHaveCount(0);
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    results.push({
      profile: name,
      savedTotalUsd: 20.5,
      posts: posts.length,
      overflow: false,
      checks: 12,
      pageErrors: errors,
      externalCalls: external,
    });
    await context.close();
  }
} finally {
  await browser.close();
}
await writeFile(`${output}/result.json`, JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
