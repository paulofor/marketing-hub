import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { createServer } from "../frontend/node_modules/vite/dist/node/index.js";
const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const evidenceDir = process.env.HERMES_TEST_EVIDENCE_DIR;
assert(
  evidenceDir,
  "Informe HERMES_TEST_EVIDENCE_DIR com a resposta gerada pelo teste SQL/API.",
);
const monitor = JSON.parse(
  await fs.readFile(path.join(evidenceDir, "monitor.json"), "utf8"),
);
assert.equal(monitor.pde.totalEvents, 86);
const virtualPath = "/hermes-metrics-entry.jsx";
const vite = await createServer({
  root: path.join(root, "frontend"),
  configFile: path.join(root, "frontend/vite.config.ts"),
  server: { host: "127.0.0.1", port: 4179, strictPort: true },
  plugins: [
    {
      name: "hermes-local-validation",
      configureServer(server) {
        server.middlewares.use(
          "/hermes-metrics-validation",
          async (req, res) => {
            res.setHeader("Content-Type", "text/html");
            res.end(
              await server.transformIndexHtml(
                req.url,
                `<html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="${virtualPath}"></script></html>`,
              ),
            );
          },
        );
      },
      resolveId(id) {
        return id === virtualPath ? id : null;
      },
      load(id) {
        if (id !== virtualPath) return null;
        return `import React from 'react';
        import {createRoot} from 'react-dom/client';
        import {QueryClient,QueryClientProvider} from '@tanstack/react-query';
        import 'bootstrap/dist/css/bootstrap.min.css';
        import Panel from '/src/pages/experiment/ExperimentLandingAnalyticsTab.tsx';
        createRoot(document.getElementById('root')).render(React.createElement(QueryClientProvider,{client:new QueryClient({defaultOptions:{queries:{retry:false}}})},React.createElement('main',{className:'container-fluid p-3'},React.createElement(Panel,{experimentId:'91',experimentType:'PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL'}))));`;
      },
    },
  ],
});
await vite.listen();
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const errors = [];
    const calls = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route(
      (url) => url.pathname.startsWith("/api/"),
      async (route) => {
        assert.equal(
          route.request().method(),
          "GET",
          "Homologação não pode gerar eventos ou mutações.",
        );
        const url = new URL(route.request().url());
        calls.push(url.pathname);
        let data;
        if (url.pathname.endsWith("/post-deploy-monitor")) data = monitor;
        else if (url.pathname.endsWith("/pde-persuasive-journey"))
          data = { version: "test", framework: "PDE", steps: [] };
        else {
          errors.push(`Rota inesperada: ${url.pathname}`);
          return route.fulfill({
            status: 500,
            body: "Rota inesperada na homologação",
          });
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(data),
        });
      },
    );
    await page.goto("http://127.0.0.1:4179/hermes-metrics-validation", {
      waitUntil: "networkidle",
    });
    await page.getByText("Eventos PDE", { exact: true }).waitFor();
    const text = await page.locator("body").innerText();
    assert(text.includes("86"), `${name}: total de eventos ausente`);
    assert(
      text.includes("Sem conclusão sobre abandono"),
      `${name}: conclusão de abandono indevida`,
    );
    assert(
      text.includes("FIELD_FILLED") ||
        text.toLowerCase().includes("field_filled"),
      `${name}: preenchimento omitido`,
    );
    assert(
      !calls.some((url) => url.includes("funnel/analytics")),
      `${name}: consulta legada indevida`,
    );
    assert.deepEqual(errors, [], `${name}: erros de execução`);
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
      `${name}: overflow horizontal`,
    );
    await page.screenshot({
      path: path.join(evidenceDir, `panel-${name}.png`),
      fullPage: true,
    });
    console.log(
      `${name}: PASS — componente real, 86 eventos, fonte PDE, somente GET`,
    );
    await context.close();
  }
} finally {
  await browser.close();
  await vite.close();
}
