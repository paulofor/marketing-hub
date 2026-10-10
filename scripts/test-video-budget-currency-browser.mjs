import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { createServer } from "../frontend/node_modules/vite/dist/node/index.js";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(path.join(root, "frontend"));
const evidence = path.join(
  root,
  "artifacts/mira-cycle9-authorization-3332/currency-browser",
);
await fs.mkdir(evidence, { recursive: true });
const entry = "/video-budget-currency-entry.jsx";
const vite = await createServer({
  root: path.join(root, "frontend"),
  configFile: path.join(root, "frontend/vite.config.ts"),
  server: { host: "127.0.0.1", port: 4186, strictPort: true },
  plugins: [
    {
      name: "video-budget-currency-local-validation",
      configureServer(server) {
        server.middlewares.use(
          "/video-budget-currency-validation",
          async (req, res) => {
            res.setHeader("Content-Type", "text/html");
            res.end(
              await server.transformIndexHtml(
                req.url,
                `<html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="${entry}"></script></html>`,
              ),
            );
          },
        );
      },
      resolveId(id) {
        return id === entry ? path.join(root, "frontend", entry) : null;
      },
      load(id) {
        if (id !== path.join(root, "frontend", entry)) return null;
        return `import React from 'react'; import {createRoot} from 'react-dom/client';
        import {QueryClient,QueryClientProvider} from '@tanstack/react-query';
        import {MemoryRouter,Route,Routes} from 'react-router-dom';
        import AudioVideoStudioPage from './src/pages/audioVideoStudio/AudioVideoStudioPage.tsx';
        import ProductSalesVideoPage from './src/pages/salesVideo/ProductSalesVideoPage.tsx';
        import 'bootstrap/dist/css/bootstrap.min.css';
        const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});
        const params=new URLSearchParams(location.search);
        const id=params.get('project')||'10';
        const route=params.has('recovery')?'/products/'+id+'/sales-videos':'/audio-video-studio/projects/'+id;
        createRoot(document.getElementById('root')).render(<QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[route]}><Routes>
          <Route path='/products/:productId/sales-videos' element={<ProductSalesVideoPage/>}/>
          <Route path='/audio-video-studio/projects/:projectId' element={<AudioVideoStudioPage/>}/>
          </Routes></MemoryRouter></QueryClientProvider>);`;
      },
    },
  ],
});
let browser;
const results = [];
try {
  await vite.listen();
  browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox", "--disable-dev-shm-usage"],
  });
  for (const [name, options, projectId, productId] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }, 10, 10],
    ["iphone", devices["iPhone 15 Pro"], 91001, 91011],
    ["pixel", devices["Pixel 7"], 91002, 91012],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const posts = [],
      errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith("/api/")) {
        if (url.hostname !== "127.0.0.1") return route.abort();
        return route.continue();
      }
      let body = [];
      if (url.pathname === `/api/sales-videos/projects/${projectId}`)
        body = {
          id: projectId,
          productId,
          experimentId: productId + 100,
          salesVideoProfileId: productId + 200,
          title: "Demonstração real de teste",
          objective: "Conferir valor antes do compromisso",
          targetDurationSeconds: 15,
          videoCategory: "COMMERCIAL_SHORT",
          contextType: "PDE",
          format: "VERTICAL_9_16",
          productionMode: "STORY_FIRST_AUDIO_VIDEO",
          targetChannel: "PDE_AND_SOCIAL",
          status: "DRAFT",
        };
      if (url.pathname === "/api/sales-videos/studio/catalog")
        body = { characters: [], captionPresets: [] };
      if (url.pathname === `/api/products/${productId}`)
        body = { id: productId, name: "Produto de fixture", price: 49 };
      if (url.pathname === `/api/products/${productId}/sales-videos/profiles`)
        body = [
          {
            id: projectId + 200,
            productId,
            title: "Vídeo de fixture",
            videoKind: "HERO",
            targetDurationSeconds: 15,
            status: "SCRIPT_READY",
          },
        ];
      if (url.pathname === `/api/products/${productId}/sales-videos/jobs`)
        body = [
          {
            id: projectId + 800,
            profileId: projectId + 200,
            assetId: projectId + 900,
            status: "VIDEO_READY",
            providerName: "EDITORIAL_MOTION",
            jobType: "RENDER",
            requestedAt: "2026-10-10T00:00:00Z",
          },
          {
            id: projectId + 801,
            profileId: projectId + 200,
            retryOfJobId: projectId + 800,
            status: "VIDEO_FAILED",
            providerName: "MUSA_POST_PRODUCTION",
            jobType: "POST_PRODUCTION",
            failureCode: "APOLLO_NARRATION_DURATION_EXCEEDED",
            requestedAt: "2026-10-10T00:00:01Z",
          },
        ];
      if (url.pathname === `/api/media/${projectId + 900}`)
        body = {
          id: projectId + 900,
          type: "VIDEO",
          status: "READY",
          url: "http://127.0.0.1:4186/fixture.mp4",
        };
      if (route.request().method() === "POST") {
        posts.push({
          path: url.pathname,
          body: route.request().postDataJSON(),
        });
        body = {
          id: projectId + 300,
          videoProjectId: projectId,
          status: "PENDING_FINANCIAL_REVIEW",
        };
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(body),
      });
    });
    await page.goto(
      `http://127.0.0.1:4186/video-budget-currency-validation?project=${projectId}`,
    );
    await page.getByLabel("Moeda da autorização").selectOption("USD");
    await page.getByLabel("Teto autorizado em USD").fill("4.00");
    assert.equal(await page.getByLabel("Cotação BRL por USD").count(), 0);
    await page
      .getByLabel("Objetivo de aprendizado")
      .fill("Demonstrar a entrega real");
    await page
      .getByLabel("Critério de sucesso")
      .fill("QA independente e teto conjunto preservado");
    const preflight = page.getByRole("button", {
      name: "Executar somente preflight sem gerar vídeo",
    });
    const production = page.getByRole("button", {
      name: "Solicitar produção a Apolo sob controle de Plutus",
    });
    await page.getByLabel("Teto autorizado em USD").fill("0");
    assert(await preflight.isDisabled());
    assert(await production.isDisabled());
    await page.getByLabel("Teto autorizado em USD").fill("4.00");
    await preflight.click();
    await production.click();
    assert.equal(posts.length, 2);
    for (const post of posts) {
      assert.equal(post.body.videoProjectId, projectId);
      assert.equal(post.body.authorizedBudgetCurrency, "USD");
      assert.equal(post.body.budgetLimitUsd, 4);
      assert.equal(post.body.authorizedBudgetAmount, 4);
      assert(!("usdBrlExchangeRate" in post.body));
      assert(!("exchangeRateSource" in post.body));
      assert(!("exchangeRateDate" in post.body));
    }
    await page.getByLabel("Moeda da autorização").scrollIntoViewIfNeeded();
    await page.screenshot({ path: path.join(evidence, `${name}.png`) });
    await page.getByLabel("Moeda da autorização").selectOption("BRL");
    assert.equal(
      await page.getByLabel("Teto autorizado em BRL").inputValue(),
      "",
    );
    assert(await production.isDisabled());
    assert.equal(errors.length, 0, errors.join("\n"));
    results.push({
      name,
      projectId,
      productId,
      posts,
      status: "PASS",
      externalCalls: 0,
    });
    const beforeRecovery = posts.length;
    await page.goto(
      `http://127.0.0.1:4186/video-budget-currency-validation?project=${productId}&recovery=1`,
    );
    // Os IDs do produto e do projeto são distintos em mobile; o roteamento continua pelo produto.
    await page.getByLabel("Voz off", { exact: true }).fill("Texto recebido");
    await page
      .getByLabel("Legenda principal", { exact: true })
      .fill("Texto recebido");
    const duration = page.getByLabel("Duração final (segundos)", {
      exact: true,
    });
    const finalize = page.getByRole("button", {
      name: "Gerar pós-produção + HLS",
      exact: true,
    });
    for (const invalid of ["5", "61", "15.7"]) {
      await duration.fill(invalid);
      assert(await finalize.isDisabled());
    }
    await duration.fill("20");
    assert(await finalize.isEnabled());
    await finalize.click();
    await page.waitForTimeout(100);
    assert.equal(posts.length, beforeRecovery + 1);
    const recovery = posts.at(-1);
    assert.equal(
      recovery.path,
      `/api/sales-videos/jobs/${projectId + 800}/request-post-production`,
    );
    assert.equal(recovery.body.targetDurationSeconds, 20);
    assert.equal(recovery.body.captionText, "Texto recebido");
    assert.equal(recovery.body.voiceOverScript, "Texto recebido");
    await duration.scrollIntoViewIfNeeded();
    await page.screenshot({
      path: path.join(evidence, `${name}-narration-recovery.png`),
    });
    assert.equal(errors.length, 0, errors.join("\n"));
    await context.close();
  }
  await fs.writeFile(
    path.join(evidence, "results.json"),
    JSON.stringify(results, null, 2),
  );
  console.log(
    JSON.stringify({
      scenarios: results.length,
      status: "PASS",
      evidence,
      externalCalls: 0,
    }),
  );
} finally {
  await browser?.close();
  await vite.close();
}
