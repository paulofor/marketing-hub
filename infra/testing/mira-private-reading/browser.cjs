const assert = require("node:assert/strict");
const { chromium, devices } = require("playwright");
const fs = require("node:fs");
const output = process.argv[2];
if (!output) throw new Error("Informe o diretório de evidências locais.");
fs.mkdirSync(output, { recursive: true });
const api = "http://127.0.0.1:18090";
const pde = process.env.MIRA_TEST_PDE_URL || "http://127.0.0.1:18076";
const admin = "http://127.0.0.1:15173/local-validation/mira-reading.html";
const profiles = [["desktop", { viewport: { width: 1365, height: 1000 } }], ["iphone", devices["iPhone 15 Pro"]], ["pixel", devices["Pixel 7"]]];

async function unavailableEvidence() {
  const response = await fetch(`${api}/api/products/10/private-readings/privateReading1`);
  assert.equal(response.status, 200);
  const workspace = await response.json();
  assert.equal(workspace.status, "EVIDENCE_UNAVAILABLE");
  assert.equal(workspace.canRecord, false);
  assert.deepEqual(workspace.signals, {});
  assert.equal(workspace.evidenceId, null);
  const browser = await chromium.launch({ executablePath: "/usr/bin/chromium", args: ["--no-sandbox"] });
  try {
    for (const [name, options] of profiles) {
      const { defaultBrowserType, ...profile } = options;
      const context = await browser.newContext(profile);
      await context.route("https://mira.sandbox.local/**", route =>
        route.fulfill({ status: 302, headers: { location: `${pde}/mira-private` } }));
      const page = await context.newPage();
      await page.goto(admin);
      await page.getByRole("alert").filter({ hasText: "não foi possível consultar" }).waitFor();
      assert.equal(await page.getByText("Consulta indisponível", { exact: true }).count(), 5);
      assert.equal(await page.getByRole("checkbox").count(), 0);
      assert.equal(await page.getByRole("button", { name: "Registrar resultado da leitura" }).isDisabled(), true);
      await page.screenshot({ path: `${output}/${name}-consulta-indisponivel.png`, fullPage: true });
      const popup = context.waitForEvent("page");
      await page.getByRole("link", { name: "Abrir protótipo de Mira" }).click();
      const prototype = await popup;
      await prototype.getByLabel("Código do convite").waitFor();
      await participantLanguage(prototype);
      await context.close();
      process.stdout.write(`${name}: acesso aceito preservado com PDE indisponível; registro bloqueado\n`);
    }
    const refused = await fetch(`${api}/api/business-processes/68/products/10/activities/privateReading1/execution-requests`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
        decision: "APPROVE", confirmationToken: "CONFIRM:pde-construction-approval:privateReading1",
        structuredEvidence: { evidenceId: "forged", humanReadingConfirmed: true } }),
    });
    assert.equal(refused.status, 400);
  } finally { await browser.close(); }
}

async function participantLanguage(page) {
  assert.equal(await page.title(), "Sua rotina, organizada com calma");
  assert.doesNotMatch(await page.locator(".mira-private-shell").innerText(), /\bMira\b|mira-private-v\d|Marketing Hub|QA_INTERNAL|validação privada/i);
}

async function evidence(number) {
  const response = await fetch(`${pde}/api/pde/mira/private/v1/internal/readings/${number}`, {
    headers: { "X-PDE-Internal-Token": "mira-local-internal" },
  });
  assert.equal(response.status, 200);
  return response.json();
}

