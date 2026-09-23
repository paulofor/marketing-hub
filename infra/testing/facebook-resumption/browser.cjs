// Homologa a tela real com dependências segregadas e sem acessar serviços produtivos.
const { chromium, devices, expect } = require("@playwright/test");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../../..");
const front = path.join(root, "frontend");
const html = path.join(front, "resumption-local.html");
const entry = path.join(front, "resumption-local.tsx");
const output = path.join(root, "artifacts/facebook-resumption");
fs.mkdirSync(output, { recursive: true });
fs.writeFileSync(
  html,
  '<html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/resumption-local.tsx"></script></html>',
);
fs.writeFileSync(
  entry,
  `import React from 'react';import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import 'bootstrap/dist/css/bootstrap.min.css';import Panel from './src/pages/experiment/ExperimentCampaignResumptionPanel';createRoot(document.getElementById('root')!).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><main className="container py-3"><Panel experimentId="910091"/></main></QueryClientProvider>);`,
);
const server = spawn(
  path.join(front, "node_modules/.bin/vite"),
  ["--host", "127.0.0.1", "--port", "15191", "--strictPort"],
  { cwd: front, stdio: "ignore" },
);
(async () => {
  let browser;
  try {
    for (let n = 0; n < 100; n++) {
      try {
        const r = await fetch("http://127.0.0.1:15191/resumption-local.html");
        if (r.ok) break;
      } catch {}
      await new Promise((r) => setTimeout(r, 100));
    }
    browser = await chromium.launch({
      executablePath: "/usr/bin/chromium",
      headless: true,
      args: ["--no-sandbox"],
    });
    const results = [];
    for (const [name, config] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(config);
      const page = await context.newPage();
      let state = {
        applicable: true,
        available: true,
        synchronizedSpend: 25.19,
        dailyBudget: 20,
        currentLimit: null,
        zeroResultStopSpend: 25,
        zeroPurchaseStopSpend: null,
        purchaseStopCount: null,
        latest: null,
      };
      let submitted;
      await page.route("**/api/**", async (route) => {
        if (
          !route
            .request()
            .url()
            .includes("/facebook-campaign-resumptions/experiments/910091")
        )
          throw new Error("Endpoint inesperado");
        if (route.request().method() === "POST") {
          submitted = route.request().postDataJSON();
          if (
            submitted.totalLimit !== 125 ||
            submitted.dailyBudget !== 20 ||
            submitted.startDate !== "2026-09-23" ||
            submitted.endDate !== "2026-09-29" ||
            submitted.zeroPurchaseSpendLimit !== 50 ||
            submitted.purchaseStopCount !== 5 ||
            !submitted.authorizeSpending
          )
            throw new Error("Autorização incorreta");
          state = {
            ...state,
            available: false,
            currentLimit: 125,
            zeroResultStopSpend: 50,
            zeroPurchaseStopSpend: 50,
            purchaseStopCount: 5,
            blocker:
              "Retomada aguardando a data inicial e a confirmação da Meta.",
            latest: {
              id: 1,
              status: "PENDING",
              totalLimit: 125,
              dailyBudget: 20,
              startDate: submitted.startDate,
              endDate: submitted.endDate,
              zeroResultSpendLimit: 50,
              zeroPurchaseSpendLimit: 50,
              purchaseStopCount: 5,
              reason: submitted.reason,
            },
          };
          await route.fulfill({ json: state.latest });
          setTimeout(() => {
            state.latest.status = "COMPLETED";
            state.blocker = "Experimento em execução.";
          }, 1000);
        } else await route.fulfill({ json: state });
      });
      await page.goto("http://127.0.0.1:15191/resumption-local.html");
      const submit = page.getByRole("button", { name: "Autorizar retomada" });
      await expect(submit).toBeDisabled();
      await page.getByLabel("Orçamento diário (R$) *").fill("20");
      await page.getByLabel("Teto acumulado de mídia (R$) *").fill("125");
      await page.getByLabel("Data inicial da retomada *").fill("2026-09-23");
      await page.getByLabel("Data final da retomada *").fill("2026-09-29");
      await page.getByLabel("Parar sem compra em (R$) *").fill("50");
      await page.getByLabel("Parar ao atingir compras").fill("5");
      await page
        .getByLabel("Motivo da retomada *")
        .fill(
          "Autorização comercial de Capella com parada em cinco compras ou falha operacional.",
        );
      await page.getByRole("checkbox").nth(0).check();
      await expect(submit).toBeDisabled();
      await page.getByRole("checkbox").nth(1).check();
      await expect(submit).toBeEnabled();
      await page.screenshot({
        path: path.join(output, `${name}-authorization.png`),
        fullPage: true,
      });
      await submit.click();
      await expect(page.getByText("PENDING", { exact: true })).toBeVisible();
      await expect(page.getByText("COMPLETED", { exact: true })).toBeVisible({
        timeout: 15000,
      });
      await expect(
        page.getByText(/Meta confirmou orçamento, prazo e campanha ativa/),
      ).toBeVisible();
      await page.screenshot({
        path: path.join(output, `${name}-completed.png`),
        fullPage: true,
      });
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth - window.innerWidth,
      );
      if (overflow > 2) throw new Error(`Overflow em ${name}: ${overflow}`);
      state = {
        ...state,
        available: true,
        latest: {
          ...state.latest,
          status: "FAILED",
          error: "Meta não confirmou orçamento; campanha permanece pausada.",
        },
      };
      await page.reload();
      await expect(page.getByText(/campanha permanece pausada/)).toBeVisible();
      await page.screenshot({
        path: path.join(output, `${name}-failure.png`),
        fullPage: true,
      });
      results.push({
        device: name,
        result: "PASS",
        dailyBudget: submitted.dailyBudget,
        totalLimit: submitted.totalLimit,
        zeroPurchaseSpendLimit: submitted.zeroPurchaseSpendLimit,
        purchaseStopCount: submitted.purchaseStopCount,
        preservesSpend: state.synchronizedSpend,
        overflow,
      });
      await context.close();
    }
    fs.writeFileSync(
      path.join(output, "browser-results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results));
  } finally {
    if (browser) await browser.close();
    server.kill("SIGTERM");
    fs.rmSync(html, { force: true });
    fs.rmSync(entry, { force: true });
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
