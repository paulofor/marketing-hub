import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const fixture = JSON.parse(
  await readFile(process.env.PLUTUS_RESUME_FIXTURE, "utf8"),
);
const financeSource = JSON.parse(
  await readFile(
    new URL(
      "../../../frontend/e2e/fixtures/execution-finance.json",
      import.meta.url,
    ),
    "utf8",
  ),
);
const sheetSource = JSON.parse(
  await readFile(
    new URL(
      "../../../frontend/e2e/fixtures/cost-worksheet.json",
      import.meta.url,
    ),
    "utf8",
  ),
);
const base = process.env.PROCESS_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["localhost", "127.0.0.1"].includes(new URL(base).hostname));
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output, "Informe o destino das evidências locais.");
assert.equal(fixture.blocked.canResume, false);
assert.equal(fixture.eligible.canResume, true);
assert.equal(fixture.eligible.status, "PAUSED");
await mkdir(output, { recursive: true });

const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [profile, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const page = await browser.newPage(options);
    let approved = false;
    let failSave = true;
    const errors = [],
      commands = [];
    const blocked = structuredClone(fixture.blocked);
    const history = structuredClone(fixture.history);
    const reportTask = {
      taskId: 99100,
      processDefinitionId: 126,
      processVersionNumber: 13,
      title: "Parecer de Plutus · TEST",
      status: "COMPLETED",
      sourceReference: "experiment:112",
      assignedAgentKey: "financial-agent",
      assignedAgentNickname: "Plutus",
      comments:
        "Análise original preservada integralmente · TEST. Esta referência histórica não aprova o ciclo atual.",
      evidenceJson: null,
      estimatedCostUsd: 0.75,
      costEstimationStatus: "ESTIMATED",
      createdAt: "2026-11-01T12:00:00Z",
      startedAt: "2026-11-01T12:00:00Z",
      finishedAt: "2026-11-01T12:01:00Z",
      modelCode: "SIMULATED",
      executionMode: "MODEL",
      promptSent: "Fixture TEST sem inferência",
      accessedUrls: [],
      visualEvidence: [],
    };
    page.on("pageerror", (error) => errors.push(error.message));
    await page.routeWebSocket("**/*", (socket) => socket.close());
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      const path = url.pathname;
      if (!path.startsWith("/api/")) {
        if (url.origin !== base) return route.abort();
        return route.continue();
      }
      if (!["GET", "OPTIONS"].includes(request.method())) commands.push(path);
      if (path.endsWith("/cost-ceilings")) {
        const body = request.postDataJSON();
        assert.equal(body.code, "AGENTS");
        assert.equal(
          body.productVersion,
          blocked.financialPending.productVersion,
        );
        assert.equal(body.experimentId, 113);
        assert.equal(body.chainDefinitionId, 27);
        assert.equal(body.approved, 150);
        if (failSave)
          return route.fulfill({
            status: 409,
            json: { detail: "SQL stack trace interno de teste" },
          });
        approved = true;
        const sheet = structuredClone(sheetSource);
        sheet.revision = 2;
        sheet.rows[0].approved = 150;
        return route.fulfill({ json: sheet });
      }
      if (path.includes("/automation/v1")) {
        assert.equal(
          request.method(),
          "GET",
          "Navegação/aprovação não pode retomar automaticamente.",
        );
        return route.fulfill({ json: approved ? fixture.eligible : blocked });
      }
      if (path.endsWith("/activity-executions"))
        return route.fulfill({ json: history });
      if (path.includes("/tasks/") && path.endsWith("/audit"))
        return route.fulfill({ json: reportTask });
      if (path.endsWith("/process-context"))
        return route.fulfill({ status: 204, body: "" });
      if (path.includes("value-chain-positions"))
        return route.fulfill({
          json: {
            productId: 7,
            chainDefinitionId: 27,
            processDefinitionId: 126,
            sequenceNumber: 2,
            processMeasurements: [],
            resolutionStatus: "IDENTIFIED",
          },
        });
      if (path.endsWith("/cost-worksheet")) {
        const sheet = structuredClone(sheetSource);
        sheet.rows[0].approved = approved ? 150 : null;
        sheet.rows[0].estimatedCost = 76.3465608;
        return route.fulfill({ json: sheet });
      }
      if (path.endsWith("/financial-review")) {
        const finance = structuredClone(financeSource);
        Object.assign(finance, {
          productId: 7,
          productName: "Capella · TEST",
          chainId: 27,
          cycleId: 20,
          experimentId: 113,
          productVersion: blocked.financialPending.productVersion,
          knownPreparationUsd: 76.3465608,
          unreportedTasks: 0,
          financialPlan: null,
          availableUsd: approved ? 53.6534392 : null,
          advice: {
            status: "UNAVAILABLE",
            title: "Sem parecer disponível",
            recommendation: "Ainda não há parecer de Plutus neste ciclo.",
            figures: [],
            sourceId: null,
            caution: "Teste local; não comprova aprovação comercial.",
            fullRationale: null,
          },
          preparationBalance: approved
            ? {
                status: "BALANCE_AVAILABLE",
                title: "Limite registrado · TEST",
                explanation: "Cobertura de teste conferida.",
                requiresBudgetReview: false,
              }
            : finance.preparationBalance,
        });
        return route.fulfill({ json: finance });
      }
      if (path.includes("video-review/summary"))
        return route.fulfill({ json: { humanDecisionRequired: 0 } });
      return route.fulfill({ json: [] });
    });
    const activityUrl = `${base}/products/7/value-chain-history/processes/126/activities?learningCycleId=20&chainId=27#activity-economics`;
    await page.goto(activityUrl);
    const activity = page.locator("#activity-economics");
    const pending = activity
      .getByRole("region", { name: "Pendência financeira da atividade" })
      .first();
    const adjustment = pending.getByRole("link", {
      name: "Ajustar limite de preparação",
    });
    await expect(adjustment).toBeVisible({ timeout: 20000 });
    await expect(pending).toContainText(
      "Falta aprovar o limite de preparação deste ciclo",
    );
    await expect(pending).toContainText("Você · responsável");
    await expect(adjustment).toHaveAttribute(
      "href",
      "/financial/execution?productId=7&chainId=27&cycleId=20#ceiling-AGENTS",
    );
    await expect(
      page.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    await activity.screenshot({
      path: `${output}/${profile}-plutus-pending.png`,
    });
    await pending.evaluate((element) => {
      const header = document.querySelector(".main-navigation");
      const offset =
        innerWidth < 768 ? (header?.getBoundingClientRect().height ?? 0) : 0;
      scrollTo({
        top: scrollY + element.getBoundingClientRect().top - offset - 12,
        behavior: "instant",
      });
    });
    const visibility = await pending
      .locator("strong")
      .first()
      .evaluate((element) => {
        const rect = element.getBoundingClientRect();
        const hit = document.elementFromPoint(
          rect.left + rect.width / 2,
          rect.top + rect.height / 2,
        );
        return {
          visible: hit !== null && element.closest("section")?.contains(hit),
          top: rect.top,
          bottom: rect.bottom,
          scrollY,
          viewportHeight: innerHeight,
          header: document
            .querySelector(".main-navigation")
            ?.getBoundingClientRect()
            .toJSON(),
          coveringElement: hit?.className,
        };
      });
    await page.screenshot({ path: `${output}/${profile}-plutus-notice.png` });
    assert(
      visibility.visible,
      `Mensagem financeira sobreposta: ${JSON.stringify(visibility)}`,
    );
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: `${output}/${profile}-plutus-page.png`,
      fullPage: true,
    });
    await adjustment.click();
    await expect(page.getByLabel("Aprovado — Agentes")).toBeFocused();
    await page.getByLabel("Aprovado — Agentes").fill("150");
    await page
      .getByRole("link", { name: "Voltar à atividade e conferir retomada" })
      .click();
    await expect(
      page.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    assert.equal(commands.length, 0, "Visitar e digitar não é aprovação.");
    await page
      .locator("#activity-economics")
      .getByRole("link", { name: "Ajustar limite de preparação" })
      .first()
      .click();
    await page.getByLabel("Aprovado — Agentes").fill("150");
    await page
      .getByRole("button", { name: "Aprovar teto — Agentes", exact: true })
      .click();
    await page.getByLabel("Responsável pela decisão *").fill("Operador TEST");
    await page
      .getByLabel("Justificativa da decisão *")
      .fill("Teste isolado, sem orçamento real");
    await page.getByRole("checkbox").check();
    await page
      .getByRole("button", { name: "Confirmar e registrar teto" })
      .click();
    await expect(
      page
        .getByRole("region", { name: "Planilha de custos" })
        .getByRole("alert"),
    ).toContainText("O contexto financeiro mudou");
    await expect(page.getByText(/SQL stack trace/)).toHaveCount(0);
    assert.equal(approved, false);
    failSave = false;
    await page
      .getByRole("button", { name: "Confirmar e registrar teto" })
      .click();
    await expect(page.getByText(/Teto de Agentes registrado/)).toBeVisible();
    await page
      .getByRole("link", { name: "Voltar à atividade e conferir retomada" })
      .click();
    await expect(
      page.getByRole("button", { name: "Retomar processo" }),
    ).toBeVisible();
    await expect(
      page.getByRole("region", { name: "Execução automática do processo" }),
    ).toContainText("Pausado");
    assert(commands.every((path) => path.endsWith("/cost-ceilings")));
    await page
      .getByRole("region", { name: "Execução automática do processo" })
      .screenshot({ path: `${output}/${profile}-plutus-eligible.png` });
    approved = false;
    history.activities[0].tasks = [reportTask];
    history.activities[0].taskCount = 1;
    await page.reload();
    await expect(
      page.getByText(
        "Histórico de outra execução; não aprova nem libera este ciclo.",
      ),
    ).toBeVisible();
    const report = page
      .locator(".business-process-document")
      .filter({ hasText: reportTask.title });
    await report.locator("summary").first().click();
    await expect(report).toContainText(reportTask.comments);
    await expect(
      report.getByRole("link", { name: "Ajustar limite de preparação" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: `${output}/${profile}-plutus-report.png`,
      fullPage: true,
    });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    console.log(
      `PASS ${profile}: pendência contextual, rascunho, erro seguro, aprovação, retorno e nenhuma retomada automática`,
    );
    await page.close();
  }
} finally {
  await browser.close();
}
