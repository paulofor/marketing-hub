// Homologa o build publicado localmente com callback real do worker e backend simulado.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, writeFile, mkdir } = require("node:fs/promises");
const { createServer } = require("node:http");
const path = require("node:path");
const assert = require("node:assert/strict");
(async () => {
  const fixture = JSON.parse(
    await readFile(
      process.env.ARGOS_PUBLIC_EVIDENCE_FIXTURE ||
        "/tmp/argos-online/worker-fixture.json",
      "utf8",
    ),
  );
  const output =
    process.env.ARGOS_BROWSER_OUTPUT || "/tmp/argos-online/browser";
  await mkdir(output, { recursive: true });
  const root = path.resolve("frontend/dist");
  const server = createServer(async (req, res) => {
    try {
      const url = new URL(req.url, "http://localhost");
      const file = url.pathname.startsWith("/assets/")
        ? path.join(root, url.pathname)
        : path.join(root, "index.html");
      assert(file.startsWith(root + path.sep));
      res.setHeader(
        "Content-Type",
        file.endsWith(".js")
          ? "application/javascript"
          : file.endsWith(".css")
            ? "text/css"
            : "text/html",
      );
      res.end(await readFile(file));
    } catch {
      res.statusCode = 404;
      res.end();
    }
  });
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    headless: true,
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [name, profile] of [
      ["desktop", { viewport: { width: 1366, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      let failed = false;
      let posts = 0,
        adopted = false,
        completed = false;
      const id = fixture.job.cycleId;
      const gap = () => ({
        cycleId: id,
        applicable: true,
        cycleStatus: failed
          ? "FAILED"
          : completed
            ? "COMPLETED"
            : adopted
              ? "READY_FOR_RESEARCH"
              : "AWAITING_CUSTOMER_EVIDENCE",
        stageCode: completed
          ? "opportunity-gate"
          : adopted
            ? "candidate-gap-deepening"
            : "customer-evidence",
        minimumInterviews: adopted ? 0 : 5,
        maximumInterviews: 8,
        interviewCount: 0,
        purchasedCount: 0,
        abandonedCount: 0,
        coveredOpportunityIds: [],
        missingOpportunityIds: [],
        readyForResearch: adopted,
        maximumPublicQueriesPerAttempt: 12,
        maximumAttempts: 2,
        maximumModelInvocations: 4,
        maximumSearchCostUsd: 0.12,
        searchPricingSource: "https://brave.com/search/api/",
        searchPricingObservedOn: "2026-09-23",
        guidance: adopted
          ? "Pesquisa pública automatizada com limites preservados."
          : "Entrevistas indisponíveis; é possível adotar pesquisa pública.",
        interviews: [],
        evidencePolicy: adopted
          ? "PUBLIC_SOURCES_V1"
          : "CONSENTED_INTERVIEWS_V1",
        canAdoptPublicEvidence: !adopted,
        canResumePublicResearch: failed,
      });
      await page.route("**/*", async (route) => {
        const req = route.request();
        const url = new URL(req.url());
        if (url.pathname.startsWith("/api/")) {
          if (req.method() !== "GET") {
            assert.equal(req.method(), "POST");
            assert.equal(
              url.pathname,
              `/api/product-discovery/v1/cycles/${id}/gap-deepening/public-research${failed ? "/resume" : ""}`,
            );
            assert.equal(req.postData(), null);
            posts++;
            await new Promise((r) => setTimeout(r, 400));
            if (posts === 1)
              return route.fulfill({
                status: 503,
                json: { detail: "Indisponibilidade temporária simulada" },
              });
            adopted = true;
            failed = false;
            return route.fulfill({ json: gap() });
          }
          if (url.pathname.endsWith("/gap-deepening"))
            return route.fulfill({ json: gap() });
          if (url.pathname === `/api/product-discovery/v1/cycles/${id}`)
            return route.fulfill({
              json: {
                cycle: {
                  id,
                  theme: fixture.job.theme,
                  status: gap().cycleStatus,
                  stageCode: gap().stageCode,
                  createdAt: "2026-09-24T00:00:00Z",
                  updatedAt: "2026-09-24T00:00:00Z",
                  decisionSummary: completed
                    ? fixture.result.decisionSummary
                    : "Fontes em preparação.",
                },
                opportunities: fixture.result.opportunities.map((c, i) => ({
                  ...c,
                  id: 95000 + i,
                  cycleId: id,
                  evidenceJson: completed ? c.evidenceJson : "{}",
                  createdAt: "2026-09-24T00:00:00Z",
                  updatedAt: "2026-09-24T00:00:00Z",
                })),
              },
            });
          return route.fulfill({ json: [] });
        }
        if (url.origin !== origin) return route.abort();
        return route.continue();
      });
      await page.goto(`${origin}/product-discovery/cycles/${id}`);
      const button = page.getByRole("button", {
        name: "Usar pesquisa pública automatizada",
      });
      await expect(button).toBeEnabled();
      await button.scrollIntoViewIfNeeded();
      const box = await button.boundingBox();
      assert(box.width >= 44 && box.height >= 38);
      assert(box.x >= 0 && box.x + box.width <= page.viewportSize().width + 1);
      await page.screenshot({ path: path.join(output, `${name}-before.png`) });
      await button.click();
      await expect(button).toBeDisabled();
      await expect(
        page
          .getByRole("alert")
          .filter({ hasText: "Indisponibilidade temporária simulada" }),
      ).toBeVisible();
      await expect(button).toBeEnabled();
      await button.click();
      await expect(button).toBeDisabled();
      await expect(
        page.getByText("Pesquisa pública automatizada", { exact: true }),
      ).toBeVisible();
      await expect(button).toHaveCount(0);
      await expect(
        page.getByRole("button", { name: "Registrar entrevista" }),
      ).toHaveCount(0);
      await expect(
        page.getByText(/Sua liberação não comprova comportamento de compra/),
      ).toBeVisible();
      await expect(
        page.getByText(/Os critérios comportamentais foram atendidos/),
      ).toHaveCount(0);
      failed = true;
      const resumeButton = page.getByRole("button", {
        name: "Retomar aprofundamento após correção",
      });
      await expect(resumeButton).toBeVisible({ timeout: 20000 });
      await resumeButton.click();
      await expect(page.getByRole("button", { name: "Retomando..." })).toBeDisabled();
      await expect(resumeButton).toHaveCount(0);
      assert.equal(
        new URL(page.url()).pathname,
        `/product-discovery/cycles/${id}`,
      );
      completed = true;
      await expect(
        page.getByRole("heading", { name: "Observações públicas de Argos" }),
      ).toHaveCount(2, { timeout: 20000 });
      await expect(
        page.getByText(fixture.result.decisionSummary, { exact: true }),
      ).toBeVisible();
      const source = page
        .getByRole("link", { name: "Fonte P1", exact: true })
        .first();
      await expect(source).toHaveAttribute(
        "href",
        "https://community.example.org/report",
      );
      await expect(source).toHaveAttribute("target", "_blank");
      await source.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, `${name}-after.png`) });
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth + 1,
        ),
        true,
      );
      assert.equal(posts, 3);
      assert.deepEqual(errors, []);
      results.push({
        profile: name,
        checks: [
          "action",
          "loading",
          "error",
          "retry",
          "no-duplicate",
          "policy",
          "polling",
          "evidence",
          "mobile-layout",
          "resume-same-cycle",
        ],
        syntheticMutations: posts,
        pageErrors: errors,
      });
      await context.close();
    }
    await writeFile(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results));
  } finally {
    await browser.close();
    await new Promise((r) => server.close(r));
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
