// Confere os pixels renderizados na auditoria real da interface, com APIs segregadas e sem escritas.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, writeFile, mkdir } = require("node:fs/promises");
const { createServer } = require("node:http");
const { createHash } = require("node:crypto");
const path = require("node:path");
const assert = require("node:assert/strict");
const fixture = require("../process-context-copy/fixture.json");

(async () => {
  const png = await readFile(process.env.VEGA_CREATIVE_PREVIEW);
  const sha256 = createHash("sha256").update(png).digest("hex");
  const output = path.dirname(process.env.VEGA_CREATIVE_PREVIEW);
  const uiDir = path.resolve("frontend/dist");
  await mkdir(output, { recursive: true });
  const history = structuredClone(fixture.history);
  const activity = history.activities[0];
  activity.tasks = [
    {
      taskId: 910403,
      processDefinitionId: history.selectedProcessDefinitionId,
      processVersionNumber: 7,
      title: "Criativo privado de homologação local",
      status: "COMPLETED",
      sourceReference: "experiment:92092",
      assignedAgentKey: "communication-director",
      assignedAgentNickname: "Íris",
      productInternalName: "Vega sandbox",
      executionMode: "MODEL",
      createdAt: "2026-09-12T09:00:00Z",
      finishedAt: "2026-09-12T09:01:00Z",
      comments:
        "Peça local para verificar apresentação e pixels; não é tarefa produtiva.",
      visualEvidence: [
        {
          id: 910130,
          captureSessionId: "sandbox-creative",
          evidenceKey: "creative-1",
          evidenceType: "CREATIVE_RENDER",
          label: "Criativo estático local",
          deviceProfile: "CREATIVE_1080X1350",
          pageNumber: 1,
          viewportWidth: 1080,
          viewportHeight: 1350,
          pageHeightPx: 1350,
          scrollY: 0,
          sourceUrl: "https://sandbox.example/source",
          finalUrl: "https://sandbox.example/source",
          contentUrl: "/api/agent-tasks/910403/visual-evidence/910130/content",
          sizeBytes: png.length,
          sha256,
          capturedAt: "2026-09-12T09:01:00Z",
        },
      ],
    },
  ];
  activity.taskCount = 1;
  history.uniqueTaskCount = 1;
  history.activitiesWithTasksCount = 1;
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, "http://localhost").pathname;
      const file = name.startsWith("/assets/")
        ? path.join(uiDir, name)
        : path.join(uiDir, "index.html");
      assert(file.startsWith(uiDir + path.sep));
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
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
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
      const errors = [],
        unexpected = [];
      let imageRequests = 0;
      page.on("pageerror", (e) => errors.push(e.message));
      await page.route("**/*", (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (
          request.method() !== "GET" ||
          (!url.pathname.startsWith("/api/") && url.origin !== origin)
        ) {
          unexpected.push(request.method() + " " + url.pathname);
          return route.abort();
        }
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname.endsWith("/910130/content")) {
          imageRequests++;
          return route.fulfill({ contentType: "image/png", body: png });
        }
        if (url.pathname.endsWith("/process-context"))
          return route.fulfill({ json: fixture.cycle });
        if (url.pathname.includes("/value-chain-positions/"))
          return route.fulfill({ json: fixture.position });
        if (url.pathname.endsWith("/activity-executions"))
          return route.fulfill({ json: history });
        if (url.pathname.endsWith("/automation/v1"))
          return route.fulfill({ json: fixture.automation });
        if (
          url.pathname.endsWith("/execution-progress") ||
          [
            "/api/creatives/video-review",
            "/api/ops-monitor/v1/modules/availability",
            "/api/facebook/configuration-status",
          ].includes(url.pathname)
        )
          return route.fulfill({ json: [] });
        unexpected.push(url.pathname);
        return route.fulfill({ status: 501, json: {} });
      });
      await page.goto(
        `${origin}/products/92004/value-chain-history/processes/92063/activities?learningCycleId=92002&chainId=92014#activity-${activity.activityId}`,
      );
      const summary = page
        .locator("summary")
        .filter({ hasText: "Criativo privado de homologação local" });
      await expect(summary)
        .toBeVisible({ timeout: 15000 })
        .catch(async (e) => {
          await writeFile(
            path.join(output, "creative-browser-failure.txt"),
            await page.locator("body").innerText(),
          );
          console.error({ errors, unexpected, url: page.url() });
          await page.screenshot({
            path: path.join(output, "creative-browser-failure.png"),
          });
          throw e;
        });
      if (!(await summary.evaluate((element) => element.parentElement.open)))
        await summary.click();
      const image = page.getByRole("img", {
        name: "Criativo produzido — Criativo estático local",
      });
      await expect(image).toBeVisible();
      await image.evaluate(async (element) => {
        await element.decode();
      });
      const dimensions = await image.evaluate((element) => ({
        width: element.naturalWidth,
        height: element.naturalHeight,
        renderedWidth: element.getBoundingClientRect().width,
      }));
      assert.equal(dimensions.width, 1080);
      assert.equal(dimensions.height, 1350);
      assert(dimensions.renderedWidth > 100);
      await expect(
        page.getByRole("heading", { name: "Imagens finais do criativo" }),
      ).toBeVisible();
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth + 1,
        ),
        false,
      );
      assert(imageRequests > 0);
      assert.deepEqual(errors, []);
      assert.deepEqual(unexpected, []);
      await image.scrollIntoViewIfNeeded();
      await page.screenshot({
        path: path.join(output, `creative-${name}.png`),
      });
      results.push({ device: name, sha256, ...dimensions, errors, unexpected });
      await context.close();
    }
    await writeFile(
      path.join(output, "creative-browser.json"),
      JSON.stringify(results, null, 2) + "\n",
    );
    console.log(
      "PASS imagem final no build real: desktop, iPhone 15 Pro e Pixel 7; pixels decodificados, sem overflow nem escritas.",
    );
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
