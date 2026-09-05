// Homologa somente a criação local; nenhum request pode alcançar produção ou a Meta.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");
const { chromium, devices } = require("playwright");

const fixture = JSON.parse(process.argv[2]);
const backend = new URL(fixture.backend);
assert.equal(backend.hostname, "127.0.0.1");
const dist = path.resolve("frontend/dist");
const evidence = path.resolve("artifacts/vega91-video-to-meta");
fs.mkdirSync(evidence, { recursive: true });
fs.rmSync(path.join(evidence, "browser-results.json"), { force: true });
const contentTypes = {
  ".js": "application/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".svg": "image/svg+xml",
};
const ownExperiment = `/api/experiments/${fixture.experimentId}`;
const realBackendPaths = new Set([
  ownExperiment,
  `${ownExperiment}/video-assets`,
  `${ownExperiment}/video-assets/${fixture.videoId}/creative`,
]);
const server = http.createServer((req, res) => {
  const relative = new URL(req.url, "http://localhost").pathname;
  if (realBackendPaths.has(relative)) {
    const upstream = http.request(
      new URL(req.url, backend),
      {
        method: req.method,
        headers: {
          ...req.headers,
          host: backend.host,
          "X-Tenant-ID": "default",
        },
      },
      (response) => {
        res.writeHead(response.statusCode, response.headers);
        response.pipe(res);
      },
    );
    upstream.on("error", (error) => {
      console.error("LOCAL_BACKEND_PROXY_ERROR", error.message);
      if (!res.headersSent) res.writeHead(502);
      res.end();
    });
    req.pipe(upstream);
    return;
  }
  let file = path.join(dist, relative);
  if (
    !file.startsWith(dist) ||
    !fs.existsSync(file) ||
    fs.statSync(file).isDirectory()
  )
    file = path.join(dist, "index.html");
  res.setHeader(
    "Content-Type",
    contentTypes[path.extname(file)] || "application/octet-stream",
  );
  fs.createReadStream(file).pipe(res);
});

(async () => {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [name, settings] of [
      ["desktop", { viewport: { width: 1440, height: 1050 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      console.log("VIDEO_CREATIVE_DEVICE", name);
      const { defaultBrowserType, ...contextOptions } = settings;
      const context = await browser.newContext(contextOptions);
      let realWrites = 0;
      await context.route("**/*", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (!url.pathname.startsWith("/api/")) {
          if (url.origin === origin) return route.continue();
          return route.fulfill({ status: 204, body: "" });
        }
        if (realBackendPaths.has(url.pathname)) {
          if (request.method() !== "GET") realWrites++;
          return route.continue({ url: origin + url.pathname });
        }
        assert.equal(
          request.method(),
          "GET",
          `Mutação fora do escopo: ${url.pathname}`,
        );
        let data = [];
        if (url.pathname.endsWith("/readiness"))
          data = {
            issues: [],
            runningGateRequirements: [],
            eligibleForRunning: false,
          };
        if (url.pathname.endsWith("/performance-dashboard"))
          data = { assets: [], campaigns: [], summary: {} };
        if (url.pathname.endsWith("/ads-in-use"))
          data = { productId: 4, ads: [] };
        if (url.pathname === "/api/products/4/ads")
          data = { productId: 4, ads: [] };
        if (url.pathname.endsWith("/funnel")) data = [];
        if (url.pathname.endsWith("/diagnostics")) data = { diagnostics: [] };
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(data),
        });
      });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => {
        errors.push(error.message);
        console.error("BROWSER_PAGE_ERROR", error.message);
      });
      await page.goto(
        `${origin}/experiments/${fixture.experimentId}?tab=video`,
      );
      await page.getByRole("tab", { name: "Vídeo", exact: true }).click();
      await page
        .getByRole("button", { name: "Usar vídeo em anúncio", exact: true })
        .click();
      await page.getByLabel("Título do anúncio").fill("Primeiro ajuste QA");
      await page
        .getByLabel("Texto principal", { exact: true })
        .fill("QA_INTERNAL: teste local de mídia aprovada.");
      await page
        .getByLabel("Descrição curta (opcional)")
        .fill("Sem tráfego ou cobrança");
      if (name === "desktop")
        await page
          .getByRole("combobox")
          .selectOption(String(fixture.rejectedId));
      await page.screenshot({
        path: path.join(evidence, `${name}-form.png`),
        fullPage: false,
      });
      const submit = page.getByRole("button", {
        name: "Cadastrar e enviar para revisão",
      });
      console.log(
        "VIDEO_CREATIVE_HIT_TEST",
        name,
        await submit.evaluate((button) => {
          const rect = button.getBoundingClientRect();
          const hit = document.elementFromPoint(
            rect.x + rect.width / 2,
            rect.y + rect.height / 2,
          );
          return {
            rect: rect.toJSON(),
            hit: hit?.outerHTML.slice(0, 350),
            viewport: [innerWidth, innerHeight],
          };
        }),
      );
      await submit.click();
      await page
        .getByRole("status")
        .filter({ hasText: /Anúncio #\d+ cadastrado/ })
        .waitFor();
      assert.equal(realWrites, 1);
      assert.deepEqual(errors, []);
      await page.screenshot({
        path: path.join(evidence, `${name}-result.png`),
        fullPage: false,
      });
      await page
        .getByRole("status")
        .filter({ hasText: /Anúncio #\d+ cadastrado/ })
        .getByRole("link", { name: "Criativos" })
        .click();
      await page.waitForFunction(
        () =>
          document.querySelector('[role="tab"][aria-selected="true"]')
            ?.textContent === "Criativos",
      );
      await context.unrouteAll({ behavior: "wait" });
      assert.deepEqual(errors, []);
      results.push({
        device: name,
        realBackendWrites: realWrites,
        outcome: "PASS",
        scope: "QA_INTERNAL",
      });
      await context.close();
    }
    fs.writeFileSync(
      path.join(evidence, "browser-results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log("VIDEO_CREATIVE_BROWSER", JSON.stringify(results));
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  server.close();
  process.exitCode = 1;
});
