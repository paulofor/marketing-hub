// Exercita a publicação encerrada no frontend e backend locais, sem acesso a produção.
const { chromium, devices, expect } = require("@playwright/test");
const { execFileSync } = require("node:child_process");
const { mkdir, writeFile } = require("node:fs/promises");
const assert = require("node:assert/strict");

const base = "http://127.0.0.1:15173";
const api = "http://127.0.0.1:18091";
const prefix = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.PUBLICATION_RECOVERY_OUTPUT ||
  "artifacts/capella-activity541/browser";

async function json(path, body) {
  const response = await fetch(
    api + path,
    body === undefined
      ? {}
      : {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
  );
  return { status: response.status, value: await response.json() };
}

(async () => {
  await mkdir(output, { recursive: true });
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [device, profile] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const cycle = JSON.parse(
        execFileSync(
          "python3",
          [
            "-c",
            `
import sys,json
sys.path.insert(0,'infra/testing/cycle-commercial')
from client import prepare,authorize,command,http,read
c=prepare(missing=False)
c=command(c,body=authorize(c))
http('/fixture/experiments/91001/publish',{})
http('/fixture/experiments/91001/stop',{})
print(json.dumps(read(c)))
`,
          ],
          { encoding: "utf8" },
        ),
      );
      const path = `${prefix}/products/${cycle.productId}/${cycle.id}/commands`;
      const url = `/business-process-chains/learning-cycles?productId=${cycle.productId}&chainId=${cycle.chainDefinitionId}&cycleId=${cycle.id}`;
      const context = await browser.newContext({
        ...profile,
        timezoneId: "UTC",
      });
      const page = await context.newPage();
      const errors = [],
        writes = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await context.route("**/*", async (route) => {
        const request = route.request(),
          target = new URL(request.url());
        if (target.origin !== base) return route.abort();
        if (!target.pathname.startsWith("/api/")) return route.continue();
        if (request.method() !== "GET")
          writes.push({ path: target.pathname, body: request.postDataJSON() });
        if (target.pathname === "/api/products")
          return route.fulfill({
            json: [
              {
                id: cycle.productId,
                internalName: "Capella · homologação segregada",
                name: "Produto de teste",
                automaticExecutionEnabled: true,
                automaticExecutionStatus: "PLAY",
              },
            ],
          });
        if (target.pathname === "/api/products/value-chain-positions")
          return route.fulfill({
            json: [
              {
                productId: cycle.productId,
                resolutionStatus: "IDENTIFIED",
                resolutionMessage: "Cadeia publicada posterior",
                chainDefinitionId: 92027,
                processDefinitionId: 5,
                sequenceNumber: 5,
                processName: "Homologação comercial",
                learningCycleNavigation: {
                  productId: cycle.productId,
                  cycleId: cycle.id,
                  experimentId: cycle.experimentId,
                  chainDefinitionId: cycle.chainDefinitionId,
                  contextChainDefinitionId: 92027,
                  stage: cycle.stage,
                  status: cycle.status,
                  reason: cycle.nextAction,
                  url,
                },
              },
            ],
          });
        if (target.pathname === "/api/business-process-chains")
          return route.fulfill({
            json: [
              {
                id: cycle.chainDefinitionId,
                name: "Cadeia de teste",
                versionNumber: 1,
              },
            ],
          });
        if (target.pathname.startsWith(prefix)) {
          const response = await route.fetch({
            url: api + target.pathname + target.search,
          });
          return route.fulfill({ response });
        }
        return route.fulfill({ json: [] });
      });
      await page.goto(base + "/products");
      const link = page.getByRole("link", {
        name: "Abrir ciclo e decisões",
        exact: true,
      });
      await expect(link).toHaveAttribute("href", url);
      await link.click();
      const form = page.getByRole("form", {
        name: "Decisão do ciclo",
        exact: true,
      });
      await expect(form).toContainText("não reativa a campanha");
      await form.locator('[name="operatorName"]').fill("QA local segregado");
      await form
        .locator('[name="summary"]')
        .fill("Conferência de publicação sintética encerrada");
      await form
        .locator('[name="evidenceReference"]')
        .fill("internal://fixture/publication-stopped");
      await page.screenshot({
        path: `${output}/${device}-before.png`,
        fullPage: true,
      });
      await form
        .getByRole("button", {
          name: "Concluir etapa com evidência",
          exact: true,
        })
        .click();
      await expect
        .poll(
          async () =>
            (await json(`${prefix}/products/${cycle.productId}`)).value.find(
              (c) => c.id === cycle.id,
            ).stage,
        )
        .toBe("DECISION");
      const after = (
        await json(`${prefix}/products/${cycle.productId}`)
      ).value.find((c) => c.id === cycle.id);
      const beforeState = (
        await json(`/fixture/experiments/${cycle.experimentId}/state`)
      ).value;
      assert.equal(beforeState.status, "USER_STOPPED");
      assert.equal(beforeState.runCount, 1);
      assert.equal(writes.length, 1);
      assert.equal(writes[0].path, path);
      assert.equal(
        (await json(path, writes[0].body)).status,
        200,
        "Replay deve ser idempotente",
      );
      assert.equal(
        (
          await json(path, {
            ...writes[0].body,
            requestKey: crypto.randomUUID(),
          })
        ).status,
        409,
        "Revisão antiga deve ser recusada",
      );
      const replayed = (
        await json(`${prefix}/products/${cycle.productId}`)
      ).value.find((c) => c.id === cycle.id);
      assert.deepEqual(replayed.events, after.events);
      assert.equal(
        (await json(`/fixture/experiments/${cycle.experimentId}/state`)).value
          .status,
        "USER_STOPPED",
      );
      assert.deepEqual(errors, []);
      await page.screenshot({
        path: `${output}/${device}-after.png`,
        fullPage: true,
      });
      results.push({
        device,
        cycleId: cycle.id,
        stage: after.stage,
        experimentStatus: beforeState.status,
        newRuns: 0,
        externalCalls: 0,
        errors,
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }
  await writeFile(`${output}/results.json`, JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
