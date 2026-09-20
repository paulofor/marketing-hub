import assert from "node:assert/strict";
import { mkdir, rm, writeFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import { chromium, devices, expect } from "@playwright/test";

const frontendRoot = new URL("../", import.meta.url);
const harnessHtml = new URL("experiment-sample-decision-test.html", frontendRoot);
const harnessTsx = new URL("experiment-sample-decision-test.tsx", frontendRoot);
const baseUrl = "http://127.0.0.1:15392";
const evidenceDirectory = "/tmp/experiment-sample-decision-responsive";

const cockpit = {
  experimentId: 92,
  experimentName: "MUSA-H003-E003",
  status: "RUNNING",
  experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
  campaignObjective: "SALES",
  scoreboard: {
    spend: 5.89,
    revenue: 0,
    margin: -5.89,
    roas: 0,
    impressions: 120,
    clicks: 4,
    ctr: 3.33,
    cpc: 1.47,
    directContacts: 0,
    directContactTarget: 0,
    humanVisitors: 4,
    pageViews: 4,
    partialVideoViews: 2,
    completeVideoViews: 0,
    leads: 0,
    checkoutAccesses: 0,
    purchases: 0,
    costPerLead: null,
    costPerCheckoutAccess: null,
    costPerPurchase: null,
  },
  question: {
    pain: "Dúvida ao combinar o que já está no guarda-roupa",
    promise: "Receber um ajuste pessoal e aplicável",
    mechanism: "AI_PERSONALIZED_SAMPLE",
    offer: "Vega",
    primaryCta: "Começar meu ajuste gratuito",
    primaryVariable: "entrada do diagnóstico",
    primaryMetric: "compras líquidas por visitante humano",
  },
  health: {
    status: "READY",
    headline: "Experimento pronto para leitura comercial",
    description: "Sem bloqueios técnicos conhecidos.",
    blockers: [],
  },
  sampleDecision: {
    applicable: true,
    measurementAvailable: true,
    measurementSource: "PDE_ATTRIBUTED_HUMAN_COHORT",
    status: "INSUFFICIENT_DATA",
    headline: "Amostra comercial ainda insuficiente",
    explanation:
      "4 de 100 visitantes humanos distintos atribuídos; ainda faltam 96 para a primeira decisão.",
    humanVisitors: 4,
    initialTargetVisitors: 100,
    visitorsRemainingForInitialDecision: 96,
    targetPurchasesAtInitialDecision: 5,
    precisionTargetVisitors: 500,
    visitorsRemainingForPrecisionDecision: 496,
    purchases: 0,
    observedPurchaseRatePercent: 0,
    confidenceLower95Percent: 0,
    confidenceUpper95Percent: 60.24,
    zeroPurchaseUpper95Percent: 52.71,
    estimatedCostPerHumanVisitor: 1.4725,
    projectedSpendForInitialTarget: 147.25,
    projectedSpendForPrecisionTarget: 736.25,
    zeroPrimaryResultStopSpend: 25,
    mediaSpendLimit: 100,
    initialTargetFitsMediaSpendLimit: false,
    precisionTargetFitsMediaSpendLimit: false,
    projectionConfidence: "PRELIMINARY",
    financialGuardrail:
      "O teto atual protege o caixa, mas não financia a primeira amostra no custo observado; qualquer ampliação exige nova autorização.",
    recommendation:
      "Preservar versão, preço, público e oferta até a primeira decisão, respeitando as travas financeiras.",
  },
  funnel: [
    {
      stage: "VISUALIZACAO_FORM",
      label: "Entrada no PDE",
      order: 1,
      totalCount: 4,
      uniqueCount: 4,
      source: "PDE atribuído ao experimento",
    },
  ],
  bottleneck: {
    code: "AMOSTRA_COMERCIAL_INSUFICIENTE",
    title: "Amostra comercial ainda insuficiente",
    severity: "secondary",
    diagnosis:
      "4 de 100 visitantes humanos distintos atribuídos; ainda faltam 96 para a primeira decisão.",
    commercialImpact:
      "O volume atual ainda não separa acaso de desempenho comercial da versão.",
    recommendedFocus:
      "Preservar versão, preço, público e oferta até a primeira decisão.",
  },
  learnings: ["Ainda não há volume para concluir sobre a oferta."],
  nextActions: [
    {
      code: "PRESERVAR_TESTE",
      label: "Preservar a versão em teste",
      rationale:
        "O volume ainda não permite atribuir a ausência de compra à página, oferta ou público.",
      targetRoute: "/experiments/92",
    },
  ],
};

await writeFile(
  harnessHtml,
  '<meta name="viewport" content="width=device-width, initial-scale=1"><div id="root"></div><script type="module" src="/experiment-sample-decision-test.tsx"></script>',
);
await writeFile(
  harnessTsx,
  `import React from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import ExperimentCockpitPage from "./src/pages/experiment/ExperimentCockpitPage";

createRoot(document.getElementById("root")!).render(
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <MemoryRouter initialEntries={["/experiments/92/cockpit"]}>
      <main className="container-fluid py-3">
        <Routes>
          <Route path="/experiments/:id/cockpit" element={<ExperimentCockpitPage />} />
        </Routes>
      </main>
    </MemoryRouter>
  </QueryClientProvider>,
);`,
);

const server = spawn(
  process.execPath,
  ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "15392"],
  { cwd: frontendRoot, stdio: "inherit" },
);