async function consentAndUse(page, token, positive) {
  await page.goto(`${pde}/mira-private#access=${token}`);
  await page.getByRole("heading", { name: "Sua rotina, organizada com calma" }).waitFor();
  await participantLanguage(page);
  assert.equal(new URL(page.url()).hash, "");
  const button = page.getByRole("button", { name: "Começar leitura privada" });
  assert.equal(await button.isDisabled(), true);
  await page.getByRole("checkbox").check();
  await button.click();
  await page.locator(".mira-private-shell").filter({ has: page.getByRole("heading", { name: /Conte o mínimo necessário|Uma ordem simples para consultar/ }) }).waitFor();
  if (await page.getByRole("heading", { name: "Conte o mínimo necessário" }).count()) {
    const generate = page.getByRole("button", { name: "Gerar rotina segura" });
    assert.equal(await generate.isDisabled(), true);
    await page.getByLabel("Nome", { exact: true }).nth(0).fill("Hidratante sintético local");
    await page.getByLabel("Como o rótulo orienta usar").nth(0).fill("Aplicar após a limpeza");
    await page.getByLabel("Nome", { exact: true }).nth(1).fill("Limpador sintético local");
    await page.getByLabel("Como o rótulo orienta usar").nth(1).fill("Usar para limpar e enxaguar");
    if (!positive) {
      await page.getByLabel("Objetivo de autocuidado").fill("Diagnosticar uma doença");
      await generate.click();
      await page.getByRole("alert").filter({ hasText: "conclusão clínica" }).waitFor();
      await participantLanguage(page);
      await page.getByLabel("Objetivo de autocuidado").fill("Organizar meus produtos");
    }
    if (token === "mira-local-human-one") {
      await page.route("**/api/pde/mira/private/v1/generate", route => route.abort("failed"), { times: 1 });
      await generate.click();
      await page.getByRole("alert").waitFor();
      await participantLanguage(page);
      await page.reload();
      await page.getByRole("heading", { name: "Conte o mínimo necessário" }).waitFor();
      assert.equal(await page.getByLabel("Nome", { exact: true }).nth(0).inputValue(), "Hidratante sintético local");
      assert.equal(await page.getByLabel("Como o rótulo orienta usar").nth(1).inputValue(), "Usar para limpar e enxaguar");
    }
    await generate.click();
    await page.getByRole("heading", { name: "Uma ordem simples para consultar" }).waitFor();
    await page.getByRole("button", { name: "Marcar uma parte como consultada" }).click();
    if (positive) {
      await page.getByRole("button", { name: "Sim, prefiro a rotina pronta" }).click();
      await page.getByRole("button", { name: "Simular avanço — sem cobrança" }).click();
    } else {
      await page.getByRole("button", { name: "Não, prefiro a alternativa gratuita" }).click();
    }
  }
  await page.getByText("Leitura encerrada.", { exact: false }).waitFor();
  await participantLanguage(page);
  await page.reload();
  await page.getByText("Leitura encerrada.", { exact: false }).waitFor();
  assert.equal(await page.locator('input[autocomplete="cc-number"]').count(), 0);
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth), true);
}

