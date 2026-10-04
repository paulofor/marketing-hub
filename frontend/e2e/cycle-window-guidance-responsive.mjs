import assert from "node:assert/strict";
import { readFile, mkdir } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:15173";
assert.equal(new URL(base).hostname, "127.0.0.1");
const fixtures = process.env.CYCLE_WINDOW_FIXTURES;
assert.ok(fixtures, "Informe os contratos exportados pelo teste Java real.");
const output = process.env.CYCLE_WINDOW_EVIDENCE || "/tmp/cycle-window-browser";
await mkdir(output, { recursive: true });
const api = "/api/business-process-chains/learning-cycles/v1";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
let checks = 0;
try {
  for (const scenario of ["legacy", "historical", "current-policy"]) {
    const cycle = JSON.parse(
      await readFile(`${fixtures}/${scenario}.json`, "utf8"),
    );
    const catalog = {
      processDefinitionId: cycle.processDefinitionId,
      version: 1,
      diagram: { nodes: [], flows: [] },
      returnTargets: [],
      experiments: [],
      entry: {
        chainDefinitionId: cycle.chainDefinitionId,
        chainName: "Cadeia sintética local",
        parentProcessDefinitionId: 901,
        parentProcessName: "Venda e aprendizado",
        sequenceNumber: 6,
        activityId: "learningCycle",
        activityName: "Conduzir ciclo",
        activitySequenceNumber: 4,
        processDefinitionId: cycle.processDefinitionId,
        processName: "Ciclos de aprendizado",
        integrated: true,
        canStartCycle: false,
        guidance: "Conservar os resultados registrados.",
        workspaceUrl:
          "/business-process-chains/learning-cycles?productId=" +
          cycle.productId,
        actionLabel: "Abrir ciclo",
        parentUrl: "/products/" + cycle.productId + "/value-chain-history",
        returnRoutes: [],
      },
    };
    for (const [profile, device] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext({
        ...device,
        timezoneId: "UTC",
      });
      const page = await context.newPage();
      const errors = [],
        writes = [],
        external = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await page.route("**/*", async (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (url.pathname.startsWith("/api/")) {
          if (request.method() !== "GET") {
            writes.push({ path: url.pathname, method: request.method() });
            return route.fulfill({
              status: 409,
              json: { message: "Teste somente leitura" },
            });
          }
          let data = [];
          if (url.pathname === "/api/products")
            data = [{ id: cycle.productId, internalName: "Produto sintético" }];
          else if (url.pathname === "/api/business-process-chains")
            data = [
              {
                id: cycle.chainDefinitionId,
                name: catalog.entry.chainName,
                versionNumber: 1,
              },
            ];
          else if (url.pathname === api + "/catalog") data = catalog;
          else if (url.pathname === `${api}/products/${cycle.productId}`)
            data = [cycle];
          else if (url.pathname.endsWith("/decision-proposal"))
            data = {
              id: null,
              cycleId: cycle.id,
              cycleRevision: cycle.revision,
              status: "WAITING",
              agentKey: "experiment-strategist",
              agentName: "Atena",
              agentId: 4,
              automaticExecutionEnabled: false,
              activityDefinitionId: null,
              proposal: null,
              operatorName: "Operador sintético",
              error: null,
              createdAt: null,
              finishedAt: null,
              approvedAt: null,
              approvedEventId: null,
            };
          return route.fulfill({ json: data });
        }
        if (url.origin !== base) {
          external.push(url.origin);
          return route.abort();
        }
        return route.continue();
      });
      await page.goto(
        `${base}/business-process-chains/learning-cycles?productId=${cycle.productId}&chainId=${cycle.chainDefinitionId}&cycleId=${cycle.id}`,
        { waitUntil: "networkidle" },
      );
      await expect(
        page.getByRole("heading", {
          name: `Ciclo #${cycle.id} · experimento #${cycle.experimentId}`,
        }),
      ).toBeVisible();
      const form = page.getByRole("form", {
        name: "Revalidar janela comercial",
      });
      if (cycle.windowRevalidation.available) await expect(form).toBeVisible();
      else {
        await expect(form).toHaveCount(0);
        await expect(
          page.getByText(cycle.windowRevalidation.reason, { exact: false }),
        ).toBeVisible();
      }
      assert.deepEqual(errors, []);
      assert.deepEqual(writes, []);
      assert.deepEqual(external, []);
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth + 1,
        ),
      );
      await page.screenshot({
        path: `${output}/${scenario}-${profile}.png`,
        fullPage: true,
      });
      checks++;
      await context.close();
    }
  }
  console.log(
    JSON.stringify({
      checks,
      result: "passed",
      sources:
        "Java real com repositórios simulados; somente APIs locais interceptadas",
    }),
  );
} finally {
  await browser.close();
}
