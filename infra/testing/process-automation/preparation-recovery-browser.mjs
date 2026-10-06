import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const fixture = JSON.parse(
  await readFile(process.env.PREPARATION_RECOVERY_FIXTURE, "utf8"),
);
const { history, control, events } = fixture;
assert.equal(control.status, "WAITING_INPUT");
assert.equal(control.completedActivities, 0);
assert.match(control.updatedAt, /^\d{4}-\d{2}-\d{2}T/);
assert.equal(history.objectiveAchieved, false);
assert.equal(
  events.filter((event) => event.eventType === "PREREQUISITE_RECOVERED").length,
  1,
);
const recovery = events.find(
  (event) => event.eventType === "PREREQUISITE_RECOVERED",
);
assert.equal(recovery.details.strategistExecutionId, 96119);
assert.equal(recovery.details.financialExecutionId, 96171);
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output, "Informe o destino das evidências sintéticas.");
await mkdir(output, { recursive: true });
const base = process.env.PROCESS_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["localhost", "127.0.0.1"].includes(new URL(base).hostname));
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
    await page.routeWebSocket("**/*", (socket) => socket.close());
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname.startsWith("/api/")) {
        if (!["GET", "OPTIONS"].includes(request.method()))
          mutations.push(request.url());
        if (url.pathname.endsWith("/activity-executions"))
          return route.fulfill({ json: history });
        if (url.pathname.endsWith("/events"))
          return route.fulfill({ json: events });
        if (url.pathname.includes("/automation/v1"))
          return route.fulfill({ json: control });
        if (url.pathname.endsWith("/process-context"))
          return route.fulfill({ status: 204, body: "" });
        if (url.pathname.includes("video-review/summary"))
          return route.fulfill({ json: { humanDecisionRequired: 0 } });
        return route.fulfill({ json: [] });
      }
      if (url.origin !== base) return route.abort();
      return route.continue();
    });
    await page.goto(
      `${base}/products/${control.productId}/value-chain-history/processes/${control.processDefinitionId}/activities?chainId=${control.chainId}&sourceReference=${encodeURIComponent(control.sourceReference)}`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Aguardando condições", { exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(panel).toContainText(
      "Atividade 1 — Preparar comunicação · Íris",
    );
    await expect(panel).toContainText(control.reason);
    await panel.getByRole("button", { name: "Histórico da execução" }).click();
    await expect(panel.getByText(recovery.message)).toBeVisible();
    await expect(page.locator("#activity-a")).not.toContainText(
      "Objetivo da atividade atingido",
    );
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await panel.screenshot({
      path: `${output}/${name}-preparation-recovery.png`,
    });
    await page.close();
    console.log(
      `PASS ${name}: passagem auditável, pré-requisitos preservados e nenhuma mutação pela leitura`,
    );
  }
} finally {
  await browser.close();
}
