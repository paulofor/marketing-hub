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
      assert(
        url.pathname.startsWith(
          "/api/business-process-chains/learning-cycles/v1/products/700/1900/",
        ),
      );
      if (url.pathname.endsWith("/financial-review"))
        return route.fulfill({ json: data });
      assert(url.pathname.endsWith("/preparation-budget"));
      const request = route.request().postDataJSON();
      sent.push(request);
      if (sent.length === 1)
        return route.fulfill({
          status: 503,
          json: { detail: "Falha temporária simulada; decisão não duplicada." },
        });
      data = {
        ...data,
        revision: 2,
        availableUsd: 7.61,
        authorization: {
          eventId: 8000,
          reference: "internal://local/receipt/8000",
          totalLimitUsd: 50,
          reserveUsd: 3.5,
          operatorName: request.operatorName,
          justification: request.justification,
          approvedAt: fixture.observedAt,
          productVersion: fixture.productVersion,
          current: true,
        },
      };
      data.history = [data.authorization];
      return route.fulfill({ json: data });
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
    await page
      .getByLabel("Teto total acumulado (US$) *", { exact: true })
      .fill("50");
    await page
      .getByLabel("Reserva para custos pendentes (US$) *", { exact: true })
      .fill("3.50");
    await page
      .getByLabel("Responsável pela decisão *", { exact: true })
      .fill("Administrador de teste");
    await page
      .getByLabel("Justificativa e fontes consideradas *", { exact: true })
      .fill("Cenários simulados. Sem gasto externo.");
    await expect(
      page.getByRole("button", { name: "Aprovar limite de preparação" }),
    ).toBeDisabled();
    await page.getByRole("checkbox").check();
    await page
      .getByRole("button", { name: "Aprovar limite de preparação" })
      .click();
    await expect(page.getByRole("alert")).toContainText(
      "Falha temporária simulada",
    );
    await page
      .getByRole("button", { name: "Aprovar limite de preparação" })
      .click();
    await expect(page.getByText(/Decisão registrada com recibo/)).toBeVisible();
    assert.equal(sent.length, 2);
    assert.deepEqual(sent[0], sent[1]);
    assert.equal(sent[1].totalLimitUsd, 50);
    assert.equal(errors.length, 0, JSON.stringify(errors));
    await page.screenshot({
      path: new URL(`${name}-approved.png`, output).pathname,
      fullPage: true,
    });
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
      2,
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
    console.log(
      `${name}: card, gráficos, parecer vigente, referência histórica sem plano e aprovação recuperável passaram`,
    );
    await context.close();
  }
} finally {
  await browser?.close();
  server.kill();
  await rm(html, { force: true });
  await rm(tsx, { force: true });
}
