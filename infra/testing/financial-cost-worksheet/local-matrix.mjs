import assert from "node:assert/strict";
import { mkdir, writeFile, rm } from "node:fs/promises";
import { spawn } from "node:child_process";
import { createRequire } from "node:module";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:18091";
const api = "/api/business-process-chains/learning-cycles/v1";
const root = new URL("../../../frontend/", import.meta.url);
const evidence = new URL(
  "../../../artifacts/financial-cost-worksheet/",
  import.meta.url,
);
const codes = [
  "AGENTS",
  "PRODUCTION",
  "MEDIA",
  "DELIVERY",
  "SALES_DEDUCTIONS",
  "FIXED_OPERATION",
];
const labels = [
  "Agentes",
  "Produção de imagens, áudio e vídeo",
  "Mídia",
  "Entrega e suporte",
  "Despesas e deduções da venda",
  "Operação fixa",
];
async function http(path, body, status = 200) {
  const response = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const result = await response.json();
  assert.equal(response.status, status, JSON.stringify(result));
  return result;
}
async function cycle(product = 91001) {
  return http(`${api}/products/${product}`, {
    requestKey: crypto.randomUUID(),
    chainDefinitionId: 91002,
    experimentId: product === 91002 ? 91006 : product,
    baseline: false,
    productVersion: "worksheet-local-v1",
    hypothesis: "Planejar os seis grupos com fontes evita custos omitidos",
    mainChange: "Planilha financeira sintética",
    successCriterion: "Seis grupos persistidos e nenhum gasto iniciado",
    audience: "Público sintético",
    offer: "Oferta de teste",
    acquisition: "Nenhum canal produtivo",
    budgetLimitBrl: 100,
    windowStart: new Date(Date.now() - 86400000).toISOString(),
    windowEnd: new Date(Date.now() + 86400000).toISOString(),
    sampleTarget: 10,
    minimumNetSales: 5,
    operatorName: "Homologação local",
  });
}
function input(c) {
  return {
    requestKey: crypto.randomUUID(),
    expectedRevision: c.revision,
    chainDefinitionId: c.chainDefinitionId,
    experimentId: c.experimentId,
    productVersion: c.productVersion,
    periodStart: "2026-11-01",
    periodEnd: "2026-11-30",
    expectedRevenueBrl: 2000,
    usdBrl: 5,
    exchangeSource: "Câmbio fictício",
    exchangeCheckedOn: "2026-10-30",
    operatorName: "Homologação local",
    justification: "Receita sintética e totais por grupo, sem duplicação",
    approveOperatingBudgets: false,
    lines: codes.map((code) => ({
      code,
      planned: 100,
      reportedCost: 50,
      forecast: 80,
      source: "Fonte fictícia local; não são custos reais",
    })),
  };
}
await mkdir(evidence, { recursive: true });
await http("/fixture/reset", {});
const first = await cycle(),
  second = await cycle(91002);
const url = `${api}/products/${first.productId}/${first.id}/cost-worksheet`;
const request = input(first);
const outcomes = await Promise.all(
  Array.from({ length: 4 }, () => http(url, request)),
);
assert(outcomes.every((result) => result.eventId === outcomes[0].eventId));
assert.equal(outcomes[0].history.length, 1);
assert.equal(outcomes[0].projectedResultBrl, 880);
assert.deepEqual(
  outcomes[0].rows.map((r) => r.code),
  codes,
);
await http(url, { ...request, expectedRevenueBrl: 2500 }, 409);
await http(url, { ...request, requestKey: crypto.randomUUID() }, 409);
await http(
  `${api}/products/91002/${first.id}/cost-worksheet?chainId=91002`,
  undefined,
  404,
);
const other = await http(
  `${api}/products/91002/${second.id}/cost-worksheet?chainId=91002`,
);
assert.equal(other.saved, null);
assert.equal(other.history.length, 0);
const approved = await http(url, {
  ...input(first),
  expectedRevision: outcomes[0].revision,
  approveOperatingBudgets: true,
});
assert.equal(approved.rows[3].approved, 100);
const edited = await http(url, {
  ...input(first),
  expectedRevision: approved.revision,
});
assert.equal(edited.rows[3].approved, null);
const cycleAfter = (await http(`${api}/products/91001?chainId=91002`)).find(
  (c) => c.id === first.id,
);
assert.equal(cycleAfter.stage, first.stage);
assert.equal(cycleAfter.budgetLimitBrl, first.budgetLimitBrl);
await writeFile(
  new URL("api.json", evidence),
  JSON.stringify(
    {
      concurrentRequests: outcomes.length,
      eventsAfterReplay: outcomes[0].history.length,
      resultBrl: edited.projectedResultBrl,
      stageUnchanged: true,
      otherProductIsolated: true,
    },
    null,
    2,
  ),
);

