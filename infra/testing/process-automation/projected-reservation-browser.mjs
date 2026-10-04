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
            cycleId: 92039,
            experimentId: 92039,
            chainDefinitionId: 92014,
            productVersion: "local-v1",
            status: "OPEN",
            previousLearning: [],
          },
        });
      return route.fulfill({ json: [] });
    });
    const open = (process) =>
      page.goto(
        `${base}/products/92039/value-chain-history/processes/${process}/activities?chainId=92014&learningCycleId=92039&sourceReference=experiment%3A92039`,
      );
    await open(92001);
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Encerrado com pendências", { exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(
      panel.getByText(/não está publicada nem fixada/),
    ).toBeVisible();
    await panel.getByRole("button", { name: "Histórico da execução" }).click();
    await expect(panel.getByText(/não está publicada nem fixada/)).toHaveCount(
      2,
    );
    await panel.screenshot({
      path: `${output}/${name}-projected-old-closed.png`,
    });
    await open(92006);
    await expect(
      panel.getByText("Aguardando condições", { exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(panel.getByText(/Estratégia vigente pendente/)).toBeVisible();
    await expect(
      panel.getByText("Esta execução aguarda outro processo deste produto", {
        exact: true,
      }),
    ).toHaveCount(0);
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.equal(errors.length, 0, errors.join("\n"));
    assert.equal(
      mutations.length,
      0,
      "Consultar não pode solicitar tarefa, campanha nem aprovar objetivo.",
    );
    await panel.screenshot({
      path: `${output}/${name}-projected-next-gate.png`,
    });
    await page.unrouteAll({ behavior: "wait" });
    await context.close();
    console.log(
      `PASS ${name}: reserva projetada encerrada, histórico preservado, gate próprio e nenhuma chamada paga`,
    );
  }
} finally {
  await browser.close();
}
