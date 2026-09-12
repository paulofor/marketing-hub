// Homologa o card no build real, com contratos sintéticos e bloqueio de conexões externas.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, writeFile, mkdir } = require("node:fs/promises");
const { createServer } = require("node:http");
const path = require("node:path");
const assert = require("node:assert/strict");
const fixture = require("./fixture.json");

async function paste(page) {
  await page.evaluate(() => {
    const field = document.createElement("textarea");
    field.id = "clipboard-paste-test";
    field.style.cssText = "position:fixed;top:0;left:0;z-index:99999";
    document.body.appendChild(field);
    field.focus({ preventScroll: true });
  });
  await page.keyboard.press("Control+V");
  const field = page.locator("#clipboard-paste-test");
  await expect(field).not.toHaveValue("");
  const text = await field.inputValue();
  await field.evaluate((element) => element.remove());
  return text;
}

(async () => {
  const output = path.resolve(
    process.env.PROCESS_COPY_OUTPUT || "artifacts/process-context-copy/browser",
  );
  const uiDir = path.resolve("frontend/dist");
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const pathname = new URL(req.url, "http://localhost").pathname;
      const file = pathname.startsWith("/assets/")
        ? path.join(uiDir, pathname)
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
      "--host-resolver-rules=MAP process-copy.test 127.0.0.1",
    ],
  });
  const results = [];
  let activePage;
  try {
    for (const [profileName, profile] of [
      ["desktop", { viewport: { width: 1366, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      for (const secure of [false, true]) {
        const name = `${profileName}-${secure ? "secure" : "http"}`;
        const origin = `http://${secure ? "127.0.0.1" : "process-copy.test"}:${port}`;
        const context = await browser.newContext(profile);
        if (secure)
          await context.grantPermissions(
            ["clipboard-read", "clipboard-write"],
            { origin },
          );
        const page = await context.newPage();
        activePage = page;
        const errors = [],
          unexpected = [];
        let mode = "running",
          mutations = 0,
          releaseAutomation;
        page.on("pageerror", (error) => {
          errors.push(error.message);
          console.error("PAGE ERROR", error.message);
        });
        await page.route("**/*", async (route) => {
          const request = route.request(),
            url = new URL(request.url());
          if (url.pathname.startsWith("/api/")) {
            if (request.method() !== "GET") {
              mutations++;
              return route.abort();
            }
            if (url.pathname.endsWith("/process-context"))
              return route.fulfill({
                json: ["no-cycle", "missing"].includes(mode)
                  ? null
                  : fixture.cycle,
              });
            if (url.pathname.includes("/value-chain-positions/")) {
              const position = structuredClone(fixture.position);
              if (["no-cycle", "missing"].includes(mode))
                position.productId = 92010;
              if (mode === "missing") {
                position.processMeasurements = [];
                position.processDefinitionId = null;
                position.sequenceNumber = null;
              }
              return route.fulfill({ json: position });
            }
            if (url.pathname.endsWith("/activity-executions")) {
              const h = structuredClone(fixture.history);
              if (["no-cycle", "missing"].includes(mode)) {
                assert.equal(url.searchParams.has("learningCycleId"), false);
                h.productId = 92010;
                h.productInternalName = mode === "missing" ? null : "Mira";
                h.currentExecutionReference = null;
                h.activities = [];
                h.activityCount = 0;
                h.uniqueTaskCount = 0;
              } else {
                assert.equal(url.searchParams.get("learningCycleId"), "92002");
                assert.equal(url.searchParams.get("chainId"), "92014");
              }
              return route.fulfill({ json: h });
            }
            if (url.pathname.endsWith("/execution-progress"))
              return route.fulfill({ json: [] });
            if (url.pathname.endsWith("/automation/v1")) {
              if (!url.searchParams.get("sourceReference")) {
                assert.equal(url.searchParams.get("chainId"), "92014");
                return route.fulfill({
                  json: {
                    ...fixture.automation,
                    id: null,
                    productId: ["no-cycle", "missing"].includes(mode)
                      ? 92010
                      : 92004,
                    sourceReference: null,
                    learningCycleId: url.searchParams.has("learningCycleId")
                      ? Number(url.searchParams.get("learningCycleId"))
                      : null,
                    childRunId: null,
                    navigationUrl: null,
                    status: "UNAVAILABLE",
                    canStart: false,
                    canResume: false,
                    canPause: false,
                    currentActivityId: null,
                    currentActivityName: null,
                    currentOwnerName: null,
                    reason: "Aguardando o contexto oficial de execução.",
                    parentProcesses: [],
                    subprocesses: [],
                  },
                });
              }
              assert.equal(
                url.searchParams.get("sourceReference"),
                "experiment:92092",
              );
              assert.equal(url.searchParams.get("learningCycleId"), "92002");
              assert.equal(url.searchParams.get("chainId"), "92014");
              if (mode === "loading")
                await new Promise((resolve) => {
                  releaseAutomation = resolve;
                });
              if (mode === "error")
                return route.fulfill({
                  status: 503,
                  json: { message: "Falha sintética de consulta." },
                });
              const run = structuredClone(fixture.automation);
              if (mode === "blocked") {
                run.status = "BLOCKED";
                run.reason = "Contrato não comprovou o objetivo.";
                run.canResume = true;
              }
              if (mode === "paused") {
                run.status = "PAUSED";
                run.reason = "Pausa solicitada.";
                run.canResume = true;
              }
              if (mode === "completed") {
                run.status = "COMPLETED";
                run.currentActivityId = null;
                run.currentActivityName = null;
                run.completedActivities = 3;
                run.remainingActivities = 0;
                run.completionPercentage = 100;
                run.reason = "Objetivos comprovados.";
              }
              return route.fulfill({ json: run });
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
            return route.fulfill({ status: 501, json: {} });
          }
          if (url.origin !== origin) {
            unexpected.push(url.origin);
            return route.abort();
          }
          return route.continue();
        });
        const initial = `${origin}/products/92004/value-chain-history/processes/92063/activities?chainId=92014&ignored=NAO_COPIAR#activity-commercialReview`;
        const panel = page.getByRole("region", {
          name: "Execução automática do processo",
        });
        const button = panel.getByRole("button", {
          name: "Copiar contexto do processo",
          exact: true,
        });
        await page.goto(initial, { waitUntil: "networkidle" });
        assert.equal(await page.evaluate(() => window.isSecureContext), secure);
        await expect(button).toBeEnabled();
        await button.scrollIntoViewIfNeeded();
        await button.focus();
        const scroll = await page.evaluate(() => window.scrollY);
        await button.press("Enter");
        await expect(panel.getByRole("status")).toHaveText("Contexto copiado!");
        await expect(button).toBeFocused();
        assert.equal(page.url(), initial, "Copiar não navega.");
        assert(
          Math.abs((await page.evaluate(() => window.scrollY)) - scroll) < 2,
          "Foco não desloca a página.",
        );
        const copied = await paste(page);

        for (const part of [
          "Produto (nome interno): Vega (ID: 92004)",
          "Processo: 4 —",
          "Versão do processo: v7 · definição ID 92063",
          "Ciclo: 2º ciclo (ID: 92002)",
          "Experimento: #92092",
          "Execução: #920001",
          "0 concluídas · 3 restantes · 1 dispensadas",
          "Atividade atual: 4.1",
          "Tarefa #920401",
          "Tarefa #920301",
          "Versão da atividade: v6 · definição histórica não informada",
          "Tipo de executor: HUMAN",
          "Tipo de executor: BACKEND",
          "Aprendizado: Não atribuir abandono sem evidência.",
          "Link da correção:",
        ])
          assert(copied.includes(part), part);
        assert(
          !/PROMPT_BRUTO|PAYLOAD_NAO|TOKEN_NAO|ignored=|undefined|null/.test(
            copied,
          ),
        );
        assert.equal(
          (copied.match(/^Atividade: /gm) || []).length,
          5,
          "Todas as atividades, inclusive histórico.",
        );
        const processUrl = copied.match(/^Link do processo: (.+)$/m)[1];
        assert(
          !new URL(processUrl).hash,
          "Link do processo sem âncora de outra atividade.",
        );
        assert.equal(
          new URL(processUrl).searchParams.get("learningCycleId"),
          "92002",
        );
        const activityUrl = copied.match(
          /^Link da atividade: (.+#activity-commercialReview)$/m,
        )[1];
        assert.equal(new URL(activityUrl).searchParams.get("chainId"), "92014");
        const bounds = await button.boundingBox(),
          cardBounds = await panel.boundingBox();
        assert(
          bounds.height >= 44 &&
            bounds.x >= cardBounds.x &&
            bounds.x + bounds.width <= cardBounds.x + cardBounds.width + 1,
          "Botão acessível e dentro do card.",
        );
        await panel.screenshot({ path: path.join(output, `${name}-card.png`) });
        await panel.getByText("Ver contexto completo", { exact: true }).click();
        await expect(panel.locator("pre")).toBeVisible();
        assert.equal(
          copied,
          await panel.locator("pre").textContent(),
          "Cópia e prévia são iguais.",
        );
        assert(
          await panel
            .locator("pre")
            .evaluate((el) => el.scrollWidth <= el.clientWidth + 1),
          "Prévia sem rolagem horizontal.",
        );
        await panel.screenshot({
          path: path.join(output, `${name}-preview.png`),
        });
        await writeFile(path.join(output, `${name}-copied.txt`), copied);
        await page.goto(processUrl, { waitUntil: "networkidle" });
        await expect(button).toBeEnabled();
        await button.click();
        await expect(panel.getByRole("status")).toHaveText("Contexto copiado!");
        assert.equal(
          await paste(page),
          copied,
          "Link reabre o mesmo contexto.",
        );
        if (secure) {
          await page.evaluate(() => {
            window.savedWriteText = navigator.clipboard.writeText.bind(
              navigator.clipboard,
            );
            navigator.clipboard.writeText = () =>
              Promise.reject(
                new DOMException("Permissão negada", "NotAllowedError"),
              );
          });
          await button.click();
          await expect(panel.getByRole("status")).toHaveText(
            "Contexto copiado!",
          );
          assert.equal(
            await paste(page),
            copied,
            "Fallback real após bloqueio da API moderna.",
          );
        }
        await page.evaluate(() => {
          window.savedExecCommand = document.execCommand.bind(document);
          document.execCommand = () => false;
        });
        await button.click();
        await expect(panel.getByRole("alert")).toContainText(
          "Não foi possível copiar automaticamente",
        );
        const manual = panel.getByRole("textbox", {
          name: "Contexto do processo para copiar manualmente",
        });
        await expect(manual).toHaveValue(copied);
        await expect(panel.getByRole("status")).toHaveCount(0);
        await manual.focus();
        assert.equal(
          await manual.evaluate((el) => el.selectionEnd - el.selectionStart),
          copied.length,
        );
        await panel.screenshot({
          path: path.join(output, `${name}-manual.png`),
        });
        await page.evaluate(() => {
          document.execCommand = window.savedExecCommand;
          if (window.savedWriteText)
            navigator.clipboard.writeText = window.savedWriteText;
        });
        await button.click();
        await expect(panel.getByRole("status")).toHaveText("Contexto copiado!");
        await expect(manual).toHaveCount(0);
        assert.equal(await paste(page), copied);
        for (const state of ["blocked", "paused", "completed"]) {
          mode = state;
          await page.reload({ waitUntil: "networkidle" });
          await expect(button).toBeEnabled();
          await button.click();
          await expect(panel.getByRole("status")).toHaveText(
            "Contexto copiado!",
          );
          const text = await paste(page);
          assert(
            text.includes(
              state === "blocked"
                ? "Contrato não comprovou o objetivo."
                : state === "paused"
                  ? "Pausa solicitada."
                  : "3 concluídas · 0 restantes",
            ),
          );
        }
        for (const nextMode of ["no-cycle", "missing"]) {
          mode = nextMode;
          await page.goto(
            `${origin}/products/92010/value-chain-history/processes/92063/activities?chainId=92014`,
            { waitUntil: "networkidle" },
          );
          await expect(button).toBeEnabled();
          await button.click();
          await expect(panel.getByRole("status")).toHaveText(
            "Contexto copiado!",
          );
          const text = await paste(page);
          assert(
            text.includes(
              `Produto (nome interno): ${mode === "missing" ? "Não informado" : "Mira"} (ID: 92010)`,
            ),
          );
          assert(text.includes("Ciclo: Não informado pelo backend"));
          assert(
            !/Execução: #920001|learningCycleId=|2º ciclo|Tarefa #920401/.test(
              text,
            ),
            "Outro produto não recebe execução ou ciclo anteriores.",
          );
          if (mode === "missing")
            assert(text.includes("Processo: Número não informado"));
        }
        mode = "loading";
        await page.goto(initial, { waitUntil: "domcontentloaded" });
        await expect(button).toBeDisabled();
        await expect.poll(() => typeof releaseAutomation).toBe("function");
        releaseAutomation();
        await expect(button).toBeEnabled();
        mode = "error";
        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(panel.getByRole("alert")).toContainText(
          "Não foi possível atualizar a execução",
          { timeout: 15000 },
        );
        await expect(button).toBeEnabled();
        await button.click();
        await expect(panel.getByRole("status")).toHaveText("Contexto copiado!");
        assert(
          (await paste(page)).includes(
            "Atenção: Não foi possível consultar a execução automática.",
          ),
        );
        assert.equal(
          mutations,
          0,
          "A cópia nunca inicia processo, tarefa ou outra escrita.",
        );
        assert.deepEqual(errors, [], "Sem falha JavaScript.");
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
            "identity",
            "cycles-and-learning",
            "all-activities",
            "task-history",
            "official-progress",
            "safe-fields",
            "own-links",
            "keyboard-and-layout",
            "secure-or-http",
            "denied-permission",
            "manual-and-retry",
            "blocked-paused-completed",
            "other-product-no-cycle",
            "missing-fields",
            "loading",
            "query-failure",
          ],
        });
        await page.unrouteAll({ behavior: "wait" });
        await context.close();
        console.log(`PASS ${name}`);
      }
    }
    await writeFile(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
  } catch (error) {
    if (activePage && !activePage.isClosed()) {
      await activePage.screenshot({
        path: path.join(output, "failure.png"),
        fullPage: true,
      });
      await writeFile(
        path.join(output, "failure-page.txt"),
        await activePage.locator("body").innerText(),
      );
    }
    throw error;
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
