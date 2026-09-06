// Homologa recuperação na UI com backend real local e parecer explicitamente simulado.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");
const { chromium, devices, expect } = require("@playwright/test");
const fixture = JSON.parse(process.argv[2]);
const backend = new URL(fixture.backend);
assert.equal(backend.hostname, "127.0.0.1");
const dist = path.resolve("frontend/dist");
assert(
  fs.existsSync(path.join(dist, "index.html")),
  "Compile o frontend antes de iniciar a homologação de navegador.",
);
const evidence = path.resolve("artifacts/vega91-publication-recovery");
fs.mkdirSync(evidence, { recursive: true });
const server = http.createServer((req, res) => {
  if (req.url.startsWith("/api/")) {
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
      console.error(error.message);
      res.writeHead(502);
      res.end();
    });
    req.pipe(upstream);
    return;
  }
  let file = path.join(dist, new URL(req.url, "http://localhost").pathname);
  if (
    !file.startsWith(dist) ||
    !fs.existsSync(file) ||
    fs.statSync(file).isDirectory()
  )
    file = path.join(dist, "index.html");
  res.setHeader(
    "Content-Type",
    {
      ".js": "application/javascript",
      ".css": "text/css",
      ".html": "text/html",
    }[path.extname(file)] || "application/octet-stream",
  );
  fs.createReadStream(file).pipe(res);
});
(async () => {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  console.log(
    JSON.stringify({
      playwrightVersion: require("@playwright/test/package.json").version,
      chromiumVersion: browser.version(),
    }),
  );
  const settings =
    fixture.device === "iphone"
      ? devices["iPhone 15 Pro"]
      : fixture.device === "pixel"
        ? devices["Pixel 7"]
        : { viewport: { width: 1440, height: 1050 } };
  const { defaultBrowserType, ...options } = settings;
  const context = await browser.newContext(options);
  const activate = (locator) =>
    fixture.device === "desktop"
      ? locator.click({ timeout: 10000 })
      : locator.tap({ timeout: 10000 });
  let page;
  try {
    await context.route("**/*", (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.startsWith("/api/"))
        return route.continue({ url: origin + url.pathname + url.search });
      return url.origin === origin
        ? route.continue()
        : route.fulfill({ status: 204, body: "" });
    });
    page = await context.newPage();
    page.on("pageerror", (error) => console.error("PAGE_ERROR", error.message));
    await page.goto(`${origin}/experiments`);
    await page.getByPlaceholder("Buscar", { exact: true }).fill(fixture.name);
    await expect(page.getByText(fixture.name, { exact: true })).toBeVisible();
    await page
      .locator("select")
      .filter({ has: page.locator('option[value="FAILED"]') })
      .selectOption("FAILED");
    await expect(page.getByText(fixture.name, { exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Corrigir e retomar" }).first(),
    ).toBeVisible();
    await page.screenshot({
      path: path.join(evidence, `${fixture.device}-failed.png`),
      fullPage: false,
    });
    const readiness = await (
      await context.request.get(
        `${origin}/api/experiments/${fixture.experimentId}/readiness`,
      )
    ).json();
    assert.equal(readiness.hasCreatives, false);
    assert(
      readiness.issues.some((issue) => issue.description.includes("202/125")),
    );
    const failedApprove = await context.request.patch(
      `${origin}/api/creatives/${fixture.creativeId}/status`,
      { data: { status: "READY" } },
    );
    assert.equal(failedApprove.status(), 409);
    await page.goto(
      `${origin}/experiments/${fixture.experimentId}?tab=creatives`,
    );
    const original = page
      .locator("article.creative-card")
      .filter({ hasText: "QA copy longa" });
    const create = original.getByRole("button", {
      name: "Criar nova versão",
      exact: true,
    });
    if (await create.count()) await activate(create);
    else
      await activate(
        original.getByRole("button", { name: "Editar", exact: true }),
      );
    await expect(page.getByText(/202\/125 caracteres/)).toBeVisible();
    await page
      .getByLabel("Headline", { exact: false })
      .last()
      .fill("QA primeiro ajuste");
    await page
      .getByLabel("Texto principal", { exact: false })
      .last()
      .fill(
        "Ruído visual ao se arrumar? Descubra seu 1º ajuste MUSA com o que já tem. Comece agora.",
      );
    const saveButton = page.getByRole("button", {
      name: "Salvar nova versão",
      exact: true,
    });
    await expect(saveButton).toBeEnabled();
    await expect(saveButton).toBeInViewport({ ratio: 1 });
    console.log(
      "LOCAL_SAVE_VIEWPORT",
      await saveButton.evaluate((el) => {
        const r = el.getBoundingClientRect();
        return {
          rect: r.toJSON(),
          viewport: [innerWidth, innerHeight],
          visualScale: visualViewport.scale,
          offsetTop: visualViewport.offsetTop,
          hit: document
            .elementFromPoint(r.x + r.width / 2, r.y + r.height / 2)
            ?.outerHTML.slice(0, 180),
        };
      }),
    );
    await page.screenshot({
      path: path.join(evidence, `${fixture.device}-editing.png`),
      fullPage: false,
    });
    const [createdResponse] = await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().endsWith(`/api/creatives/${fixture.creativeId}/versions`) &&
          r.request().method() === "POST",
      ),
      activate(saveButton),
    ]);
    assert(createdResponse.ok(), await createdResponse.text());
    const created = await createdResponse.json();
    assert.equal(created.status, "DRAFT");
    assert.equal(created.agentReviewStatus, "PENDING");
    const premature = await context.request.patch(
      `${origin}/api/creatives/${created.id}/status`,
      { data: { status: "READY" } },
    );
    assert.equal(premature.status(), 409);
    const review = await context.request.post(
      `${origin}/api/internal/creatives/${created.id}/agent-review/result`,
      {
        data: {
          decision: "APPROVED",
          attentionScore: 90,
          clarityScore: 90,
          desireScore: 90,
          credibilityScore: 90,
          actionScore: 90,
          copyAssessment: "LOCAL_TEST_DOUBLE: copy válida.",
          commercialAestheticAssessment: "LOCAL_TEST_DOUBLE: mídia preservada.",
          destinationIntegrationAssessment: "LOCAL_TEST_DOUBLE: destino local.",
          summary: "LOCAL_TEST_DOUBLE: sem validação comercial real.",
          issuesJson: "[]",
          recommendationsJson: "[]",
          model: "LOCAL_TEST_DOUBLE",
          requestJson: "{}",
          responseJson: "{}",
          costUsd: 0,
        },
      },
    );
    assert(review.ok(), await review.text());
    await page.reload();
    const revised = page
      .locator("article.creative-card")
      .filter({ hasText: "QA primeiro ajuste" });
    const [approval] = await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().endsWith(`/api/creatives/${created.id}/status`) &&
          r.request().method() === "PATCH",
      ),
      activate(revised.getByRole("button", { name: "Aprovar", exact: true })),
    ]);
    assert(approval.ok());
    await expect(revised.getByText("Aprovado", { exact: true })).toBeVisible();
    const ready = await (
      await context.request.get(
        `${origin}/api/facebook-campaigns/experiments/${fixture.experimentId}/creatives-ready`,
      )
    ).json();
    assert.deepEqual(
      ready.map((c) => c.id),
      [created.id],
    );
    assert.equal(ready[0].audibleApprovedVideo, true);
    await page.screenshot({
      path: path.join(evidence, `${fixture.device}-approved.png`),
      fullPage: false,
    });
    console.log(
      JSON.stringify({
        device: fixture.device,
        status: "PASS",
        realLocalBackend: true,
        copyApprovedThroughUi: true,
        realMetaCalls: 0,
      }),
    );
  } catch (error) {
    if (page) {
      console.error(
        "LOCAL_UI_STATE",
        (await page.locator("body").innerText()).slice(-7000),
      );
      await page.screenshot({
        path: path.join(evidence, `${fixture.device}-error.png`),
        fullPage: false,
      });
    }
    throw error;
  } finally {
    await context.close();
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
  server.close();
});