(async () => {
  if (process.argv.includes("--unavailable")) return unavailableEvidence();
  const denied = await fetch(`${pde}/api/pde/mira/private/v1/internal/readings/1`);
  assert.equal(denied.status, 403);
  const navigation = await fetch(`${pde}/mira-private`);
  assert.match(navigation.headers.get("cache-control"), /no-store/);
  assert.equal(navigation.headers.get("referrer-policy"), "no-referrer");
  assert.match(navigation.headers.get("x-robots-tag"), /noindex/);
  const noConsent = await fetch(`${pde}/api/pde/mira/private/v1/access`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ accessToken: "mira-local-human-one", consentAccepted: false }) });
  assert.equal(noConsent.status, 403);
  assert.equal((await evidence(1)).trafficClass, "NOT_STARTED");
  const fake = await fetch(`${api}/api/business-processes/68/products/10/activities/privateReading1/execution-requests`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ decision: "APPROVE",
      confirmationToken: "CONFIRM:pde-construction-approval:privateReading1", structuredEvidence: { evidenceId: "forged", humanReadingConfirmed: true,
        participantReference: "PV-000000000001", consentConfirmed: true, firstPartyEvidenceConfirmed: true,
        signals: Object.fromEntries(["EXPERIENCE_STARTED", "VALUE_MOMENT", "READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED"].map(s => [s, true])) } }),
  });
  assert.equal(fake.status, 400);
  const browser = await chromium.launch({ executablePath: "/usr/bin/chromium", args: ["--no-sandbox"] });
  try {
    for (const [name, options] of profiles) {
      const { defaultBrowserType, ...profile } = options;
      const context = await browser.newContext(profile);
      context.setDefaultTimeout(30000);
      const errors = [];
      context.on("page", page => page.on("pageerror", e => errors.push(e.message)));
      await context.route("**/*", async route => {
        const url = new URL(route.request().url());
        if (url.hostname === "mira.sandbox.local") {
          return route.fulfill({ response: await context.request.fetch(pde + url.pathname + url.search, { method: route.request().method(), data: route.request().postDataBuffer(), headers: route.request().headers() }) });
        }
        if (!["localhost", "127.0.0.1", new URL(pde).hostname].includes(url.hostname)) {
          errors.push(`Requisição externa não autorizada no teste: ${url.origin}`); return route.abort();
        }
        assert.equal(route.request().url().includes("mira-local-human"), false);
        return route.continue();
      });
      const panel = await context.newPage();
      await panel.goto(admin);
      const link = panel.getByRole("link", { name: "Abrir protótipo de Mira" });
      await link.waitFor();
      assert.equal(await link.getAttribute("href"), "https://mira.sandbox.local/mira-private");
      if (name === "desktop") assert.equal(await panel.getByRole("button", { name: "Registrar resultado da leitura" }).isDisabled(), true);
      const popupPromise = context.waitForEvent("page");
      await link.click();
      const prototype = await popupPromise;
      await prototype.getByLabel("Código do convite").waitFor();
      await participantLanguage(prototype);
      await prototype.screenshot({ path: `${output}/${name}-entrada.png`, fullPage: true });
      await consentAndUse(prototype, "mira-local-human-one", true);
      await prototype.screenshot({ path: `${output}/${name}-resultado.png`, fullPage: true });
      const first = await evidence(1);
      assert.equal(first.trafficClass, "PRIVATE_READING");
      assert.equal(Object.values(first.signals).filter(Boolean).length, 5);
      assert.ok(first.finishedAt);
      await panel.bringToFront();
      await panel.getByRole("button", { name: "Atualizar resultado", exact: true }).click();
      const confirm = panel.getByRole("checkbox"); await confirm.waitFor();
      assert.equal(await confirm.isChecked(), false);
      if (name === "desktop") {
        await confirm.check();
        await panel.getByRole("button", { name: "Registrar resultado da leitura" }).click();
        await panel.getByTestId("bpm-outcome").filter({ hasText: "COMPLETED" }).waitFor();
      }
      await panel.screenshot({ path: `${output}/${name}-atividade.png`, fullPage: true });
      assert.equal(await panel.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth), true);
      await consentAndUse(prototype, "mira-local-human-two", false);
      const second = await evidence(2);
      assert.notEqual(first.participantReference, second.participantReference);
      assert.equal(second.signals.PREFERRED_OVER_FREE, false); assert.equal(second.signals.CHECKOUT_STARTED, false);
      assert.ok(second.finishedAt);
      if (name === "desktop") {
        await panel.goto(admin + "?activity=privateReading2");
        await panel.getByRole("checkbox").check();
        await panel.getByRole("button", { name: "Registrar resultado da leitura" }).click();
        await panel.getByTestId("bpm-outcome").filter({ hasText: "BLOCKED" }).waitFor();
      }
      await consentAndUse(prototype, "mira-local-qa", true);
      assert.deepEqual(await evidence(1), first); assert.deepEqual(await evidence(2), second);
      assert.deepEqual(errors, []);
      await context.close();
      process.stdout.write(`${name}: acesso, consentimento, retomada, prova, negativa, QA e atividade aprovados\n`);
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
