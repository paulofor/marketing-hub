const { chromium, devices } = require("@playwright/test");
const fs = require("node:fs");
const path = require("node:path");
const { spawn } = require("node:child_process");
const root = path.resolve(__dirname, "..");
const html = path.join(root, "waiver-test.html"),
  tsx = path.join(root, "waiver-test.tsx");
fs.writeFileSync(
  html,
  '<meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/waiver-test.tsx"></script>',
);
fs.writeFileSync(
  tsx,
  "import React from 'react';import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import 'bootstrap/dist/css/bootstrap.min.css';import Panel from './src/pages/financial/LegacyCostWaivers';import fixture from './e2e/fixtures/execution-finance.json';createRoot(document.getElementById('root')!).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><Panel data={fixture as any}/></QueryClientProvider>);",
);
const server = spawn(
  process.execPath,
  [
    path.join(root, "node_modules/vite/bin/vite.js"),
    "--host",
    "127.0.0.1",
    "--port",
    "15179",
  ],
  { cwd: root, stdio: "ignore", detached: true },
);
const output = path.resolve(root, "../artifacts/legacy-cost-waivers");
fs.mkdirSync(output, { recursive: true });
let browser;
(async () => {
  for (let i = 0; i < 80; i++) {
    try {
      if ((await fetch("http://127.0.0.1:15179/waiver-test.html")).ok) break;
    } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  try {
    browser = await chromium.launch({
      executablePath: "/usr/bin/chromium",
      headless: true,
      args: ["--no-sandbox"],
    });
    const fixture = require("./fixtures/execution-finance.json");
    for (const mobile of [false, true]) {
      const context = await browser.newContext(
        mobile
          ? devices["Pixel 7"]
          : { viewport: { width: 1366, height: 900 } },
      );
      await context.addInitScript(() =>
        Object.defineProperty(crypto, "randomUUID", { value: undefined }),
      );
      const p = await context.newPage();
      let saved = false,
        posts = 0;
      await p.route("**/api/**", async (route) => {
        const url = route.request().url();
        if (!new URL(url).pathname.startsWith("/api/")) return route.continue();
        if (url.includes("cost-waivers")) {
          if (route.request().method() === "POST") {
            const b = route.request().postDataJSON();
            if (JSON.stringify(b.taskIds) !== "[246]" || !b.confirmed)
              throw Error("Seleção incorreta");
            saved = true;
            posts++;
          }
          return route.fulfill({
            json: {
              tasks: [
                {
                  taskId: 246,
                  experimentId: 90,
                  status: "CANCELLED",
                  costUsd: null,
                  waived: saved,
                  selectable: !saved,
                },
                {
                  taskId: 247,
                  experimentId: 90,
                  status: "CANCELLED",
                  costUsd: null,
                  waived: false,
                  selectable: true,
                },
              ],
              history: saved
                ? [
                    {
                      eventId: 1,
                      cycleId: 1900,
                      operatorName: "Homologação",
                      decidedAt: "2026-11-22T12:00:00Z",
                      reason: "Teste isolado",
                      taskIds: [246],
                    },
                  ]
                : [],
              blockingTasks: saved ? 1 : 2,
            },
          });
        }
        if (route.request().method() !== "GET")
          throw Error("Comando indevido " + url);
        if (url.includes("financial-review"))
          return route.fulfill({ json: fixture });
        return route.fulfill({
          status: 404,
          json: { message: "Fixture fora deste escopo" },
        });
      });
      await p.goto("http://127.0.0.1:15179/waiver-test.html");
      await p.getByLabel(/Tarefa #246/).check();
      await p.getByLabel("Responsável *", { exact: true }).fill("Homologação");
      await p
        .getByLabel("Motivo da decisão *", { exact: true })
        .fill("Teste isolado");
      await p.getByLabel(/Confirmo a dispensa/).check();
      await p.getByRole("button", { name: "Registrar dispensa" }).click();
      await p
        .getByText("Dispensa registrada. Nenhum processo foi retomado.")
        .waitFor();
      await p.reload();
      await p.getByText("Pendências de conciliação sem dispensa: 1.").waitFor();
      if (posts !== 1) throw Error("Duplicação");
      await p.getByText("Histórico de dispensas (1)").click();
      await p.screenshot({
        path: path.join(output, (mobile ? "mobile" : "desktop") + ".png"),
        fullPage: true,
      });
      console.log(
        (mobile ? "mobile" : "desktop") +
          ": seleção, persistência após recarga e nenhuma retomada conferidos",
      );
      await context.close();
    }
  } finally {
    await browser?.close();
    // Encerra também o verificador TypeScript filho do Vite, sem deixar consumo órfão.
    try {
      process.kill(-server.pid, "SIGTERM");
    } catch (error) {
      if (error.code !== "ESRCH") throw error;
    }
    fs.rmSync(html, { force: true });
    fs.rmSync(tsx, { force: true });
  }
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
