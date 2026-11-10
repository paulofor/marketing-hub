import assert from "node:assert/strict";
import { readFile, writeFile, rm, mkdir } from "node:fs/promises";
import { spawn } from "node:child_process";
import { chromium, devices, expect } from "@playwright/test";
const root = new URL("../", import.meta.url),
  fixture = JSON.parse(
    await readFile(
      new URL("fixtures/execution-finance.json", import.meta.url),
      "utf8",
    ),
  );
const html = new URL("execution-finance-test.html", root),
  tsx = new URL("execution-finance-test.tsx", root);
await writeFile(
  html,
  '<meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/execution-finance-test.tsx"></script>',
);
await writeFile(
  tsx,
  `import React from 'react';import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import {MemoryRouter,Routes,Route} from 'react-router-dom';import 'bootstrap/dist/css/bootstrap.min.css';import Finance from './src/pages/financial/ExecutionFinancePage';import Card from './src/pages/product/ProductProcessAutomationPanel';createRoot(document.getElementById('root')).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><MemoryRouter initialEntries={['/execution']}><Routes><Route path='/execution' element={<main style={{maxWidth:900,padding:16,margin:'auto'}}><Card productId={700} processId={126} chainId={2700} cycleId={1900} sourceReference='experiment:11200'/></main>}/><Route path='/financial/execution' element={<Finance/>}/></Routes></MemoryRouter></QueryClientProvider>);`,
);
const server = spawn(
  process.execPath,
  ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "15278"],
  { cwd: root, stdio: "ignore" },
);
let browser;
try {
  for (let i = 0; i < 80; i++) {
    try {
      if (
        (await fetch("http://127.0.0.1:15278/execution-finance-test.html")).ok
      )
        break;
    } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const output = new URL("../../artifacts/execution-finance/", import.meta.url);
  await mkdir(output, { recursive: true });
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(profile),
      page = await context.newPage(),
      sent = [],
      errors = [];
    let data = structuredClone(fixture);
    if (name !== "desktop") {
      await context.addInitScript(() => {
        Object.defineProperty(globalThis.crypto, "randomUUID", {
          value: undefined,
          configurable: true,
        });
      });
    }
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (route.request().method() !== "GET") {
        sent.push({ url: url.pathname, method: route.request().method() });
        return route.fulfill({
          status: 500,
          json: { detail: "Teste consultivo: mutação inesperada" },
        });
      }
      if (url.pathname.endsWith("/automation/v1"))
        return route.fulfill({
          json: {
            id: 9500,
            status: "PAUSED",
            productId: 700,
            processDefinitionId: 126,
            chainId: 2700,
            learningCycleId: 1900,
            sourceReference: "experiment:11200",
            currentActivityName: "Planejar",
            currentOwnerName: "Plutus",
            currentActivityId: "finance",
            currentSequence: 1,
            reason: "Limite de preparação precisa de revisão",
            totalActivities: 3,
            completedActivities: 0,
            remainingActivities: 3,
            omittedActivities: 0,
            completionPercentage: 0,
            financialReviewUrl:
              "/financial/execution?productId=700&chainId=2700&cycleId=1900",
          },
        });
      if (url.pathname === "/api/sales-videos/projects")
        return route.fulfill({ json: [] });
      if (url.pathname.endsWith("/single-video-budget"))
        return route.fulfill({
          json: {
            productId: 700,
            chainDefinitionId: 2700,
            cycleId: 1900,
            experimentId: 11200,
            productVersion: fixture.productVersion,
            revision: data.revision,
            scope: "SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1",
            currency: "USD",
            canAuthorize: false,
            blocker: null,
            startsProduction: false,
            guidance: "Sem projeto de vídeo único neste teste.",
            authorizations: [],
          },
        });
      if (
        !url.pathname.startsWith(
          "/api/business-process-chains/learning-cycles/v1/products/700/1900/",
        )
      ) {
        errors.push(`Chamada inesperada: ${url.pathname}`);
        return route.fulfill({
          status: 500,
          json: { detail: "Contrato de teste ausente" },
        });
      }
      if (url.pathname.endsWith("/cost-worksheet")) {
        const worksheet = JSON.parse(
          await readFile(
            new URL("fixtures/cost-worksheet.json", import.meta.url),
            "utf8",
          ),
        );
        worksheet.rows[0].approved = data.authorization?.totalLimitUsd ?? null;
        worksheet.rows[0].estimatedCost = data.knownPreparationUsd;
        if (data.authorization?.carriedForward) {
          worksheet.rows[0].approvalStatus = `Teto acumulado mantido do ciclo #${data.authorization.sourceCycleId} · recibo #${data.authorization.eventId}. Mudar de ciclo não exige nova aprovação do teto nem renova o saldo.`;
        }
        return route.fulfill({ json: worksheet });
      }
      if (url.pathname.endsWith("/financial-review"))
        return route.fulfill({ json: data });
      if (url.pathname.endsWith("/video-budget"))
        return route.fulfill({
          json: {
            productId: 700,
            productName: fixture.productName,
            internalName: "teste",
            chainDefinitionId: 2700,
            processDefinitionId: 1200,
            cycleId: 1900,
            experimentId: 11200,
            productVersion: fixture.productVersion,
            revision: 1,
            stageLabel: "Planejamento",
            status: "AWAITING_LIMIT",
            instruction: "Conferir etapa",
            scope: "TWO_VIDEOS_PRODUCTION_AND_REVIEW",
            currency: "USD",
            canAuthorize: false,
            blocker: "Conclua o planejamento antes de definir vídeos.",
            currentAuthorization: null,
            history: [],
            cycleUrl: fixture.cycleUrl,
            financeUrl: fixture.videoUrl,
          },
        });
      errors.push(`Chamada inesperada: ${url.pathname}`);
      return route.fulfill({
        status: 500,
        json: { detail: "Contrato de teste ausente" },
      });
    });
    await page.goto("http://127.0.0.1:15278/execution-finance-test.html");
    await page
      .getByRole("link", { name: "Ver demonstração financeira" })
      .click();
    await expect(
      page.getByRole("heading", {
        name: "Demonstração financeira",
        exact: true,
      }),
    ).toBeVisible();
    const advice = page.getByRole("region", { name: "Conselho de Plutus" });
    await expect(advice.getByText(/Parecer simulado/).first()).toBeVisible();
    await expect(
      advice.getByText("Preço por venda", { exact: true }),
    ).toBeVisible();
    const details = advice.locator("details");
    assert.equal(await details.getAttribute("open"), null);
    await advice.getByText("Ver parecer completo, cenários e fontes").click();
    await expect(
      advice.getByText(
        `Plano financeiro #${fixture.financialPlan.id}, revisão ${fixture.financialPlan.revision}.`,
      ),
    ).toBeVisible();
    await advice.getByText("Ver parecer completo, cenários e fontes").click();
    await expect(
      page.getByRole("heading", { name: "Preparar o produto", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Produzir vídeos", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Pagar anúncios", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Ver teto na planilha" }),
    ).toHaveCount(3);
    const sheet = page.getByRole("region", {
      name: "Planilha de custos",
      exact: true,
    });
    await expect(
      sheet.getByRole("textbox", { name: /^Aprovado —/ }),
    ).toHaveCount(6);
    assert.equal(sent.length, 0);
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    );
    await page.screenshot({
      path: new URL(`${name}-analysis.png`, output).pathname,
      fullPage: true,
    });
    assert.equal(errors.length, 0, JSON.stringify(errors));
    data.financialPlan = null;
    data.advice = {
      ...data.advice,
      status: "STALE",
      title: "Reavaliar antes de investir mais",
      recommendation:
        "A estratégia mudou. Estes números são referência para reavaliação.",
      basis: "PLANNING",
      figures: [
        { label: "Preço por venda", value: 67, unit: "BRL" },
        { label: "Custos por venda, sem anúncios", value: 41.74, unit: "BRL" },
        {
          label: "Sobra por venda, antes dos anúncios",
          value: 25.26,
          unit: "BRL",
        },
      ],
      caution:
        "Hipótese de planejamento, não aprovação comercial nem lucro líquido.",
      fullRationale:
        "Parecer de planejamento preservado para consulta. Dados de teste.",
    };
    await page.getByRole("button", { name: "Atualizar valores" }).click();
    await expect(
      advice.getByText("Reavaliar antes de investir mais"),
    ).toBeVisible();
    await expect(
      advice.getByText("Custos por venda, sem anúncios", { exact: true }),
    ).toBeVisible();
    assert.equal(
      sent.length,
      0,
      "Consultar o parecer não autoriza novos gastos",
    );
    assert.equal(await details.getAttribute("open"), null);
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    );
    await page.screenshot({
      path: new URL(`${name}-planning-reference.png`, output).pathname,
      fullPage: true,
    });
    data = {
      ...data,
      revision: 3,
      knownPreparationUsd: 67.37689,
      availableUsd: -7.37689,
      authorization: {
        ...data.authorization,
        eventId: 8001,
        totalLimitUsd: 80,
        reserveUsd: 20,
        operatorName: "Operador local",
        approvedAt: fixture.observedAt,
        productVersion: fixture.productVersion,
        sourceCycleId: data.cycleId,
        carriedForward: false,
      },
      preparationBalance: {
        status: "NO_BALANCE",
        title: "Aprovação registrada, mas sem saldo para novas tarefas",
        explanation:
          "O recibo está salvo. Revise o teto total ou a reserva abaixo, ou mantenha a pausa.",
        shortfallUsd: 7.37689,
        requiresBudgetReview: true,
      },
    };
    data.history = [data.authorization, ...data.history];
    data.budgets = data.budgets.map((budget) =>
      budget.code === "PREPARATION" ? { ...budget, limit: 80 } : budget,
    );
    await page.getByRole("button", { name: "Atualizar valores" }).click();
    const balance = page.getByRole("region", {
      name: "Situação da preparação",
    });
    await expect(balance).toContainText("Aprovação registrada, mas sem saldo");
    await expect(balance).toContainText("Recibo #8001");
    await expect(balance).toContainText(
      /80,00 de teto − US\$\s*67,38 de custos estimados − US\$\s*20,00 de reserva = -US\$\s*7,38/,
    );
    await page.getByRole("link", { name: "Rever teto aprovado" }).click();
    await expect(
      page.getByLabel("Aprovado — Agentes", { exact: true }),
    ).toHaveValue("80");
    await expect(
      page.getByRole("button", { name: "Atualizar valores" }),
    ).toBeEnabled();
    assert.equal(
      sent.length,
      0,
      "Conferir insuficiência não repete aprovação nem inicia tarefa",
    );
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    );
    await page.screenshot({
      path: new URL(`${name}-saved-without-balance.png`, output).pathname,
      fullPage: true,
    });
    data.authorization.sourceCycleId = 1899;
    data.authorization.carriedForward = true;
    data.authorization.productVersion = "versao-predecessora";
    data.preparationBalance.title = "Atenção: teto de agentes atingido";
    await page.getByRole("button", { name: "Atualizar valores" }).click();
    await expect(balance).toContainText("Atenção: teto de agentes atingido");
    await expect(balance).toContainText("Recibo #8001 · Ciclo #1899");
    await expect(balance).toContainText(
      "Teto e reserva mantidos do ciclo anterior",
    );
    await expect(
      page.getByRole("link", { name: "Rever teto aprovado" }),
    ).toHaveAttribute("href", "#ceiling-AGENTS");
    assert.equal(
      sent.length,
      0,
      "O sucessor não duplica aprovação nem inicia gasto",
    );
    await expect(
      page.getByRole("button", { name: "Atualizar valores" }),
    ).toBeEnabled();
    assert.equal(errors.length, 0, JSON.stringify(errors));
    await expect(balance).not.toContainText("Invalid Date");
    await page.screenshot({
      path: new URL(`${name}-cumulative-ceiling.png`, output).pathname,
      fullPage: true,
    });
    console.log(
      `${name}: recibo original no sucessor, reserva preservada e atenção ao teto passaram`,
    );
    await context.close();
  }
} finally {
  await browser?.close();
  server.kill();
  await rm(html, { force: true });
  await rm(tsx, { force: true });
}
