import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const output =
  process.env.PROCESS_TEST_ARTIFACTS || "artifacts/process-automation/browser";
await mkdir(output, { recursive: true });
const base = "http://127.0.0.1:4173";
const browser = await chromium.launch({
  ...(process.env.PROCESS_TEST_BROWSER === "bundled"
    ? {}
    : { executablePath: "/usr/bin/chromium" }),
  headless: true,
  args: ["--no-sandbox"],
});

try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const errors = [];
    const mutations = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (request) => {
      if (request.method() === "POST" && request.url().includes("/api/"))
        mutations.push(request.url());
    });
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (
        url.pathname.includes("/automation/v1") ||
        url.pathname.endsWith("/activity-executions")
      )
        return route.continue({ url: base + url.pathname + url.search });
      if (url.pathname.endsWith("/process-context"))
        return route.fulfill({
          json: {
            cycleId: 92031,
            experimentId: 92031,
            chainDefinitionId: 92014,
            productVersion: "local-v1",
            status: "OPEN",
            previousLearning: [],
          },
        });
      return route.fulfill({ json: [] });
    });
    await page.goto(
      `${base}/products/92031/value-chain-history/processes/92002/activities?chainId=92014&learningCycleId=92031`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Encerrado com pendências", { exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(
      panel.getByText(/não está publicada nem fixada/),
    ).toBeVisible();
    await expect(
      panel.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    await panel.getByRole("button", { name: "Histórico da execução" }).click();
    await expect(panel.getByText(/não está publicada nem fixada/)).toHaveCount(
      2,
    );
    assert.equal(
      mutations.length,
      0,
      "A consulta não pode disparar trabalho nem aprovar atividade.",
    );
    assert.equal(errors.length, 0, errors.join("\n"));
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    await panel.screenshot({ path: `${output}/${name}-retired-version.png` });
    await page.unrouteAll({ behavior: "wait" });
    await context.close();
    console.log(
      `PASS ${name}: versão encerrada, causa explícita, diário preservado e sem retomada/disparo`,
    );
  }
} finally {
  await browser.close();
}