const ceilingRequest = {
  requestKey: crypto.randomUUID(),
  expectedRevision: edited.revision,
  chainDefinitionId: first.chainDefinitionId,
  experimentId: first.experimentId,
  productVersion: first.productVersion,
  code: "DELIVERY",
  approved: 125.5,
  operatorName: "Homologação local",
  justification: "Teto sintético com quatro pedidos concorrentes",
  confirmed: true,
};
const ceilingUrl = url.replace(/cost-worksheet$/, "cost-ceilings");
const ceilings = await Promise.all(
  Array.from({ length: 4 }, () => http(ceilingUrl, ceilingRequest)),
);
assert(
  ceilings.every(
    (result) =>
      result.ceilingHistory.length === 1 && result.rows[3].approved === 125.5,
  ),
);
assert(
  ceilings.every(
    (result) =>
      result.ceilingHistory[0].eventId ===
      ceilings[0].ceilingHistory[0].eventId,
  ),
);
await http(ceilingUrl, { ...ceilingRequest, approved: 126 }, 409);
assert.equal(
  (
    await http(
      `${api}/products/91002/${second.id}/cost-worksheet?chainId=91002`,
    )
  ).ceilingHistory.length,
  0,
);
console.log(
  "PASS tetos/API/MySQL: concorrência, recuperação e isolamento entre produtos",
);

if (process.argv.includes("--api-only")) {
  console.log(
    "PASS API/MySQL: concorrência, cálculos, revisão, contexto e limites preservados",
  );
  process.exit(0);
}

const html = new URL("cost-worksheet-test.html", root),
  entry = new URL("cost-worksheet-test.tsx", root);
