const { chromium, devices, expect } = require("@playwright/test");
const fs = require("node:fs");
const path = require("node:path");
const { spawn } = require("node:child_process");
const root = path.resolve(__dirname, "..");
const html = path.join(root, "continuity-qa.html");
const tsx = path.join(root, "continuity-qa.tsx");
const output = path.resolve(root, "../artifacts/process-version-continuity");
fs.mkdirSync(output, { recursive: true });
fs.writeFileSync(
  html,
  '<meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/continuity-qa.tsx"></script>',
);
fs.writeFileSync(
  tsx,
  `import React from 'react';import {createRoot} from 'react-dom/client';import {MemoryRouter} from 'react-router-dom';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import 'bootstrap/dist/css/bootstrap.min.css';import Panel from './src/pages/product/ProductProcessVersionContinuity';createRoot(document.getElementById('root')!).render(<MemoryRouter><QueryClientProvider client={new QueryClient()}><Panel productId={71} cycleId={99} processId={171}/></QueryClientProvider></MemoryRouter>);`,
);
const server = spawn(
  process.execPath,
  [
    path.join(root, "node_modules/vite/bin/vite.js"),
    "--host",
    "127.0.0.1",
    "--port",
    "15189",
  ],
  { cwd: root, stdio: "ignore", detached: true },
);
let browser;
(async () => {
  try {
    for (let i = 0; i < 80; i++) {
      try {
        if ((await fetch("http://127.0.0.1:15189/continuity-qa.html")).ok)
          break;
      } catch {}
      await new Promise((r) => setTimeout(r, 250));
    }
    browser = await chromium.launch({
      executablePath: "/usr/bin/chromium",
      args: ["--no-sandbox"],
    });
    const results = [];
    for (const [name, device] of [
      ["desktop", { viewport: { width: 1366, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      let saved = false,
        posts = 0,
        fail = true;
      await page.route("**/api/**", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (
          !url.pathname.endsWith("/171/products/71/automation/v1/continuity") ||
          url.searchParams.get("learningCycleId") !== "99"
        )
          throw Error("Contexto incorreto");
        if (request.method() === "POST") {
          const body = request.postDataJSON();
          if (
            body.fingerprint !== "synthetic-v1" ||
            Object.keys(body.compatibilityEvidence).length !== 0
          )
            throw Error("Conferência ausente");
          saved = true;
          posts++;
        } else if (fail) {
          fail = false;
          return route.fulfill({
            status: 409,
            json: { detail: "Falha sintética recuperável" },
          });
        }
        return route.fulfill({
          json: {
            fingerprint: "synthetic-v1",
            sourceProcessId: 171,
            targetProcessId: 181,
            successorRunId: saved ? 901 : null,
            canPrepare: !saved,
            reason: saved
              ? "Continuidade preparada e pausada"
              : "Confira as provas",
            url: "/products/71/value-chain-history/processes/181/activities?chainId=31&learningCycleId=99&sourceReference=experiment%3A201",
            outputs: Array.from({ length: 8 }, (_, n) => ({
              activityId: `step-${n}`,
              name: `Entrega ${n + 1}`,
              accepted: true,
              instanceId: 300 + n,
              changedFields: n === 0 ? ["/description"] : [],
              sourceContract: "Contrato anterior",
              targetContract: "Contrato publicado",
              evidence: "Prova sintética " + n,
              owner: "Dédalo",
              taskIds: [800 + n],
              destinationStatus: n === 0 ? "REVALIDATION_PENDING" : "ACCEPTED",
              criteria: [
                {
                  description: "Critério novo exige prova executada",
                  status: "REVALIDATION_PENDING",
                },
              ],
            })),
          },
        });
      });
      await page.goto("http://127.0.0.1:15189/continuity-qa.html");
      await expect(page.getByRole("alert")).toBeVisible();
      await page.getByRole("button", { name: "Consultar novamente" }).click();
      await expect(
        page.getByText("Entrega 8 · entrega original #307"),
      ).toBeVisible();
      await expect(
        page.getByRole("button", {
          name: "Preparar continuidade sem executar",
        }),
      ).toBeDisabled();
      await page
        .getByLabel("Responsável pela conferência")
        .fill("Operador sintético");
      await page
        .getByLabel("Motivo da continuidade")
        .fill("Homologação isolada");
      await page.screenshot({
        path: path.join(output, name + "-before.png"),
        fullPage: true,
      });
      await page
        .getByRole("button", { name: "Preparar continuidade sem executar" })
        .click();
      await expect(
        page.getByRole("link", { name: "Abrir continuidade #901" }),
      ).toBeVisible();
      await page.reload();
      await expect(
        page.getByRole("link", { name: "Abrir continuidade #901" }),
      ).toBeVisible();
      await expect(
        page.getByText(/Revalidação pendente — responsável: Dédalo/),
      ).toBeVisible();
      if (posts !== 1) throw Error("Comando duplicado");
      if (
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth + 1,
        )
      )
        throw Error("Overflow horizontal");
      await page.screenshot({
        path: path.join(output, name + "-after.png"),
        fullPage: true,
      });
      results.push({ device: name, posts, reload: true, isolated: true });
      await context.close();
    }
    fs.writeFileSync(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results));
  } finally {
    if (browser) await browser.close();
    process.kill(-server.pid, "SIGTERM");
    fs.unlinkSync(html);
    fs.unlinkSync(tsx);
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
