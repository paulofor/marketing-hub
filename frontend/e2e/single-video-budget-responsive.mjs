import assert from "node:assert/strict";
import { writeFile, rm, mkdir } from "node:fs/promises";
import { spawn } from "node:child_process";
import { chromium, devices, expect } from "@playwright/test";
const root = new URL("../", import.meta.url);
const html = new URL("single-video-budget-test.html", root);
const tsx = new URL("single-video-budget-test.tsx", root);
const output = new URL("../../artifacts/single-video-budget/", import.meta.url);
await writeFile(
  html,
  '<meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/single-video-budget-test.tsx"></script>',
);
await writeFile(
  tsx,
  `import React from 'react';import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import {MemoryRouter} from 'react-router-dom';import 'bootstrap/dist/css/bootstrap.min.css';import Panel from './src/pages/financial/SingleVideoBudgetPanel';
const id=Number(new URLSearchParams(location.search).get('id'));
createRoot(document.getElementById('root')).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><MemoryRouter><main style={{maxWidth:900,margin:'20px auto',padding:12}}><p>HOMOLOGAÇÃO LOCAL — dados sintéticos</p><Panel productId={id} cycleId={id+100} chainId={27}/></main></MemoryRouter></QueryClientProvider>);`,
);
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "--host",
    "127.0.0.1",
    "--port",
    "15274",
    "--strictPort",
  ],
  { cwd: root, stdio: "ignore" },
);
let browser;
try {
  for (let i = 0; i < 60; i++) {
    try {
      if (
        (await fetch("http://127.0.0.1:15274/single-video-budget-test.html")).ok
      )
        break;
    } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  await mkdir(output, { recursive: true });
  const results = [];
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 900 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    for (const id of [4, 7]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      const data = {
        productId: id,
        chainDefinitionId: 27,
        cycleId: id + 100,
        experimentId: id + 200,
        productVersion: "synthetic-v1",
        revision: 8,
        scope: "SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1",
        currency: "USD",
        canAuthorize: true,
        blocker: null,
        startsProduction: false,
        guidance:
          "O recibo não libera consumo. Preflight integral e revisões continuam necessários.",
        authorizations: [],
      };
      const sent = [];
      await page.route("**/api/**", async (route) => {
        const request = route.request();
        const path = new URL(request.url()).pathname;
        if (!path.startsWith("/api/")) return route.continue();
        if (path === "/api/sales-videos/projects")
          return route.fulfill({
            json: [
              {
                id: id + 300,
                productId: id,
                experimentId: id + 200,
                salesVideoProfileId: id + 400,
                campaignKey: "synthetic-v1",
                title: "Vídeo privado de teste",
              },
            ],
          });
        assert.equal(
          path,
          `/api/business-process-chains/learning-cycles/v1/products/${id}/${id + 100}/single-video-budget`,
        );
        if (request.method() === "POST") {
          const body = request.postDataJSON();
          sent.push(body);
          if (sent.length === 1)
            return route.fulfill({
              status: 503,
              json: {
                detail:
                  "Resposta temporariamente indisponível — simulação local.",
              },
            });
          assert.deepEqual(sent[0], body);
          assert.equal(body.totalLimitUsd, "15");
          assert.equal(body.confirmed, true);
          assert.equal(body.videoProjectId, id + 300);
          assert.equal(body.profileId, id + 400);
          data.revision = 9;
          data.authorizations = [
            {
              eventId: 55,
              reference: "internal://synthetic/55",
              videoProjectId: id + 300,
              profileId: id + 400,
              totalLimitUsd: 15,
              operatorName: body.operatorName,
              authorizationSource: body.authorizationSource,
              authorizedAt: "2026-11-05T14:00:00Z",
              current: true,
            },
          ];
        }
        return route.fulfill({ json: data });
      });
      await page.goto(
        `http://127.0.0.1:15274/single-video-budget-test.html?id=${id}`,
      );
      await page
        .getByLabel("Projeto existente *")
        .selectOption(String(id + 300));
      await page.getByLabel("Aprovado — teto total em US$ *").fill("15");
      await page
        .getByLabel("Operador que registra *")
        .fill("Operador sintético");
      await page
        .getByLabel("Origem da autorização do cliente *")
        .fill("Autorização sintética única de USD 15, sem gastos históricos.");
      await expect(
        page.getByRole("button", { name: "Registrar autorização" }),
      ).toBeDisabled();
      await page.getByRole("checkbox").check();
      await page.getByRole("button", { name: "Registrar autorização" }).click();
      await expect(page.getByRole("alert")).toContainText("simulação local");
      await page.getByRole("button", { name: "Registrar autorização" }).click();
      await expect(page.getByText(/Recibo #55/)).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Registrar autorização" }),
      ).toBeDisabled();
      assert.equal(sent.length, 2);
      assert.deepEqual(errors, []);
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        true,
      );
      await page.screenshot({
        path: new URL(`${name}-${id}.png`, output).pathname,
        fullPage: true,
      });
      results.push({
        device: name,
        productId: id,
        success: true,
        paidCalls: 0,
      });
      await context.close();
    }
  }
  await writeFile(
    new URL("results.json", output),
    JSON.stringify(results, null, 2),
  );
  console.log(JSON.stringify(results));
} finally {
  await browser?.close();
  server.kill("SIGTERM");
  await rm(html, { force: true });
  await rm(tsx, { force: true });
}
