import assert from "node:assert/strict";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");
const [baseUrl, fixturePath, outputPath] = process.argv.slice(2);
if (!baseUrl || !fixturePath || !outputPath || !/^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(baseUrl))
  throw new Error("Use URL local, fixture sanitizada e diretório de evidências.");
const fixture = JSON.parse(await readFile(fixturePath, "utf8"));
await mkdir(outputPath, { recursive: true });
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium", headless: true, args: ["--no-sandbox"] });
const results = [];
try {
  for (const productId of [4, 99004]) for (const device of ["desktop", "iPhone 15 Pro", "Pixel 7"]) {
    const context = await browser.newContext(device === "desktop" ? { viewport: { width: 1440, height: 1100 } } : devices[device]);
    const page = await context.newPage();
    const data = structuredClone(fixture);
    const cycleId = productId === 4 ? 22 : 99022;
    const experimentId = productId === 4 ? 115 : 99115;
    const taskId = productId === 4 ? 921 : 990921;
    data.history.productId = productId;
    data.history.currentExecutionReference = `experiment:${experimentId}`;
    data.cycle.cycleId = cycleId; data.cycle.experimentId = experimentId;
    data.position.productId = productId;
    Object.assign(data.automation, { id: productId === 4 ? 102 : 990102, productId, learningCycleId: cycleId, sourceReference: `experiment:${experimentId}` });
    for (const activity of data.history.activities) for (const task of activity.tasks) task.sourceReference = `experiment:${experimentId}`;
    const activity = data.history.activities.find(a => a.activityId === "commercialIntegrityReview");
    const task = activity.tasks[0];
    task.taskId = taskId;
    const taskSummary = { text: "Têmis pediu provas adicionais dos testes do aplicativo. Esse parecer ainda não aprova a etapa; o complemento precisa passar por uma nova revisão.", source: "DETERMINISTIC", decision: "BLOCKED" };
    task.humanSummary = null;
    let summaryCalls = 0;
    if (productId !== 4) {
      data.history.productName = "Produto sintético B";
      data.history.productInternalName = "Produto B";
      data.taskResult = { contractVersion: "PDE_TEMIS_AGENT_VALIDATION_V1", decision: "BLOCKED", productId, sourceReference: `experiment:${experimentId}`, commercialRationale: "Faltam provas técnicas do produto sintético B.", requiredChanges: ["Complementar a prova sintética e submeter à revisão."] };
    }
    const writes = [];
    let failStatus = false;
    let failSummary = false;
    await context.route("**/*", async route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith("/api/")) return url.origin === baseUrl ? route.continue() : route.abort();
      assert.equal(url.hostname, new URL(baseUrl).hostname, "Toda API do bundle deve ser substituída localmente");
      const headers = { "access-control-allow-origin": baseUrl, "access-control-allow-methods": "GET,POST,OPTIONS", "access-control-allow-headers": "*", "access-control-allow-credentials": "true" };
      if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers });
      let body = [];
      if (route.request().method() === "POST") {
        writes.push(url.pathname);
        assert.ok(url.pathname.endsWith("/pause"), "O teste nunca deve retomar ou iniciar tarefas");
        const input = route.request().postDataJSON();
        assert.equal(input.reasonCode, "COST_RECONCILIATION");
        Object.assign(data.automation, { canResume: false, userAction: {
          code: "RECONCILE_COSTS_BEFORE_RESUME", title: "Confira os custos antes de retomar",
          reason: "O processo foi pausado para conferir os gastos. Há 10 tarefas antigas sem custo informado; por isso o saldo disponível ainda não é conhecido.",
          responsible: "Você · decisão financeira; Plutus · conferência dos custos", actionLabel: "Ver custos pendentes",
          actionUrl: `/financial/execution?productId=${productId}&chainId=27&cycleId=${cycleId}`,
          afterAction: "Após conferir os custos, retome o processo. Têmis fará a revisão pendente, preservando as aprovações anteriores.", evidenceReference: "internal://local/pause-context",
        } });
        body = data.automation;
      } else if (url.pathname.includes("/automation/v1")) {
        if (failStatus) return route.fulfill({ status: 503, headers, contentType: "application/json", body: '{}' });
        body = data.automation;
      } else if (url.pathname.endsWith("/process-context")) body = data.cycle;
      else if (url.pathname.endsWith("/activity-executions")) body = data.history;
      else if (url.pathname.includes("/value-chain-positions/")) body = data.position;
      else if (url.pathname.endsWith("/human-summary")) {
        assert.equal(url.pathname, `/api/business-processes/117/products/${productId}/tasks/${taskId}/human-summary`);
        assert.equal(url.searchParams.get("sourceReference"), `experiment:${experimentId}`);
        summaryCalls += 1;
        if (failSummary) return route.fulfill({ status: 503, headers, contentType: "application/json", body: '{}' });
        body = { humanSummary: taskSummary };
      }
      else if (url.pathname.endsWith("/audit")) body = { ...task, humanSummary: taskSummary, comments: JSON.stringify(data.taskResult) };
      else if (url.pathname.endsWith("/configuration-status")) body = { hasConfiguredPages: false };
      return route.fulfill({ status: 200, headers, contentType: "application/json", body: JSON.stringify(body) });
    });
    const target = `${baseUrl}/products/${productId}/value-chain-history/processes/117/activities?learningCycleId=${cycleId}&chainId=27#activity-commercialIntegrityReview`;
    await page.goto(target);
    const current = page.locator("#activity-commercialIntegrityReview");
    await current.getByRole("region", { name: "Por que esta atividade não continua" }).waitFor();
    await page.getByRole("button", { name: "Registrar motivo da pausa" }).click();
    await page.getByLabel("Por que o processo deve continuar pausado? *", { exact: true }).selectOption("COST_RECONCILIATION");
    await page.getByLabel("Explicação da pausa", { exact: true }).fill("Conferir custos antes de novos gastos.");
    await page.getByRole("button", { name: "Salvar motivo da pausa" }).click();
    const notice = current.getByRole("region", { name: "Por que esta atividade não continua" });
    await notice.getByRole("link", { name: "Ver custos pendentes" }).waitFor();
    assert.match(await notice.innerText(), /10 tarefas antigas/);
    assert.equal(await notice.getByRole("link").getAttribute("href"), data.automation.userAction.actionUrl);
    assert.equal(await page.getByRole("button", { name: "Retomar processo" }).count(), 0);
    assert.ok(await current.getByLabel("Resumo do parecer").isVisible());
    const card = current.locator("details.business-process-document");
    await card.locator(":scope > summary").click();
    const report = card.getByText("Parecer completo de Têmis", { exact: true }).locator("..");
    await report.locator(":scope > summary").click();
    await report.getByText("Visualizar JSON em árvore", { exact: true }).click();
    try {
      await report.getByText('"PDE_TEMIS_AGENT_VALIDATION_V1"', { exact: true }).waitFor({ timeout: 5000 });
    } catch (error) {
      console.log(await card.locator("details").evaluateAll(nodes => nodes.map(node => ({ title: node.querySelector(":scope > summary")?.textContent, open: node.open }))));
      await page.screenshot({path: path.join(outputPath, "report-failure.png"), fullPage: false});
      throw error;
    }
    await notice.scrollIntoViewIfNeeded();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 2);
    assert.equal(overflow, false, "A orientação deve caber no dispositivo");
    await page.screenshot({ path: path.join(outputPath, `${productId}-${device.replaceAll(' ', '-')}.png`), fullPage: false });
    assert.equal(writes.length, 1);
    assert.ok(summaryCalls > 0, "A lista leve não contém o parecer; a síntese deve vir da consulta curta");
    results.push({ productId, device, status: "PASS", pauseWrites: writes.length, summaryCalls, paidCalls: 0, scope: "LOCAL_TEST_DOUBLE" });
    if (productId === 4 && device === "desktop") {
      failSummary = true; await page.reload();
      await current.getByText("Não foi possível carregar o resumo simples.", { exact: false }).waitFor({ timeout: 20000 });
      failSummary = false;
      await current.getByRole("button", { name: "Atualizar resumo" }).click();
      await current.getByLabel("Resumo do parecer").waitFor();
      assert.equal(writes.length, 1);
      results.push({ productId, device, status: "PASS", scenario: "SUMMARY_RECOVERY", paidCalls: 0 });
      failStatus = true; await page.reload();
      await page.getByText("Não foi possível atualizar o motivo da pausa.", { exact: false }).waitFor({ timeout: 20000 });
      results.push({ productId, device, status: "PASS", scenario: "STATUS_UNAVAILABLE", paidCalls: 0 });
    }
    await context.close();
  }
  await writeFile(path.join(outputPath, "browser-results.json"), JSON.stringify(results, null, 2) + "\n");
  console.log(JSON.stringify(results));
} finally { await browser.close(); }