let browser;
try {
  let ready = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    try {
      ready = (await fetch(`${baseUrl}/experiment-sample-decision-test.html`)).ok;
      if (ready) break;
    } catch {
      // O servidor local ainda está iniciando.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  assert.equal(ready, true, "Vite local não iniciou para o teste responsivo");

  await mkdir(evidenceDirectory, { recursive: true });
  browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });

  for (const [profileName, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone-15-pro", devices["iPhone 15 Pro"]],
    ["pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const pageErrors = [];
    const apiRequests = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") pageErrors.push(message.text());
    });
    await page.route(`${baseUrl}/api/**`, async (route) => {
      const url = new URL(route.request().url());
      apiRequests.push(`${route.request().method()} ${url.pathname}`);
      if (
        route.request().method() === "GET" &&
        url.pathname === "/api/experiments/92/cockpit"
      ) {
        await route.fulfill({ json: cockpit });
        return;
      }
      await route.fulfill({
        status: 404,
        json: { message: "Contrato fora da homologação local" },
      });
    });

    await page.goto(`${baseUrl}/experiment-sample-decision-test.html`);
    await page.waitForTimeout(500);
    if ((await page.getByText("Cockpit do Experimento").count()) === 0) {
      throw new Error(
        JSON.stringify({
          body: await page.locator("body").innerText(),
          apiRequests,
          pageErrors,
        }),
      );
    }
    await expect(
      page.getByRole("heading", { name: "Cockpit do Experimento" }),
    ).toBeVisible();
    const panel = page.getByRole("region", {
      name: "Estratégia progressiva de amostra",
    });
    await expect(panel).toBeVisible();
    await expect(panel.getByText("4 / 100")).toBeVisible();
    await expect(panel.getByText("0 / 5")).toBeVisible();
    await expect(panel.getByText("4 / 500")).toBeVisible();
    await expect(panel.getByText("Projeção preliminar")).toBeVisible();
    await expect(
      panel.getByText(/Parada automática sem resultado primário: R\$\s*25,00/),
    ).toBeVisible();
    await expect(panel.getByText(/Teto atual: R\$\s*100,00/)).toBeVisible();
    await expect(page.getByText("Preservar a versão em teste")).toBeVisible();
    assert.deepEqual(apiRequests, ["GET /api/experiments/92/cockpit"]);
    assert.deepEqual(pageErrors, []);
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
      true,
      `${profileName}: existe overflow horizontal`,
    );
    await page.screenshot({
      path: `${evidenceDirectory}/${profileName}.png`,
      fullPage: true,
    });
    await context.close();
    console.log(`PASS decisão de amostra responsiva: ${profileName}`);
  }
} finally {
  await browser?.close();
  server.kill();
  await rm(harnessHtml, { force: true });
  await rm(harnessTsx, { force: true });
}
