import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const backend = "http://127.0.0.1:18092";
const base = "http://127.0.0.1:4173";
const output =
  process.env.PROCESS_TEST_ARTIFACTS ||
  "artifacts/process-automation/initial-planning";
await mkdir(output, { recursive: true });
async function request(path, body) {
  const response = await fetch(backend + path, {
    method: body === undefined ? "GET" : "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Process-Worker-Token": "process-fixture-only",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json();
  assert.equal(response.status, 200, JSON.stringify(data));
  return data;
}
const tick = (id) =>
  request(
    `/api/internal/business-processes/automation/v1/stage-executions/${id}/reconcile`,
    {},
  );
const browser = await chromium.launch({
  ...(process.env.PROCESS_TEST_BROWSER === "bundled"
    ? {}
    : { executablePath: "/usr/bin/chromium" }),
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, product, options] of [
    ["desktop", 92050, { viewport: { width: 1440, height: 1000 } }],
    ["iphone", 92052, devices["iPhone 15 Pro"]],
    ["pixel", 92053, devices["Pixel 7"]],
  ]) {
    const input = { chainId: 92014, sourceReference: `experiment:${product}` };
    const root = (process) =>
      `/api/business-processes/${process}/products/${product}/automation/v1`;
    const state = (process) =>
      request(`${root(process)}?${new URLSearchParams(input)}`);
    await request(`/fixture/products/${product}`, { blockedProcessId: 92049 });
    const context = await browser.newContext({
      ...options,
      reducedMotion: "reduce",
    });
    const page = await context.newPage();
    const errors = [],
      mutations = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/api/"))
        mutations.push(new URL(req.url()).pathname);
    });
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (
        url.pathname.includes("/automation/v1") ||
        url.pathname.endsWith("/activity-executions")
      )
        return route.continue({ url: base + url.pathname + url.search });
      return route.fulfill({ json: [] });
    });
    const open = async (process) => {
      await page.goto(
        `${base}/products/${product}/value-chain-history/processes/${process}/activities?${new URLSearchParams(input)}`,
      );
    };
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await open(92049);
    await panel
      .getByRole("button", { name: "Executar processo", exact: true })
      .click();
    await expect.poll(async () => (await state(92049)).id).not.toBeNull();
    const communication = await state(92049);
    assert.equal((await tick(communication.id)).status, "WAITING_INPUT");
    await open(92050);
    await panel
      .getByRole("button", { name: "Executar processo", exact: true })
      .click();
    await expect.poll(async () => (await state(92050)).id).not.toBeNull();
    const planning = await state(92050);
    assert.equal(planning.status, "QUEUED");
    assert.equal(planning.queueBlocker, null);
    assert.equal((await state(92049)).status, "WAITING_INPUT");
    await expect(panel.getByLabel("Processo que reserva a fila")).toHaveCount(
      0,
    );
    await panel.screenshot({
      path: `${output}/${name}-initial-planning-queue.png`,
    });
    await tick(planning.id);
    await request(`/fixture/${product}/92050/a`, {
      status: "COMPLETED",
      achieved: true,
    });
    await tick(planning.id);
    await request(`/fixture/${product}/92050/b`, {
      status: "COMPLETED",
      achieved: true,
    });
    await tick(planning.id);
    assert.equal((await tick(planning.id)).status, "COMPLETED");
    await tick(communication.id);
    assert.equal(
      (await state(92049)).currentActivityId,
      "communicationContract",
    );
    await open(92049);
    await expect(
      panel.getByText(new RegExp(`Execução #${communication.id} ·`)),
    ).toBeVisible();
    await panel.screenshot({
      path: `${output}/${name}-communication-started.png`,
    });
    await request(`/fixture/${product}/92049/communicationContract`, {
      status: "COMPLETED",
      achieved: true,
    });
    await tick(communication.id);
    await request(`/fixture/${product}/92049/b`, {
      status: "COMPLETED",
      achieved: true,
    });
    await tick(communication.id);
    const finished = await tick(communication.id);
    assert.equal(finished.status, "COMPLETED");
    assert.equal(finished.learningCycleId, null);
    assert.equal(finished.sourceReference, input.sourceReference);
    assert.equal(finished.costCoverage, "NOT_REPORTED");
    assert.deepEqual(
      mutations,
      [root(92049), root(92050)],
      "Somente os dois comandos originais; nenhuma pausa ou retomada manual.",
    );
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    await context.close();
    console.log(
      `PASS ${name}: primeiro planejamento e comunicação continuam na mesma referência sem fila circular, pause ou duplicação`,
    );
  }
} finally {
  await browser.close();
}
