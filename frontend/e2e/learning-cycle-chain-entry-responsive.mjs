import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const { chromium, devices, expect } = createRequire(import.meta.url)(
  "@playwright/test",
);
const base = "http://127.0.0.1:15173";
const api = "http://127.0.0.1:18091";
const cycleApi = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/learning-cycle-entry";
await mkdir(output, { recursive: true });
const fixture = await (
  await fetch(api + "/api/business-process-chains/91001")
).json();
const parent = fixture.processes.find((p) => p.sequenceNumber === 6);
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
let checks = 0;
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext({
      ...profile,
      defaultBrowserType: undefined,
    });
    const page = await context.newPage();
    const errors = [],
      mutations = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.origin !== base) {
        errors.push("Requisição externa: " + url.origin);
        await route.abort();
        return;
      }
      if (request.method() !== "GET") {
        mutations.push(request.url());
        await route.abort();
        return;
      }
      if (/^\/api\/business-processes\/\d+\/products\//.test(url.pathname)) {
        await route.fulfill({ json: null });
        return;
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(cycleApi) &&
        !url.pathname.startsWith("/api/business-process-chains") &&
        !/^\/api\/business-processes(\/\d+(\/composition)?)?$/.test(
          url.pathname,
        ) &&
        url.pathname !== "/api/products"
      ) {
        await route.fulfill({ json: [] });
        return;
      }
      await route.continue();
    });
    await page.goto(
      base + "/business-process-chains?chainId=91001&productId=91001",
      { waitUntil: "networkidle" },
    );
    const processes = page.locator(".business-process-chain-processes > li");
    await expect(processes).toHaveCount(6);
    const sales = processes.nth(5);
    const entry = sales.getByRole("region", {
      name: "Ciclo dentro do processo de venda",
    });
    await expect(entry).toBeVisible();
    assert.equal(
      await page
        .getByRole("region", { name: "Ciclo dentro do processo de venda" })
        .count(),
      1,
    );
    await entry
      .locator("summary")
      .filter({ hasText: "Decisão comercial" })
      .click();
    for (const n of [2, 3, 4, 5, 6])
      await expect(
        entry.getByRole("link", { name: new RegExp("^" + n + "\\.") }),
      ).toBeVisible();
    await page.screenshot({
      path: `${output}/${name}-chain.png`,
      fullPage: true,
    });
    await entry
      .getByRole("link", { name: /Abrir ciclo|Retomar ciclo/ })
      .click();
    await expect(
      page.getByRole("navigation", { name: "Local do ciclo na cadeia" }),
    ).toBeVisible();
    assert.equal(new URL(page.url()).searchParams.get("productId"), "91001");
    await page.getByRole("link", { name: "Voltar à Cadeia de Valor" }).click();
    await page
      .locator(".business-process-chain-processes > li")
      .nth(5)
      .getByRole("link", { name: /Abrir atividades/ })
      .click();
    await expect(
      page.getByRole("region", { name: "Ciclo dentro do processo de venda" }),
    ).toBeVisible();
    await expect(
      page.getByRole("navigation", {
        name: "Decisões de Qual decisão comercial?",
      }),
    ).toBeVisible();
    await expect(
      page
        .getByRole("navigation", {
          name: "Decisões de Qual decisão comercial?",
        })
        .getByRole("link"),
    ).toHaveCount(5);
    const subprocess = page.getByRole("link", {
      name: "Abrir subprocesso Ciclos de aprendizado e vendas",
      exact: true,
    });
    await subprocess.click();
    await expect(
      page.getByRole("region", { name: "Ciclo dentro do processo de venda" }),
    ).toBeVisible();
    await page
      .getByRole("region", { name: "Ciclo dentro do processo de venda" })
      .getByRole("link", { name: /Abrir ciclo|Retomar ciclo/ })
      .click();
    assert.equal(new URL(page.url()).searchParams.get("chainId"), "91001");
    await page.goto(
      base +
        `/products/91001/value-chain-history/processes/${parent.processDefinitionId}/activities?chainId=91001`,
      { waitUntil: "networkidle" },
    );
    const productEntry = page.getByRole("region", {
      name: "Ciclo dentro do processo de venda",
    });
    await expect(productEntry).toBeVisible();
    await productEntry
      .getByRole("link", { name: /Abrir ciclo|Retomar ciclo/ })
      .click();
    assert.equal(new URL(page.url()).searchParams.get("productId"), "91001");
    await expect(
      page.getByRole("navigation", { name: "Local do ciclo na cadeia" }),
    ).toBeVisible();
    await page.screenshot({
      path: `${output}/${name}-cycle.png`,
      fullPage: true,
    });
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
      "Overflow mobile",
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    // A indisponibilidade do contrato deve ser mostrada sem oferecer um vínculo inferido.
    await page.route("**" + cycleApi + "/entry?**", (route) =>
      route.fulfill({ status: 503, json: { detail: "Fixture indisponível" } }),
    );
    await page.goto(
      base +
        `/products/91002/value-chain-history/processes/${parent.processDefinitionId}/activities?chainId=91001`,
      { waitUntil: "networkidle" },
    );
    await expect(
      page
        .getByRole("alert")
        .filter({ hasText: "Não foi possível consultar o vínculo" }),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      page.getByRole("region", { name: "Ciclo dentro do processo de venda" }),
    ).toHaveCount(0);
    checks += 6;
    await context.close();
  }
} finally {
  await browser.close();
}
console.log(
  JSON.stringify({ checks, profiles: 3, externalCalls: 0, mutations: 0 }),
);
