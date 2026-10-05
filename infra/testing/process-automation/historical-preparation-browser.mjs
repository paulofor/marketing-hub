import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const history = JSON.parse(
  await readFile(process.env.HISTORICAL_ACTIVITIES_FIXTURE, "utf8"),
);
const control = JSON.parse(
  await readFile(process.env.HISTORICAL_AUTOMATION_FIXTURE, "utf8"),
);
const base = process.env.HISTORICAL_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["127.0.0.1", "localhost"].includes(new URL(base).hostname));
assert.equal(history.productId, control.productId);
assert.equal(history.selectedProcessDefinitionId, control.processDefinitionId);
assert.equal(history.currentExecutionReference, control.sourceReference);
assert.equal(history.completedActivityCount, control.completedActivities);
assert.equal(history.objectiveAchieved, true);
assert.equal(control.status, "COMPLETED");
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output, "Informe destino segregado para as evidências locais.");
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const page = await browser.newPage(options);
    const errors = [];
    const mutations = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (request) => {
      if (
        request.method() !== "GET" &&
        request.method() !== "OPTIONS" &&
        request.url().includes("/api/")
      )
        mutations.push(request.url());
    });
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith("/activity-executions"))
        return route.fulfill({ json: history });
      if (url.pathname.includes("/automation/v1"))
        return route.fulfill({ json: control });
      if (url.pathname.endsWith("/process-context"))
        return route.fulfill({ status: 204, body: "" });
      if (url.pathname.includes("video-review/summary"))
        return route.fulfill({ json: { humanDecisionRequired: 0 } });
      return route.fulfill({ json: [] });
    });
    await page.route("**/ws/**", (route) => route.abort());
    await page.goto(
      `${base}/products/${history.productId}/value-chain-history/processes/${history.selectedProcessDefinitionId}/activities?chainId=${control.chainId}&sourceReference=${encodeURIComponent(control.sourceReference)}#activity-economics`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Processo concluído", { exact: true }),
    ).toBeVisible({
      timeout: 20000,
    });
    await expect(
      panel.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    await expect(
      page.getByText(
        `${history.selectedActivityCount} de ${history.selectedActivityCount} atividades concluídas`,
        { exact: true },
      ),
    ).toBeVisible();
    await expect(page.locator("#activity-economics")).toContainText(
      "Concluída",
    );
    assert(
      !(await page
        .getByText(/Atualize o plano financeiro vencido ou alterado/)
        .count()),
    );
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await page.screenshot({
      path: `${output}/${name}-historical-preparation.png`,
      fullPage: true,
    });
    await page.close();
    console.log(
      `PASS ${name}: ${history.selectedActivityCount} objetivos preservados, economia histórica e nenhuma tarefa ou chamada paga`,
    );
  }
} finally {
  await browser.close();
}
