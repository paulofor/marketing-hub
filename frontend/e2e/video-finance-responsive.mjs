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
        ["infra/testing/video-finance/validate.py", "--prepare-process"],
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
        !url.pathname.startsWith("/api/business-processes/") &&
        !url.pathname.startsWith("/api/products/value-chain-positions/") &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      if (
        delayed &&
        route.request().method() === "POST" &&
        url.pathname.endsWith("/cost-ceilings")
      )
        await new Promise((resolve) => setTimeout(resolve, 600));
      return route.continue();
    });
    const cycleUrl = `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`;
    const processUrl = `${base}/products/91001/value-chain-history/processes/${cycle.automation.processDefinitionId}/activities?learningCycleId=${cycle.id}&chainId=91002#process-execution`;
    await page.goto(processUrl);
    const panel = () =>
      page.getByRole("region", { name: "Execução automática do processo" });
    await expect(
      panel().getByText("Precisa da sua decisão", { exact: true }),
    ).toBeVisible();
    await expect(panel().getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(
      panel().locator(".product-process-situation__running-icon"),
    ).toHaveCount(0);
    await expect(
      panel().getByRole("link", { name: "Abrir pendência", exact: true }),
    ).toHaveCount(0);
    await panel().getByText("Ver contexto completo", { exact: true }).click();
    await expect(
      panel().getByLabel("Contexto completo do processo", { exact: true }),
    ).toContainText(
      "Próxima ação necessária: Falta informar o teto dos dois vídeos",
    );
    await panel().getByText("Ver contexto completo", { exact: true }).click();
    await page.screenshot({
      path: `${output}/${name}-process-pending.png`,
      fullPage: true,
    });
    await page
      .getByRole("link", { name: "Informar teto dos vídeos", exact: true })
      .click();
    if (new URL(page.url()).pathname === "/financial/videos") {
      await page
        .getByRole("link", { name: "Decidir teto na planilha" })
        .click();
    }
    await expect(
      page.getByRole("heading", {
        name: "Demonstração financeira",
        exact: true,
      }),
    ).toBeVisible();
    const sheet = page.getByRole("region", {
      name: "Planilha de custos",
      exact: true,
    });
    const amount = sheet.getByLabel(
      "Aprovado — Produção de imagens, áudio e vídeo",
      { exact: true },
    );
    await expect(amount).toHaveValue("");
    assert.equal(posts.length, 0);
    await amount.fill("20,50");
    await sheet
      .getByRole("button", {
        name: "Aprovar teto — Produção de imagens, áudio e vídeo",
      })
      .click();
    const form = () => sheet.getByRole("form");
    await expect(form().getByRole("checkbox")).not.toBeChecked();
    await form()
      .getByLabel("Responsável pela decisão *")
      .fill("Homologação local");
    await form()
      .getByLabel("Justificativa da decisão *")
      .fill("Dados sintéticos sem produção paga");
    await form().getByRole("checkbox").check();
    delayed = true;
    await form()
      .getByRole("button", { name: "Confirmar e registrar teto" })
      .click();
    await expect(
      form().getByRole("button", { name: "Registrando…" }),
    ).toBeDisabled();
    await expect(
      sheet.getByText(/Teto de Produção de imagens, áudio e vídeo registrado/),
    ).toBeVisible();
    assert.equal(posts.length, 1);
    await page.reload();
    await expect(amount).toHaveValue("20.5");
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
      .getByRole("link", { name: "Voltar ao ciclo", exact: true })
      .click();
    await expect(page.getByRole("link", { name: "Ver atividade e pendência" })).toBeVisible();
    await page.goto(processUrl);
    await expect(
      panel().getByText("Teto registrado. Falta definir os vídeos", {
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      panel().getByRole("link", { name: "Continuar definição dos vídeos" }),
    ).toHaveAttribute("href", new RegExp(`cycleId=${cycle.id}`));
    await expect(panel().getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(
      panel().getByRole("link", { name: "Informar teto dos vídeos" }),
    ).toHaveCount(0);
    await page.screenshot({
      path: `${output}/${name}-process-after-budget.png`,
      fullPage: true,
    });
    const bounds = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      view: innerWidth,
    }));
    assert(
      bounds.scroll <= bounds.view + 1,
      `${name}: overflow do processo ${JSON.stringify(bounds)}`,
    );
    await panel()
      .getByRole("link", { name: "Continuar definição dos vídeos" })
      .click();
    const persisted = await (
      await fetch(
        `http://127.0.0.1:18091${api}/products/91001/${cycle.id}/video-budget?chainId=91002`,
      )
    ).json();
    assert.match(persisted.currentAuthorization.reference, new RegExp(`internal://learning-cycles/${cycle.id}/video-budget/`));
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
      checks: [
        "Processo identifica espera humana sem spinner",
        "Contexto copiado inclui a decisão",
        "Link direto ao financeiro preserva ciclo",
        "Confirma teto da produção na planilha central",
        "Gravação única com indicador de carregamento",
        "Recibo persiste após recarga",
        "Retorno ao processo atualiza próxima ação",
        "Recibo oficial preserva referência do briefing",
        "Outro contexto e produto sem ciclo não oferecem formulário",
        "Sem consumo externo, erro de página ou overflow",
      ],
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
