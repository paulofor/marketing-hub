/** Complementa a prova de encerramento sem repetir a matriz funcional já aceita. */
const { chromium, devices, expect } = require("@playwright/test");
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const base = "http://127.0.0.1:57188";
const api = "/api/pde/vega/consultor/v1";
const output = process.env.MUSA_SUPPLEMENT_OUTPUT;
if (!output) throw new Error("Informe o diretório das provas locais.");
fs.mkdirSync(output, { recursive: true });
const profiles = [
  ["DESKTOP_1440", { viewport: { width: 1440, height: 1000 } }],
  ["IPHONE_15_PRO", devices["iPhone 15 Pro"]],
  ["PIXEL_7", devices["Pixel 7"]],
];
const states = ["NOT_STARTED", "COMPLETED", "BLOCKED", "RECOVERABLE_TRANSPORT_FAILURE"];
const results = [];
const cycleBase = Number(process.env.MUSA_SUPPLEMENT_CYCLE_BASE || 98300);
if (!Number.isInteger(cycleBase) || cycleBase < 98000 || cycleBase > 99900) throw new Error("Use apenas identidades sintéticas reservadas.");
let browser;
/** Captura somente agregados funcionais sintéticos, sem chave ou cookie. */
function summary(report) {
  const data = report.execution;
  return {
    sessionId: report.sessionId,
    cycleId: report.cycleId,
    productId: report.productId,
    experimentId: report.experimentId,
    prototypeVersion: report.prototypeVersion,
    revision: data.revision,
    draft: data.draft,
    operations: Object.values(data.operations).map((op) => ({
      operationId: op.operationId,
      status: op.status,
      action: op.action,
      reason: op.reason,
      answers: op.answers,
    })),
    choices: Object.keys(data.choices),
    providerCalls: report.providerCalls,
    trafficClass: report.trafficClass,
    internalMarker: report.internalMarker,
  };
}
/** Recebe a resposta real do gateway preservando status e mensagem literal. */
async function call(page, url, body) {
  return page.evaluate(async ({ url, body }) => {
    const r = await fetch(url, {
      method: body === undefined ? "GET" : "POST",
      headers: { "Content-Type": "application/json", "X-PDE-Internal-Token": "vega-local-internal-only" },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    return { status: r.status, body: await r.json().catch(() => ({})) };
  }, { url, body });
}
/** Executa um percurso curto até o estado exigido e fecha o acesso pelo contrato oficial. */
(async () => {
  browser = await chromium.launch({ executablePath: "/usr/bin/chromium" });
  let sequence = 0;
  for (const [device, options] of profiles) for (const state of states) {
    const cycleId = cycleBase + sequence++;
    const name = `${device}-${state}`;
    if (process.env.MUSA_SUPPLEMENT_CASES && !process.env.MUSA_SUPPLEMENT_CASES.split(",").includes(name)) continue;
    const context = await browser.newContext(options);
    const page = await context.newPage();
    try {
      await page.route("**/internal/cycles/*/access", (route) => route.continue({
        headers: { ...route.request().headers(), "X-PDE-Internal-Token": "vega-local-internal-only" },
      }));
      await page.goto(`${base}/experiences/musa-consultor-pwa-v1/agent-validation`);
      await page.getByText("Operação da homologação").click();
      await page.getByLabel("Ciclo", { exact: true }).fill(String(cycleId));
      await page.getByRole("button", { name: "Emitir acesso privado" }).click();
      await page.getByRole("link", { name: "Abrir acesso emitido" }).click();
      await page.getByRole("checkbox").check();
      await page.getByRole("button", { name: "Começar minha orientação" }).click();
      await expect(page.getByText("Passo 1 de 3")).toBeVisible();
      if (state !== "NOT_STARTED") {
        await page.getByLabel("Para qual ocasião").fill("almoço");
        await page.getByRole("button", { name: "Próximo", exact: true }).click();
        await page.getByLabel("Quais peças").fill(state === "BLOCKED" ? "camisa e calça" : "Camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça.");
        await page.getByRole("button", { name: "Próximo", exact: true }).click();
        await page.getByLabel("O que você prefere").selectOption("Cor e criatividade");
        if (state === "RECOVERABLE_TRANSPORT_FAILURE") {
          await page.route("**/commands", async (route) => {
            if (route.request().postDataJSON()?.action === "generate") await route.abort("failed");
            else await route.continue();
          });
        }
        await page.getByRole("button", { name: "Receber orientação", exact: true }).click();
        if (state === "COMPLETED") {
          await expect(page.getByRole("article", { name: "Resultado da orientação" })).toBeVisible();
          await page.getByRole("button", { name: "Salvar minha escolha", exact: true }).click();
          await expect(page.getByRole("button", { name: "Escolha salva", exact: true })).toBeVisible();
        } else if (state === "BLOCKED") await expect(page.getByText("Vamos ajustar a entrada")).toBeVisible();
        else await expect(page.getByRole("alert")).not.toBeEmpty();
      }
      const before = await call(page, api + "/internal/report");
      expect(before.status).toBe(200);
      const observed = summary(before.body);
      const diagnosticPanelVisible = await page.getByRole("article", { name: "Resultado da orientação" }).count();
      const resultVisible = await page.getByRole("button", { name: /^(Salvar minha escolha|Escolha salva)$/ }).count();
      expect(resultVisible).toBe(state === "COMPLETED" ? 1 : 0);
      expect(observed.choices.length).toBe(state === "COMPLETED" ? 1 : 0);
      expect(observed.operations.length).toBe(["COMPLETED", "BLOCKED"].includes(state) ? 1 : 0);
      if (state === "BLOCKED") expect(observed.operations[0].status).toBe("BLOCKED");
      const beforeMessage = await page.getByRole("alert").allTextContents();
      await page.screenshot({ path: path.join(output, name + "-before.png"), fullPage: true });
      const close = await call(page, `${api}/internal/cycles/${cycleId}/close`, {});
      expect(close.status).toBe(200);
      expect(close.body.preserved).toBe(true);
      const afterClose = await call(page, api + "/workspace");
      expect(afterClose.status).toBe(401);
      await page.reload();
      await expect(page.getByRole("button", { name: "Entrar no meu espaço", exact: true })).toBeVisible();
      await expect(page.getByRole("article", { name: "Resultado da orientação" })).toHaveCount(0);
      await expect(page.getByRole("alert")).toContainText("preservados");
      const afterMessage = await page.getByRole("alert").allTextContents();
      expect(afterMessage.join(" ")).toContain("preservados");
      await page.screenshot({ path: path.join(output, name + "-after.png"), fullPage: true });
      const result = {
        device, state, status: "PASS", persistedBeforeClose: observed,
        resultVisibleBeforeClose: resultVisible === 1, diagnosticPanelVisible: diagnosticPanelVisible === 1,
        literalMessageBeforeClose: beforeMessage,
        close: close.body, deniedAfterClose: afterClose,
        afterReload: { resultVisible: false, loginVisible: true, literalMessage: afterMessage },
        screenshots: [name + "-before.png", name + "-after.png"],
      };
      fs.writeFileSync(path.join(output, name + ".json"), JSON.stringify(result, null, 2) + "\n");
      results.push({ ...result, resultSha256: crypto.createHash("sha256").update(JSON.stringify(result)).digest("hex") });
      console.log("PASS", name);
    } finally { await context.close(); }
  }
  const assembled = profiles.flatMap(([device]) => states.map((state) => {
    const file = path.join(output, `${device}-${state}.json`);
    if (!fs.existsSync(file)) return null;
    return { ...JSON.parse(fs.readFileSync(file, "utf8")), evidencePersistedAt: fs.statSync(file).mtime.toISOString() };
  })).filter(Boolean);
  fs.writeFileSync(path.join(output, "terminal-result.json"), JSON.stringify({
    contractVersion: "MUSA_CONTINUOUS_TERMINAL_EVIDENCE_V1", generatedAt: new Date().toISOString(),
    origin: "LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES", browser: "Chromium; iPhone/Pixel emulados",
    providerCalls: 0, commercialSideEffects: false, results: assembled,
  }, null, 2) + "\n");
})().catch((error) => { console.error(error); process.exitCode = 1; }).finally(async () => { await browser?.close(); });
