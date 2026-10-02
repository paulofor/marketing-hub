import assert from "node:assert/strict";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const fixture = JSON.parse(
  await readFile(process.env.PREFLIGHT_FIXTURE_RESULT, "utf8"),
);
assert.ok(
  fixture.stale.currentEvidenceBlockReason.includes("evidência técnica"),
);
const renewalFixture = JSON.parse(
  await readFile(process.env.PREFLIGHT_RENEWAL_RESULT, "utf8"),
);
const output = process.env.PREFLIGHT_EVIDENCE_DIR;
const base = process.env.PREFLIGHT_FRONTEND_URL || "http://127.0.0.1:15173";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [deviceName, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    for (const state of ["stale", "current"]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const errors = [],
        writes = [],
        external = [];
      const runId = fixture[state].runId;
      let renewed = false;
      page.on("pageerror", (error) => errors.push(error.message));
      page.on("console", (message) => {
        if (message.type() === "error") errors.push(message.text());
      });
      await page.route("**/*", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.startsWith("/api/")) {
          if (request.method() !== "GET") writes.push(url.pathname);
          if (
            url.pathname ===
              `/api/experiment-runs/${runId}/technical-homologation-renewal` &&
            request.method() === "POST"
          ) {
            assert.equal(state, "stale");
            renewed = true;
            return route.fulfill({ json: renewalFixture });
          }
          if (url.pathname === "/api/experiments/92008")
            return route.fulfill({
              json: {
                id: 92008,
                name: "Experimento sintético de homologação",
                productId: 92011,
                creationSource: "SYSTEM_FLOW",
                experimentType: "LOW_TICKET_PRODUCT",
                campaignObjective: "SALES",
                platform: "FACEBOOK",
                status: "INVALIDATED",
                unitPrice: 67,
                dailyBudget: 20,
                mediaSpendLimit: 100,
              },
            });
          if (url.pathname === "/api/experiments/92008/runs")
            return route.fulfill({
              json: [
                {
                  id: renewed ? renewalFixture.runId : runId,
                  experimentId: 92008,
                  runNumber: renewed ? 2 : 1,
                  mode: "PRODUCTION",
                  status: renewed ? renewalFixture.runStatus : "COMPLETED",
                  evidenceValidity: renewed
                    ? "NOT_EVALUATED"
                    : "COMMERCIALLY_VALID",
                  dataQualityStatus: renewed ? "UNKNOWN" : "VALID",
                  stopPolicy: "MANUAL_ONLY",
                  requestedAt: "2026-10-02T00:00:00Z",
                },
              ],
            });
          if (
            url.pathname ===
            `/api/experiment-runs/${renewalFixture.runId}/preflight`
          )
            return route.fulfill({ json: renewalFixture });
          if (url.pathname === `/api/experiment-runs/${runId}/preflight`)
            return route.fulfill({ json: fixture[state] });
          if (url.pathname.includes("configuration-status"))
            return route.fulfill({ json: { accounts: [] } });
          if (url.pathname.includes("post-deploy-monitor"))
            return route.fulfill({ json: null });
          if (
            url.pathname.includes("metrics") ||
            url.pathname.includes("readiness")
          )
            return route.fulfill({ json: {} });
          return route.fulfill({ json: [] });
        }
        if (url.origin !== base) {
          external.push(url.href);
          return route.abort();
        }
        return route.continue();
      });
      await page.goto(`${base}/experiments/92008?tab=execucao`, {
        waitUntil: "networkidle",
      });
      const panel = page.locator(".experiment-run-panel");
      try {
        await panel.waitFor();
      } catch (error) {
        await page.screenshot({
          path: `${output}/${deviceName}-${state}-failure.png`,
        });
        await writeFile(
          `${output}/failure.json`,
          JSON.stringify(
            {
              deviceName,
              state,
              errors,
              writes,
              external,
              text: await page.locator("body").innerText(),
            },
            null,
            2,
          ),
        );
        throw error;
      }
      if (state === "stale") {
        const alert = panel.getByRole("alert");
        await alert.waitFor();
        assert.ok(
          (await alert.innerText()).includes(
            fixture.stale.currentEvidenceBlockReason,
          ),
        );
        const bounds = await alert.boundingBox();
        assert.ok(bounds && bounds.width <= device.viewport.width);
      } else {
        await expect(panel).toContainText("Sem bloqueadores");
        assert.equal(await panel.getByRole("alert").count(), 0);
      }
      assert.equal(
        await panel.getByRole("button", { name: "Criar run" }).count(),
        0,
      );
      assert.deepEqual(errors, []);
      assert.deepEqual(writes, []);
      assert.deepEqual(external, []);
      await panel.screenshot({ path: `${output}/${deviceName}-${state}.png` });
      if (state === "stale") {
        await panel
          .getByRole("button", { name: "Renovar homologação técnica" })
          .click();
        await expect(panel).toContainText("Preflight pendente");
        await expect(
          panel.getByRole("button", { name: "Renovar homologação técnica" }),
        ).toHaveCount(0);
        assert.deepEqual(writes, [
          `/api/experiment-runs/${runId}/technical-homologation-renewal`,
        ]);
        await panel.screenshot({ path: `${output}/${deviceName}-renewed.png` });
      }
      results.push({
        device: deviceName,
        state,
        runId,
        renewedRunId: renewed ? renewalFixture.runId : null,
        errors,
        writes,
        external,
      });
      await context.close();
    }
  }
} finally {
  await browser.close();
}
await writeFile(`${output}/result.json`, JSON.stringify(results, null, 2));
console.log(JSON.stringify(results));
