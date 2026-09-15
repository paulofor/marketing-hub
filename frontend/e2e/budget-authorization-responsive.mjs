import assert from "node:assert/strict";
import { writeFile, rm, mkdir } from "node:fs/promises";
import { spawn } from "node:child_process";
import { chromium, devices, expect } from "@playwright/test";
const root = new URL("../", import.meta.url);
const html = new URL("budget-test.html", root);
const tsx = new URL("budget-test.tsx", root);
await writeFile(
  html,
  '<meta name="viewport" content="width=device-width, initial-scale=1"><div id="root"></div><script type="module" src="/budget-test.tsx"></script>',
);
await writeFile(
  tsx,
  `import React from 'react'; import {createRoot} from 'react-dom/client';
import {QueryClient,QueryClientProvider} from '@tanstack/react-query'; import {MemoryRouter} from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css'; import './src/pages/learningCycle/LearningCyclesPage.css';
import Form from './src/pages/learningCycle/LearningCycleCommandForm';
const blocked = new URLSearchParams(location.search).has('blocked');
const cycle = {id:200,productId:400,experimentId:920,revision:3,stage:'AUTHORIZATION',productVersion:'synthetic-v12',budgetLimitBrl:100,
 authorizationReview:{dailyBudgetBrl:50}, commands:[{action:'COMPLETE',available:!blocked,reason:'Janela encerrada'}]};
createRoot(document.getElementById('root')).render(<QueryClientProvider client={new QueryClient()}><MemoryRouter><main style={{maxWidth:900,margin:'20px auto',padding:16}}><Form cycle={cycle as any} catalog={{returnTargets:[]} as any} onUpdated={()=>document.body.dataset.approved='true'}/></main></MemoryRouter></QueryClientProvider>);`,
);
const server = spawn(
  process.execPath,
  ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "15273"],
  { cwd: root, stdio: "ignore" },
);
let browser;
try {
  for (let i = 0; i < 60; i++) {
    try {
      if ((await fetch("http://127.0.0.1:15273/budget-test.html")).ok) break;
    } catch {}
    await new Promise((r) => setTimeout(r, 250));
  }
  browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  await mkdir(
    new URL("../../artifacts/budget-authorization/", import.meta.url),
    { recursive: true },
  );
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 900 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const sent = [];
    let fail = true;
    await page.route("**/api/**", async (route) => {
      if (!new URL(route.request().url()).pathname.startsWith("/api/"))
        return route.continue();
      assert(
        route
          .request()
          .url()
          .endsWith("/products/400/200/budget-authorization"),
      );
      sent.push(route.request().postDataJSON());
      if (fail) {
        fail = false;
        return route.fulfill({
          status: 409,
          json: { detail: "Falha temporária simulada" },
        });
      }
      return route.fulfill({ json: { stage: "PUBLICATION" } });
    });
    await page.goto("http://127.0.0.1:15273/budget-test.html");
    await expect(page.locator("input")).toHaveCount(2);
    const daily = page.getByLabel("Orçamento diário (R$)"),
      total = page.getByLabel("Orçamento total (R$)");
    await expect(daily).toHaveValue("50");
    await expect(total).toHaveValue("100");
    assert.equal(sent.length, 0);
    await daily.fill("101");
    await page.getByRole("button", { name: "Aprovar orçamento" }).click();
    await expect(page.getByRole("alert")).toContainText("diário");
    assert.equal(sent.length, 0);
    await daily.fill("30.25");
    await total.fill("120");
    await page.getByRole("button", { name: "Aprovar orçamento" }).click();
    await expect(page.getByRole("alert")).toContainText("Falha temporária");
    await page.getByRole("button", { name: "Aprovar orçamento" }).click();
    await expect(page.locator("body")).toHaveAttribute("data-approved", "true");
    assert.deepEqual(sent[0], sent[1]);
    assert.equal(sent[1].dailyBudgetBrl, 30.25);
    assert.equal(sent[1].budgetLimitBrl, 120);
    assert.deepEqual(Object.keys(sent[1]).sort(), [
      "budgetLimitBrl",
      "dailyBudgetBrl",
      "expectedRevision",
      "requestKey",
    ]);
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    );
    await page.screenshot({
      path: new URL(
        "../../artifacts/budget-authorization/" + name + ".png",
        import.meta.url,
      ).pathname,
      fullPage: true,
    });
    await page.goto("http://127.0.0.1:15273/budget-test.html?blocked");
    await expect(
      page.getByRole("button", { name: "Aprovar orçamento" }),
    ).toBeDisabled();
    await expect(page.getByRole("alert")).toContainText("Janela encerrada");
    await context.close();
    console.log(
      name + ": sugestões, edição, limites, falha, replay e bloqueio aprovados",
    );
  }
} finally {
  await browser?.close();
  server.kill();
  await rm(html, { force: true });
  await rm(tsx, { force: true });
}
