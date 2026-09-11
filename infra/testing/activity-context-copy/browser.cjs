// Verifica a cópia real no build do frontend, com APIs locais e nenhum comando produtivo.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, writeFile, mkdir } = require("node:fs/promises");
const { createServer } = require("node:http");
const path = require("node:path");
const assert = require("node:assert/strict");

const activity = {
  activityDefinitionId: 900006,
  activityId: "prototypeCorrection",
  activityName: "Corrigir o protótipo a partir do parecer",
  activityObjective:
    "Corrigir a causa registrada e devolver a versão à homologação.",
  activityOwnerName: "Dédalo",
  sequenceNumber: 6,
  selectedVersionActivity: true,
  operationalState: "BLOCKED",
  stateReason: "Simulação local: versão executável pendente.",
  objectiveAchieved: false,
  stateEvidence: "DIRECT",
  taskCount: 0,
  tasks: [],
  executionRequestAvailable: false,
  executionRequestReason: "Dados sintéticos; não executar tarefa.",
};
const history = {
  productId: 4,
  productName: "Nome comercial diferente do nome interno",
  productInternalName: "Vega",
  selectedProcessDefinitionId: 70,
  processCode: "pde-prototype-validation",
  processName: "Protótipo, validação multiagente e aprovação do PDE",
  selectedProcessVersionNumber: 8,
  selectedProcessStatus: "PUBLISHED",
  operationalState: "BLOCKED",
  objectiveAchieved: false,
  selectedActivityCount: 2,
  completedActivityCount: 0,
  remainingActivityCount: 2,
  blockedActivityCount: 1,
  activityCount: 3,
  activitiesWithTasksCount: 0,
  uniqueTaskCount: 0,
  knownEstimatedCostUsd: 0,
  costCoverage: "NO_EXECUTIONS",
  activities: [
    activity,
    {
      ...activity,
      activityId: "humanApproval",
      activityName: "Aprovar protótipo privado",
      activityOwnerName: "Responsável pelo produto",
      sequenceNumber: 11,
      executionControl: {
        executorType: "HUMAN",
        interactionType: "STATUS",
        description: "Decisão humana.",
        actionAvailable: false,
        availabilityReason: "Simulação local.",
        confirmationRequired: false,
        requirements: [],
      },
    },
    {
      ...activity,
      activityId: "historical",
      activityName: "Validação anterior",
      activityOwnerName: "Psique",
      sequenceNumber: 1,
      selectedVersionActivity: false,
      operationalState: "HISTORICAL",
    },
  ],
};

async function paste(page) {
  await page.evaluate(() => {
    const field = document.createElement("textarea");
    field.id = "test-clipboard-paste";
    field.style.cssText = "position:fixed;top:0;left:0;z-index:99999";
    document.body.appendChild(field);
    field.focus({ preventScroll: true });
  });
  await page.keyboard.press("Control+V");
  const field = page.locator("#test-clipboard-paste");
  await expect(field).not.toHaveValue("");
  const value = await field.inputValue();
  await field.evaluate((element) => element.remove());
  return value;
}

