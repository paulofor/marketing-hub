import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const output =
  process.env.PROCESS_TEST_ARTIFACTS || "artifacts/process-automation/browser";
await mkdir(output, { recursive: true });
const base = "http://127.0.0.1:4173";
const backend = "http://127.0.0.1:18092";
async function post(path, body = {}) {
  const r = await fetch(backend + path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Process-Worker-Token": "process-fixture-only",
    },
    body: JSON.stringify(body),
  });
  assert.equal(r.status, 200, await r.clone().text());
  return r.json();
}

// O proxy HTTP local acompanha o cancelamento nativo sem responder novamente a uma rota encerrada.
function forwardLocal(route) {
  const url = new URL(route.request().url());
  return route.continue({ url: base + url.pathname + url.search });
}
const browser = await chromium.launch({
  ...(process.env.PROCESS_TEST_BROWSER === "bundled"
    ? {}
    : { executablePath: "/usr/bin/chromium" }),
  headless: true,
  args: ["--no-sandbox"],
});
try {
  let index = 0;
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const product = 92021 + index++;
    const process = 92001;
    const ctx = await browser.newContext({
      ...options,
      reducedMotion: "reduce",
    });
    const page = await ctx.newPage();
    const mutations = [];
    let failStatus = false;
    page.on("request", (r) => {
      if (r.method() === "POST" && r.url().includes("/api/"))
        mutations.push(r.url());
    });
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith("/process-context"))
        return route.fulfill({
          json: {
            cycleId: product,
            cycleNumber: 2,
            experimentId: product,
            chainDefinitionId: 92014,
            productVersion: "local-v1",
            status: "OPEN",
            stageLabel: "Validação",
            hypothesis: "Comprovar o resultado útil antes da oferta.",
            mainChange: "Controle automático com qualidade comprovada.",
            cycleUrl: `/business-process-chains/learning-cycles?productId=${product}&cycleId=${product}`,
            previousLearning: [],
            nextWork: null,
          },
        });
      if (url.pathname.includes("/automation/v1")) {
        if (
          failStatus &&
          route.request().method() === "GET" &&
          !url.pathname.endsWith("/events")
        )
          return route.fulfill({
            status: 503,
            json: { message: "Indisponibilidade simulada" },
          });
        return forwardLocal(route);
      }
      if (url.pathname.endsWith("/activity-executions")) {
        return forwardLocal(route);
      }
      if (url.pathname.endsWith("/execution-progress"))
        return route.fulfill({ json: [] });
      if (url.pathname.includes("value-chain-position"))
        return route.fulfill({
          json: {
            productId: product,
            chainDefinitionId: 92014,
            processDefinitionId: process,
            sequenceNumber: 4,
            processCount: 6,
            processMeasurements: [],
          },
        });
      if (url.pathname.includes("/users/me"))
        return route.fulfill({ json: { id: 1, name: "Teste local" } });
      if (url.pathname.includes("token") || url.pathname.includes("health"))
        return route.fulfill({ json: { status: "OK" } });
      return route.fulfill({ json: [] });
    });
    const url = `${base}/products/${product}/value-chain-history/processes/${process}/activities?learningCycleId=${product}&chainId=92014`;
    await page.goto(url, { waitUntil: "domcontentloaded" });
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(panel.getByRole("button", { name: "Executar processo" }))
      .toBeVisible({ timeout: 20000 })
      .catch(async (error) => {
        await page.screenshot({ path: `${output}/${name}-failure.png` });
        await writeFile(
          `${output}/${name}-failure.txt`,
          JSON.stringify({
            errors,
            body: await page.locator("body").innerText(),
          }),
        );
        throw error;
      });
    assert.equal(
      await page
        .getByRole("button", { name: "Executar atividade", exact: true })
        .count(),
      0,
    );
    await panel.getByRole("button", { name: "Executar processo" }).click();
    await expect(
      panel.getByRole("button", { name: "Pausar", exact: true }),
    ).toBeVisible();
    const result = await (
      await fetch(
        backend +
          `/api/business-processes/${process}/products/${product}/automation/v1?chainId=92014&learningCycleId=${product}&sourceReference=experiment:${product}`,
      )
    ).json();
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await expect(
      panel.getByRole("link", { name: /Atividade 1 — Atividade a/ }),
    ).toBeVisible({ timeout: 12000 });
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await page.screenshot({
      path: `${output}/${name}-running.png`,
      fullPage: false,
    });
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth + 1,
    );
    assert.equal(overflow, false, name);
    const rotation = await panel
      .locator(".product-process-situation__running-icon")
      .evaluate((e) => getComputedStyle(e).animationName);
    assert.equal(rotation, "none");
    await page.emulateMedia({ reducedMotion: "no-preference" });
    assert.notEqual(
      await panel
        .locator(".product-process-situation__running-icon")
        .evaluate((e) => getComputedStyle(e).animationName),
      "none",
    );
    await panel.getByRole("button", { name: "Histórico da execução" }).click();
    await expect(panel.getByText(/Atividade solicitada;/)).toBeVisible();
    await page.reload();
    await expect(
      panel.getByRole("button", { name: "Pausar", exact: true }),
    ).toBeVisible({ timeout: 15000 });
    assert.equal(
      mutations.filter((u) => u.endsWith("/automation/v1")).length,
      1,
    );
    await panel.getByRole("button", { name: "Pausar", exact: true }).click();
    await expect(
      panel.getByText("Concluindo a pausa", { exact: true }),
    ).toBeVisible();
    await post(`/fixture/${product}/${process}/a`, {
      status: "COMPLETED",
      achieved: true,
    });
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await expect(panel.getByText("Pausado", { exact: true })).toBeVisible({
      timeout: 12000,
    });
    await panel.getByRole("button", { name: "Retomar processo" }).click();
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await post(`/fixture/${product}/${process}/b`, {
      status: "BLOCKED",
      achieved: false,
      reason: "Falta prova de aplicação real.",
    });
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await expect(
      panel.getByText("Precisa de atenção", { exact: true }),
    ).toBeVisible({ timeout: 12000 });
    await expect(
      panel.getByText(/Falta prova de aplicação real/),
    ).toBeVisible();
    await page.screenshot({ path: `${output}/${name}-blocked.png` });
    failStatus = true;
    await expect(panel.getByRole("alert")).toBeVisible({ timeout: 20000 });
    await expect(
      panel.getByRole("button", { name: "Retomar processo" }),
    ).toBeDisabled();
    failStatus = false;
    await panel.getByRole("button", { name: "Atualizar", exact: true }).click();
    await expect(
      panel.getByRole("button", { name: "Retomar processo" }),
    ).toBeEnabled({ timeout: 12000 });
    await panel.getByRole("button", { name: "Retomar processo" }).click();
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await post(`/fixture/${product}/${process}/b`, {
      status: "COMPLETED",
      achieved: true,
    });
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${result.id}/reconcile`,
    );
    await expect(
      panel.getByText("Processo concluído", { exact: true }),
    ).toBeVisible({ timeout: 12000 });
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "100",
    );
    await page.screenshot({ path: `${output}/${name}-completed.png` });
    const parentRoot = `/api/business-processes/92004/products/${product}/automation/v1`;
    const parent = await post(parentRoot, {
      chainId: 92014,
      learningCycleId: product,
      sourceReference: `experiment:${product}`,
    });
    const waitingChild = await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${parent.id}/reconcile`,
    );
    await page.goto(
      `${base}/products/${product}/value-chain-history/processes/92004/activities?chainId=92014&learningCycleId=${product}`,
    );
    const childLink = panel.getByRole("link", {
      name: "Processo de teste 92005 · v1",
    });
    await expect(childLink).toBeVisible({ timeout: 15000 });
    await childLink.click();
    await expect(page).toHaveURL(
      new RegExp(
        `/92005/activities\\?chainId=92014&learningCycleId=${product}`,
      ),
    );
    const backLink = panel.getByRole("link", {
      name: /Voltar ao processo pai/,
    });
    await expect(backLink).toHaveAttribute(
      "href",
      `/products/${product}/value-chain-history/processes/92004/activities?chainId=92014&learningCycleId=${product}#activity-a`,
    );
    const reconcile = () =>
      post(
        `/api/internal/business-processes/automation/v1/stage-executions/${waitingChild.childRunId}/reconcile`,
      );
    await reconcile();
    await post(`/fixture/${product}/92005/a`, {
      status: "COMPLETED",
      achieved: true,
    });
    await reconcile();
    await post(`/fixture/${product}/92005/b`, {
      status: "COMPLETED",
      achieved: true,
    });
    await reconcile();
    await reconcile();
    await expect(
      panel.getByText("Processo concluído", { exact: true }),
    ).toBeVisible({ timeout: 12000 });
    await expect(backLink).toBeVisible();
    await panel.screenshot({ path: `${output}/${name}-child-return.png` });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    await backLink.click();
    await expect(page).toHaveURL(
      new RegExp(
        `/92004/activities\\?chainId=92014&learningCycleId=${product}#activity-a`,
      ),
    );
    await post(
      `/api/internal/business-processes/automation/v1/stage-executions/${parent.id}/reconcile`,
    );
    await expect(
      panel.getByText(/Subprocesso concluído e confirmado/),
    ).toBeVisible({ timeout: 12000 });
    assert.equal(
      mutations.some((u) => u.includes("/execution-requests")),
      false,
    );
    assert.equal(errors.length, 0, errors.join("\n"));
    // Aguarda as consultas interceptadas antes de descartar o contexto e suas respostas.
    await page.unrouteAll({ behavior: "wait" });
    await ctx.close();
    console.log(
      `PASS ${name}: início, contagem, pausa, retomada, falha, histórico, conclusão, subprocesso, retorno contextual e acessibilidade`,
    );
  }
} finally {
  await browser.close();
}
