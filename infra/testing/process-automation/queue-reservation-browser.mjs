import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const backend = "http://127.0.0.1:18092";
const base = "http://127.0.0.1:4173";
const product = Number(process.env.PROCESS_QUEUE_TEST_PRODUCT || 92039);
assert(Number.isSafeInteger(product) && product >= 92001 && product <= 92040);
const root = (process) =>
  `/api/business-processes/${process}/products/${product}/automation/v1`;
const input = { chainId: 92014, sourceReference: `experiment:${product}` };
const output =
  process.env.PROCESS_TEST_ARTIFACTS ||
  "artifacts/process-automation/queue-browser";
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
const waiting = await request(root(92002), input);
await tick(waiting.id);
await request(`/fixture/${product}/92002/a`, {
  status: "COMPLETED",
  achieved: true,
});
assert.equal((await tick(waiting.id)).status, "WAITING_HUMAN");
const queued = await request(root(92001), input);
assert.equal((await tick(queued.id)).status, "QUEUED");
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
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const mutations = [],
      errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/api/"))
        mutations.push(req.url());
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
    await page.goto(
      `${base}/products/${product}/value-chain-history/processes/92001/activities?${new URLSearchParams(input)}`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    const reserve = panel.getByLabel("Processo que reserva a fila");
    await expect(reserve).toBeVisible({ timeout: 20000 });
    await expect(
      reserve.getByText(
        new RegExp(`Execução #${waiting.id} · Processo de teste 92002`),
      ),
    ).toBeVisible();
    const link = reserve.getByRole("link", {
      name: "Ver processo que reserva a fila",
    });
    await expect(link).toHaveAttribute(
      "href",
      `/products/${product}/value-chain-history/processes/92002/activities?chainId=92014&sourceReference=experiment%3A${product}#activity-b`,
    );
    await panel.getByText("Ver contexto completo", { exact: true }).click();
    await expect(
      panel.getByText(
        new RegExp(`Execução que reserva a fila: #${waiting.id}`),
      ),
    ).toBeVisible();
    assert(
      (await panel.textContent()).includes(
        `Link do processo: ${base}/products/${product}/value-chain-history/processes/92001/activities?chainId=92014&sourceReference=experiment%3A${product}`,
      ),
    );
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    await panel.screenshot({ path: `${output}/${name}-queue-reservation.png` });
    await link.click();
    await expect(
      panel.getByText(
        `Execução #${waiting.id} · O progresso e o histórico ficam salvos mesmo com esta tela fechada.`,
      ),
    ).toBeVisible();
    assert.equal(
      new URL(page.url()).searchParams.get("sourceReference"),
      input.sourceReference,
    );
    assert.equal(
      mutations.length,
      0,
      "Navegar pela reserva não executa nem pausa processos.",
    );
    assert.equal(errors.length, 0, errors.join("\n"));
    await context.close();
    console.log(
      `PASS ${name}: reserva, contexto copiado, link exato e leitura sem comandos`,
    );
  }
  await request(root(92002) + `/${waiting.id}/pause`, {});
  assert.equal((await tick(waiting.id)).status, "PAUSED");
  const refreshed = await request(
    root(92001) + "?" + new URLSearchParams(input),
  );
  assert.equal(refreshed.queueBlocker, null);
  assert.equal(refreshed.status, "QUEUED");
  assert.equal((await tick(queued.id)).status, "WAITING_ACTIVITY");
  console.log(
    "PASS pausa auditável remove somente a reserva; a próxima execução depende da conciliação",
  );
} finally {
  await browser.close();
}