(async () => {
  const output = path.resolve(
    process.env.ACTIVITY_COPY_OUTPUT ||
      "artifacts/activity-context-copy/browser",
  );
  const uiDir = path.resolve("frontend/dist");
  const cycle = JSON.parse(
    await readFile(
      "infra/testing/vega-harness-readiness/fixtures/cycle-context.json",
      "utf8",
    ),
  );
  const position = JSON.parse(
    await readFile(
      "infra/testing/vega-harness-readiness/fixtures/position.json",
      "utf8",
    ),
  );
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, "http://localhost").pathname;
      const file = name.startsWith("/assets/")
        ? path.join(uiDir, name)
        : path.join(uiDir, "index.html");
      assert(file.startsWith(uiDir + path.sep));
      res.setHeader(
        "Content-Type",
        file.endsWith(".js")
          ? "application/javascript"
          : file.endsWith(".css")
            ? "text/css"
            : "text/html",
      );
      res.end(await readFile(file));
    } catch {
      res.statusCode = 404;
      res.end();
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const port = server.address().port;
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    headless: true,
    args: [
      "--no-sandbox",
      "--no-proxy-server",
      "--host-resolver-rules=MAP activity-copy.test 127.0.0.1",
    ],
  });
  const results = [];
  try {
    for (const [profileName, profile] of [
      ["desktop", { viewport: { width: 1366, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      for (const secure of [false, true]) {
        const name = `${profileName}-${secure ? "secure" : "http"}`;
        const origin = `http://${secure ? "127.0.0.1" : "activity-copy.test"}:${port}`;
        const context = await browser.newContext(profile);
        if (secure)
          await context.grantPermissions(
            ["clipboard-read", "clipboard-write"],
            { origin },
          );
        const page = await context.newPage();
        const errors = [],
          unexpected = [];
        let mode = "cycle",
          mutations = 0,
          releasePosition;
        page.on("pageerror", (error) => errors.push(error.message));
        await page.route("**/*", async (route) => {
          const request = route.request(),
            url = new URL(request.url());
          if (url.pathname.startsWith("/api/")) {
            if (request.method() !== "GET") {
              mutations++;
              return route.abort();
            }
            if (url.pathname.endsWith("/activity-executions")) {
              const response = structuredClone(history);
              if (mode === "cycle" || mode === "loading") {
                assert.equal(url.searchParams.get("learningCycleId"), "2");
                assert.equal(url.searchParams.get("chainId"), "14");
              } else {
                assert.equal(url.searchParams.has("learningCycleId"), false);
                response.productId = 10;
                response.productInternalName =
                  mode === "missing" ? null : "Mira";
                response.activities[0].activityOwnerName =
                  mode === "missing" ? null : "Backend principal";
                if (mode !== "missing")
                  response.activities[0].executionControl = {
                    executorType: "BACKEND",
                    interactionType: "STATUS",
                    description: "Consolidação automática.",
                    actionAvailable: false,
                    availabilityReason: "Simulação local.",
                    confirmationRequired: false,
                    requirements: [],
                  };
              }
              return route.fulfill({ json: response });
            }
            if (url.pathname.endsWith("/process-context"))
              return route.fulfill({
                json: mode === "cycle" || mode === "loading" ? cycle : null,
              });
            if (url.pathname.includes("/value-chain-positions/")) {
              if (mode === "loading")
                await new Promise((resolve) => {
                  releasePosition = resolve;
                });
              const response = structuredClone(position);
              if (mode === "missing") response.processMeasurements = [];
              if (mode === "no-cycle")
                response.processMeasurements[0].sequenceLabel = "6.1";
              return route.fulfill({ json: response });
            }
            if (
              [
                "/api/creatives/video-review",
                "/api/ops-monitor/v1/modules/availability",
                "/api/facebook/configuration-status",
              ].includes(url.pathname)
            )
              return route.fulfill({ json: [] });
            unexpected.push(url.pathname);
            return route.abort();
          }
          if (url.origin !== origin) {
            unexpected.push(url.origin);
            return route.abort();
          }
          return route.continue();
        });
        const initial = `${origin}/products/4/value-chain-history/processes/70/activities?chainId=14#activity-humanApproval`;
        await page.goto(initial, { waitUntil: "networkidle" });
        assert.equal(await page.evaluate(() => window.isSecureContext), secure);
        if (!secure)
          assert.equal(
            await page.evaluate(() => typeof navigator.clipboard),
            "undefined",
          );
        const card = page.locator("#activity-prototypeCorrection");
        const button = card.getByRole("button", {
          name: "Copiar contexto da atividade 3.6",
          exact: true,
        });
        await expect(
          page.getByRole("button", { name: /Copiar contexto da atividade/ }),
        ).toHaveCount(3);
        await expect(button).toBeEnabled();
        await button.scrollIntoViewIfNeeded();
        if (profileName === "desktop") {
          const heading = await card.locator("h2").boundingBox(),
            bounds = await button.boundingBox();
          assert(
            bounds.x >= heading.x + heading.width &&
              Math.abs(bounds.y - heading.y) < 25,
            "Ícone ao lado do título.",
          );
        } else {
          const bounds = await button.boundingBox();
          assert(
            bounds.width >= 44 && bounds.height >= 44,
            "Alvo de toque de ao menos 44 pixels.",
          );
        }
        await button.focus();
        const scroll = await page.evaluate(() => window.scrollY);
        await button.press("Enter");
        await expect(card.getByRole("status")).toHaveText("Contexto copiado!");
        await expect(button).toBeFocused();
        assert(
          Math.abs((await page.evaluate(() => window.scrollY)) - scroll) < 2,
          "Copiar não desloca a leitura.",
        );
        assert.equal(page.url(), initial, "Copiar não navega.");
        const copied = await paste(page);
        assert(
          copied.includes(
            "Processo: 3 — Protótipo, validação multiagente e aprovação do PDE\n",
          ),
        );
        assert(
          copied.includes(
            "Atividade: 3.6 — Corrigir o protótipo a partir do parecer\n",
          ),
        );
        assert(
          copied.includes(
            "Produto (nome interno): Vega (ID: 4)\nAgente (nome interno): Dédalo\n",
          ),
        );
        assert(copied.includes("Ciclo: 2º ciclo (ID: 2)\nExperimento: #92\n"));
        assert(copied.includes(`Versão do produto: ${cycle.productVersion}\n`));
        assert(copied.includes("Processo selecionado: ID 70 · versão 8"));
        const link = copied.split("Link da atividade: ")[1];
        assert.equal(
          link,
          `${origin}/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-prototypeCorrection`,
        );
        assert(!copied.includes(history.productName));
        await writeFile(path.join(output, `${name}-clipboard.txt`), copied);
        await card.screenshot({ path: path.join(output, `${name}-card.png`) });
        const noOverflow = await card.evaluate(
          (element) => element.scrollWidth <= element.clientWidth + 1,
        );
        assert(noOverflow, "Card sem overflow horizontal.");

        await page.goto(link, { waitUntil: "networkidle" });
        await expect(button).toBeInViewport();
        const human = page.locator("#activity-humanApproval");
        await human.getByRole("button", { name: /Copiar contexto/ }).click();
        await expect(human.getByRole("status")).toHaveText("Contexto copiado!");
        const humanText = await paste(page);
        assert(
          humanText.includes(
            "Agente (nome interno): Não se aplica (atividade humana)\nResponsável: Responsável pelo produto",
          ),
        );
        assert(humanText.endsWith("#activity-humanApproval"));
        const historical = page.locator("#activity-historical");
        await historical
          .getByRole("button", { name: /Copiar contexto/ })
          .click();
        await expect(historical.getByRole("status")).toHaveText(
          "Contexto copiado!",
        );
        assert(
          (await paste(page)).includes(
            "Registro da atividade: histórico (fora da versão selecionada)",
          ),
        );

        if (secure) {
          await page.evaluate(() => {
            window.savedWriteText = navigator.clipboard.writeText.bind(
              navigator.clipboard,
            );
            navigator.clipboard.writeText = () =>
              Promise.reject(
                new DOMException(
                  "Simulação de permissão negada",
                  "NotAllowedError",
                ),
              );
          });
          await button.click();
          await expect(card.getByRole("status")).toHaveText(
            "Contexto copiado!",
          );
          assert.equal(
            await paste(page),
            copied,
            "Permissão moderna negada usa cópia real alternativa.",
          );
        }
        await page.evaluate(() => {
          window.savedExecCommand = document.execCommand.bind(document);
          document.execCommand = () => false;
        });
        await button.click();
        await expect(card.getByRole("alert")).toContainText(
          "Não foi possível copiar automaticamente",
        );
        await expect(card.getByRole("status")).toHaveCount(0);
        const manual = card.getByRole("textbox", {
          name: /para copiar manualmente/,
        });
        await expect(manual).toHaveValue(copied);
        await manual.focus();
        assert.equal(
          await manual.evaluate(
            (element) => element.selectionEnd - element.selectionStart,
          ),
          copied.length,
        );
        await card.screenshot({
          path: path.join(output, `${name}-manual.png`),
        });
        await page.evaluate(() => {
          document.execCommand = window.savedExecCommand;
          if (window.savedWriteText)
            navigator.clipboard.writeText = window.savedWriteText;
        });
        await button.click();
        await expect(card.getByRole("status")).toHaveText("Contexto copiado!");
        await expect(manual).toHaveCount(0);
        assert.equal(await paste(page), copied);

        mode = "no-cycle";
        await page.goto(
          `${origin}/products/10/value-chain-history/processes/70/activities?chainId=14#activity-prototypeCorrection`,
          { waitUntil: "networkidle" },
        );
        await card
          .getByRole("button", {
            name: "Copiar contexto da atividade 6.1.6",
            exact: true,
          })
          .click();
        await expect(card.getByRole("status")).toHaveText("Contexto copiado!");
        const other = await paste(page);
        assert(other.includes("Processo: 6.1 —"));
        assert(other.includes("Produto (nome interno): Mira (ID: 10)"));
        assert(
          other.includes(
            "Agente (nome interno): Não se aplica (execução pelo backend)",
          ),
        );
        assert(
          !other.includes("Vega") &&
            !other.includes("Ciclo:") &&
            !other.includes("learningCycleId=") &&
            !other.includes("Experimento:"),
        );

        mode = "missing";
        await page.reload({ waitUntil: "networkidle" });
        await card
          .getByRole("button", {
            name: "Copiar contexto da atividade 6",
            exact: true,
          })
          .click();
        await expect(card.getByRole("status")).toHaveText("Contexto copiado!");
        const missing = await paste(page);
        assert(missing.includes("Processo: Número não informado —"));
        assert(
          missing.includes("Produto (nome interno): Não informado (ID: 10)"),
        );
        assert(missing.includes("Agente (nome interno): Não informado"));
        assert(!missing.includes(history.productName));

        mode = "loading";
        await page.goto(initial, { waitUntil: "domcontentloaded" });
        await expect(
          card.getByRole("button", { name: /Copiar contexto/ }),
        ).toBeDisabled();
        await expect.poll(() => typeof releasePosition).toBe("function");
        releasePosition();
        await expect(button).toBeEnabled();
        assert.equal(mutations, 0, "Copiar não cria tarefas ou grava dados.");
        assert.deepEqual(errors, [], "Sem falhas JavaScript.");
        assert.deepEqual(
          unexpected,
          [],
          "Somente contratos previstos e dependências locais.",
        );
        results.push({
          name,
          status: "PASS",
          realClipboard: true,
          mutations,
          scenarios: [
            "cycle",
            "own-card-link",
            "human",
            "historical",
            "http-or-secure",
            "failure-and-retry",
            "no-cycle-other-product",
            "missing-fields",
            "loading",
            "keyboard-and-layout",
          ],
        });
        await context.close();
        console.log(`PASS ${name}`);
      }
    }
    await writeFile(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
