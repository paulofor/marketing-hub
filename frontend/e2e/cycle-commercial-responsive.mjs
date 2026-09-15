import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173";
const apiBase = "http://127.0.0.1:18091";
const api = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR ||
  "artifacts/cycle-commercial/browser";
await mkdir(output, { recursive: true });
async function post(path, body = {}) {
  const response = await fetch(apiBase + path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  assert.equal(response.status, 200, await response.text());
}
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1050 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const { cycle, run, processPath } = JSON.parse(
      execFileSync("python3", ["infra/testing/cycle-commercial/client.py"], {
        encoding: "utf8",
      }),
    );
    const context = await browser.newContext({
      ...profile,
      timezoneId: "America/Sao_Paulo",
    });
    const page = await context.newPage();
    const errors = [],
      external = [],
      commands = [];
    let rejectOnce = false;
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.origin !== base) {
        external.push(url.href);
        return route.abort();
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(api) &&
        !url.pathname.startsWith("/api/business-processes/") &&
        !url.pathname.startsWith("/api/products/value-chain-positions/") &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      if (
        request.method() === "POST" &&
        url.pathname.endsWith("/budget-authorization")
      ) {
        commands.push(request.postDataJSON());
        await new Promise((resolve) => setTimeout(resolve, 500));
        if (rejectOnce) {
          rejectOnce = false;
          return route.fulfill({
            status: 409,
            json: {
              detail:
                "Fonte comercial temporariamente indisponível (simulação local).",
            },
          });
        }
      }
      return route.continue();
    });
    const parentUrl = `${base}/products/91001/value-chain-history/processes/${run.processDefinitionId}/activities?chainId=91002&learningCycleId=${cycle.id}#process-execution`;
    await page.goto(parentUrl);
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Aguardando condições", { exact: true }),
    ).toBeVisible();
    await expect(
      panel.locator(".product-process-situation__running-icon"),
    ).toHaveCount(0);
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await panel.getByRole("link", { name: "Ver pendências do ciclo" }).click();
    await expect(page).toHaveURL(
      new RegExp(
        `chainId=91002&productId=91001&cycleId=${cycle.id}#cycle-decision`,
      ),
    );
    const form = page.getByRole("form", { name: "Decisão do ciclo" });
    await expect(form).toContainText("preparação");
    await expect(
      form.getByRole("link", { name: /experimento/i }),
    ).toHaveAttribute("href", "/experiments/91001");
    await expect(
      form.getByRole("button", { name: "Aprovar orçamento" }),
    ).toBeEnabled();
    assert.equal(commands.length, 0);
    await page.screenshot({
      path: `${output}/${name}-pending.png`,
      fullPage: true,
    });
    await post("/fixture/commercial-preparation/91001", []);
    await page.reload();
    await expect(
      form.getByRole("button", { name: "Aprovar orçamento" }),
    ).toBeEnabled();
    await expect(form.locator("input")).toHaveCount(2);
    await expect(form.locator('[name="budgetLimitBrl"]')).toHaveValue("100");
    await form.locator('[name="dailyBudgetBrl"]').fill("25");
    assert.equal(commands.length, 0, "Sugestão não autoriza orçamento");
    rejectOnce = true;
    await form.getByRole("button", { name: "Aprovar orçamento" }).click();
    await expect(
      form.getByRole("button", { name: "Aprovar orçamento" }),
    ).toBeDisabled();
    await expect(form.getByRole("alert")).toContainText(
      "temporariamente indisponível",
    );
    await expect(form.locator('[name="dailyBudgetBrl"]')).toHaveValue("25");
    await page.screenshot({
      path: `${output}/${name}-retry.png`,
      fullPage: true,
    });
    const response = page.waitForResponse(
      (r) =>
        r.request().method() === "POST" &&
        r.url().endsWith("/budget-authorization") &&
        r.status() === 200,
    );
    await form.getByRole("button", { name: "Aprovar orçamento" }).click();
    const updated = await (await response).json();
    assert.equal(updated.stage, "PUBLICATION");
    assert.equal(commands.length, 2);
    assert.equal(commands[0].requestKey, commands[1].requestKey);
    assert.equal(commands[1].dailyBudgetBrl, 25);
    assert.equal(commands[1].budgetLimitBrl, 100);
    assert.equal(updated.events.at(-1).evidence.confirmed, true);
    assert.equal(
      updated.events.at(-1).evidence.productVersion,
      cycle.productVersion,
    );
    await expect(form).toContainText("publicação");
    await page.screenshot({
      path: `${output}/${name}-publication.png`,
      fullPage: true,
    });
    await page.goto(base + updated.workUrl);
    await expect(
      page.getByText(
        "A confirmação autoriza a publicação da campanha na Meta e o gasto de mídia até o teto informado, dentro da janela aprovada. O experimento só entra em execução após a confirmação da plataforma.",
        { exact: true },
      ),
    ).toBeVisible();
    await expect(page.getByText(/sem criar campanha paga/)).toHaveCount(0);
    await page.screenshot({
      path: `${output}/${name}-activation-effect.png`,
      fullPage: true,
    });
    await page.goto(parentUrl);
    await expect(
      panel.getByRole("link", { name: "Continuar no ciclo" }),
    ).toHaveAttribute("href", new RegExp(`cycleId=${cycle.id}#cycle-decision`));
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    const size = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      viewport: innerWidth,
    }));
    assert(size.scroll <= size.viewport + 1, JSON.stringify(size));
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    results.push({
      profile: name,
      checks: 7,
      cycleId: cycle.id,
      processPath,
      externalCalls: 0,
      realSales: false,
    });
    await context.close();
  }
} finally {
  await browser.close();
}
await writeFile(
  `${output}/results.json`,
  JSON.stringify({ profiles: results, checks: results.length * 7 }, null, 2),
);
console.log(
  JSON.stringify({
    passed: results.length * 7,
    profiles: results,
    externalCalls: 0,
  }),
);
