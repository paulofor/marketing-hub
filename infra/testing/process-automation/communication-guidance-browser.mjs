import assert from "node:assert/strict";
import { readFile, mkdir } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const action = JSON.parse(
  await readFile(process.env.COMMUNICATION_GUIDANCE_FIXTURE, "utf8"),
);
const responses = JSON.parse(
  await readFile(process.env.COMMUNICATION_PROCESS_FIXTURE, "utf8"),
);
const automation = responses.find((row) =>
  row.url.includes("/automation/v1"),
)?.body;
assert(automation && action.code === "DEFINE_CONTRIBUTION_TARGET");
assert(automation.completedActivities === 0);
const base = process.env.PROCESS_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["localhost", "127.0.0.1"].includes(new URL(base).hostname));
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output);
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
    await page.routeWebSocket("**/*", (socket) => socket.close());
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname.startsWith("/api/")) {
        if (!["GET", "OPTIONS"].includes(request.method())) {
          mutations.push(request.url());
          return route.abort();
        }
        if (url.pathname.includes("/automation/v1"))
          return route.fulfill({
            json: {
              ...automation,
              status: "WAITING_HUMAN",
              currentOwnerName: action.responsible,
              reason: action.reason,
              userAction: action,
            },
          });
        const response = responses.find(
          (row) => new URL(row.url).pathname === url.pathname,
        );
        return route.fulfill({ json: response?.body ?? [] });
      }
      if (url.origin !== base) return route.abort();
      return route.continue();
    });
    await page.goto(
      `${base}/products/${automation.productId}/value-chain-history/processes/${automation.processDefinitionId}/activities?chainId=${automation.chainId}&sourceReference=${encodeURIComponent(automation.sourceReference)}`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    const decision = panel.getByLabel("Próxima ação necessária");
    await expect(decision.getByText(action.title, { exact: true })).toBeVisible(
      { timeout: 20000 },
    );
    await expect(decision).toContainText("não é lucro líquido");
    await expect(decision).toContainText("Exemplo fictício");
    await expect(decision).toContainText("custos estiverem completos");
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(
      decision.getByRole("link", { name: action.actionLabel }),
    ).toHaveAttribute("href", action.actionUrl);
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await panel.screenshot({
      path: `${output}/${name}-communication-guidance.png`,
    });
    await decision.getByRole("link", { name: action.actionLabel }).click();
    await expect(page).toHaveURL(`${base}${action.actionUrl}`);
    const revisionId = new URL(action.actionUrl, base).searchParams.get(
      "revisionId",
    );
    const planId = new URL(action.actionUrl, base).searchParams.get(
      "commercialPlanId",
    );
    const financialRevision = responses
      .find(
        (row) =>
          new URL(row.url).pathname ===
          `/api/financial-plans/v1/products/${automation.productId}`,
      )
      ?.body.find((revision) => revision.id === Number(revisionId));
    assert.equal(financialRevision?.commercialPlanId, Number(planId));
    await expect(page.getByLabel("Histórico de revisões")).toHaveValue(
      revisionId,
    );
    await expect(
      page.getByText(
        `Plano comercial #${planId} · versão ${financialRevision.commercialPlanVersion}.`,
        { exact: true },
      ),
    ).toBeVisible();
    await expect(
      page.getByLabel("Margem mínima proposta (%)", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByLabel("Margem mínima proposta (%)", { exact: true }),
    ).toHaveValue("");
    await expect(
      page.getByRole("button", { name: "Salvar margem mínima", exact: true }),
    ).toBeVisible();
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await page.close();
    console.log(
      `PASS ${name}: decisão financeira da referência exata, zero objetivos fabricados e zero mutações`,
    );
  }
} finally {
  await browser.close();
}