await writeFile(
  html,
  '<meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/cost-worksheet-test.tsx"></script>',
);
await writeFile(
  entry,
  `import React from 'react';import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import {BrowserRouter} from 'react-router-dom';import 'bootstrap/dist/css/bootstrap.min.css';import Page from './src/pages/financial/ExecutionFinancePage';createRoot(document.getElementById('root')).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><BrowserRouter><Page/></BrowserRouter></QueryClientProvider>);`,
);
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "--config",
    "vite.learning-cycles-local.config.ts",
  ],
  { cwd: root, stdio: "ignore" },
);
let browser;
try {
  for (let i = 0; i < 80; i++) {
    try {
      if ((await fetch("http://127.0.0.1:15173/cost-worksheet-test.html")).ok)
        break;
    } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1050 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    await http("/fixture/reset", {});
    const c = await cycle();
    const context = await browser.newContext(profile),
      page = await context.newPage(),
      errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    const pageUrl = `http://127.0.0.1:15173/cost-worksheet-test.html?productId=${c.productId}&chainId=${c.chainDefinitionId}&cycleId=${c.id}`;
    await page.goto(pageUrl);
    const section = page.getByRole("region", {
      name: "Planilha de custos",
      exact: true,
    });
    await expect(section.getByRole("rowheader")).toHaveCount(6);
    const fields = section.getByRole("textbox", { name: /^Aprovado —/ });
    await expect(fields).toHaveCount(6);
    assert(
      await fields.evaluateAll((items) =>
        items.every((item) => item.closest("td").cellIndex === 1),
      ),
    );
    await expect(section.getByRole("spinbutton")).toHaveCount(0);
    await expect(
      section.getByLabel("Aprovado — Produção de imagens, áudio e vídeo", {
        exact: true,
      }),
    ).toBeEnabled();
    for (const [code, label, index] of [
      ["PRODUCTION", labels[1], 1],
      ["MEDIA", labels[2], 2],
    ]) {
      await section
        .getByLabel(`Aprovado — ${label}`, { exact: true })
        .fill("150,25");
      await section
        .getByRole("button", { name: `Aprovar teto — ${label}`, exact: true })
        .click();
      await expect(
        section.getByText(
          /Esta decisão registra somente o teto de planejamento/,
        ),
      ).toBeVisible();
      await section
        .getByLabel("Responsável pela decisão *", { exact: true })
        .fill("Teste " + name);
      await section
        .getByLabel("Justificativa da decisão *", { exact: true })
        .fill("Decisão sintética antecipada");
      await section.getByRole("checkbox").check();
      await section
        .getByRole("button", {
          name: "Confirmar e registrar teto",
          exact: true,
        })
        .click();
      await expect(
        section.getByText(`Teto de ${label} registrado.`, { exact: false }),
      ).toBeVisible();
      await page.reload();
      await expect(
        section.getByLabel(`Aprovado — ${label}`, { exact: true }),
      ).toHaveValue("150.25");
      const saved = await http(
        `${api}/products/${c.productId}/${c.id}/cost-worksheet?chainId=${c.chainDefinitionId}`,
      );
      assert.equal(saved.rows[index].approvalScope, "PLANNING");
      assert.equal(saved.rows[index].approved, 150.25);
      const finance = await http(
        `${api}/products/${c.productId}/${c.id}/financial-review?chainId=${c.chainDefinitionId}`,
      );
      assert.equal(finance.videoBudgetUsd, null);
      assert.equal(finance.mediaBudgetBrl, 100);
    }
    const agents = section.getByLabel("Aprovado — Agentes", { exact: true });
    await agents.fill("100,50");
    await section
      .getByRole("button", { name: "Aprovar teto — Agentes", exact: true })
      .click();
    await section
      .getByLabel("Responsável pela decisão *", { exact: true })
      .fill("Teste " + name);
    await section
      .getByLabel("Justificativa da decisão *", { exact: true })
      .fill("Teto sintético, sem gasto externo");
    await section.getByRole("checkbox").check();
    let lost = false;
    await page.route("**/cost-ceilings", async (route) => {
      if (route.request().method() === "POST" && !lost) {
        lost = true;
        await route.fetch();
        return route.abort("failed");
      }
      await route.continue();
    });
    await section
      .getByRole("button", { name: "Confirmar e registrar teto", exact: true })
      .click();
    await expect(section.getByRole("alert")).toBeVisible();
    const read = `${api}/products/${c.productId}/${c.id}/cost-worksheet?chainId=${c.chainDefinitionId}`;
    let persisted = await http(read);
    assert.equal(persisted.ceilingHistory.length, 3);
    assert.equal(persisted.rows[0].approved, 100.5);
    await section
      .getByRole("button", { name: "Confirmar e registrar teto", exact: true })
      .click();
    await expect(section.getByText(/Teto de Agentes registrado/)).toBeVisible();
    persisted = await http(read);
    assert.equal(
      persisted.ceilingHistory.length,
      3,
      "Resposta perdida não duplica recibo",
    );
    await page.reload();
    await expect(
      section.getByLabel("Aprovado — Agentes", { exact: true }),
    ).toHaveValue("100.5");
    await section
      .getByLabel("Aprovado — Entrega e suporte", { exact: true })
      .fill("0");
    await section
      .getByRole("button", {
        name: "Aprovar teto — Entrega e suporte",
        exact: true,
      })
      .click();
    await section
      .getByLabel("Responsável pela decisão *", { exact: true })
      .fill("Teste " + name);
    await section
      .getByLabel("Justificativa da decisão *", { exact: true })
      .fill("Sem custo neste cenário sintético");
    await section.getByRole("checkbox").check();
    await section
      .getByRole("button", { name: "Confirmar e registrar teto", exact: true })
      .click();
    await expect(
      section.getByText(/Teto de Entrega e suporte registrado/),
    ).toBeVisible();
    persisted = await http(read);
    assert.equal(persisted.ceilingHistory.length, 4);
    assert.equal(persisted.rows[0].approved, 100.5);
    assert.equal(persisted.rows[3].approved, 0);
    assert.equal(persisted.rows[4].approved, null);
    assert.equal(persisted.rows[3].forecast, null);
    assert.equal(errors.length, 0, errors.join("\n"));
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
      "Página sem overflow horizontal",
    );
    await section.scrollIntoViewIfNeeded();
    await page.screenshot({
      path: new URL(`${name}.png`, evidence).pathname,
      fullPage: true,
    });
    await writeFile(
      new URL(`${name}.json`, evidence),
      JSON.stringify(
        {
          product: c.productId,
          cycle: c.id,
          eventId: persisted.eventId,
          groups: persisted.rows.length,
          result: persisted.projectedResultBrl,
          errors,
          replayEvents: 1,
          approvalEvents: 2,
          monetaryInputsOnlyInApprovedColumn: true,
        },
        null,
        2,
      ),
    );
    console.log(
      `PASS ${name}: gravação, resposta perdida, replay, recarga e aprovação`,
    );
    await context.close();
  }
} finally {
  await browser?.close();
  server.kill("SIGTERM");
  await rm(html, { force: true });
  await rm(entry, { force: true });
}
console.log(
  "PASS API/MySQL: concorrência, cálculos, revisão, contexto e limites preservados",
);
