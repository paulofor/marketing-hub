import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";
import { chromium, devices } from "playwright-core";

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
if (!inputPath || !outputPath || !evidenceDirectory) {
  throw new Error("Uso: node alcyone-agent-validation-harness.mjs <input> <output> <evidencias>");
}
const input = JSON.parse(await readFile(inputPath, "utf8"));
const internalToken = process.env.PDE_INTERNAL_API_TOKEN?.trim();
if (!internalToken) throw new Error("PDE_INTERNAL_API_TOKEN não foi configurado para Alcyone.");
await mkdir(evidenceDirectory, { recursive: true });

const apiBase = "/api/pde/agent-validation/v1/products/pde-planejado-46";
const profiles = {
  DESKTOP_1440: { viewport: { width: 1440, height: 900 } },
  IPHONE_15_PRO: { ...devices["iPhone 15 Pro"], defaultBrowserType: "chromium" },
  PIXEL_7: { ...devices["Pixel 7"] },
};
const plans = input.mode === "TECHNICAL"
  ? ["ADHERENT", "RECOVERY", "SAFETY"].flatMap((scenario) =>
      Object.keys(profiles).map((profile) => [scenario, profile]))
  : [[input.scenarioCode, profileFor(input.scenarioCode)]];
const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN;
const browser = await chromium.launch({
  ...(executablePath ? { executablePath } : {}),
  headless: true,
});
const startedAt = new Date();
const scenarios = [];
const artifacts = [];

try {
  for (const [scenarioCode, deviceProfile] of plans) {
    scenarios.push(await executeScenario(scenarioCode, deviceProfile));
  }
} finally {
  await browser.close();
}

const finishedAt = new Date();
const devicesResult = Object.keys(profiles).map((deviceProfile) => ({
  deviceProfile,
  viewportWidth: profiles[deviceProfile].viewport.width,
  viewportHeight: profiles[deviceProfile].viewport.height,
  status: scenarios
    .filter((scenario) => scenario.deviceProfile === deviceProfile)
    .every((scenario) => scenario.status === "PASS")
    ? "PASS"
    : "FAIL",
  screenshotEvidenceKeys: artifacts
    .filter((artifact) => artifact.deviceProfile === deviceProfile)
    .map((artifact) => artifact.evidenceKey),
}));
const checks = {
  sameVersion: scenarios.every((scenario) => scenario.prototypeVersion === input.prototypeVersion),
  desktopAndMobile:
    input.mode !== "TECHNICAL" ||
    devicesResult.every((device) => device.status === "PASS"),
  happyResultWithinTenMinutes: scenarios
    .filter((scenario) => scenario.scenarioCode === "ADHERENT")
    .every((scenario) => scenario.resultReadySeconds <= 600),
  recoveryPreserved:
    input.mode !== "TECHNICAL" ||
    scenarios
      .filter((scenario) => scenario.scenarioCode === "RECOVERY")
      .every((scenario) => scenario.resumed && scenario.recovered),
  safetyBlocked:
    input.mode !== "TECHNICAL" ||
    scenarios
      .filter((scenario) => scenario.scenarioCode === "SAFETY")
      .every((scenario) => scenario.safetyBlocked),
  accessibilityBasic: scenarios.every((scenario) => scenario.accessibilityBasic),
  responsiveLayout: scenarios.every((scenario) => scenario.noHorizontalOverflow),
  privacyPreserved: scenarios.every((scenario) => scenario.privacyPreserved),
  internalTrafficSegregated: scenarios.every(
    (scenario) => scenario.trafficClass === "AGENT_VALIDATION" && scenario.mhInternalTest,
  ),
  paymentDisabled: scenarios.every((scenario) => scenario.sideEffects.paymentEnabled === false),
  publicationDisabled: scenarios.every((scenario) => scenario.sideEffects.published === false),
  campaignDisabled: scenarios.every((scenario) => scenario.sideEffects.campaignCreated === false),
  zeroMediaSpend: scenarios.every((scenario) => scenario.sideEffects.mediaSpendBrl === 0),
  staticFixturesValid: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.staticFixturesValid),
  providerCallsZero: scenarios.every((scenario) => scenario.providerCalls === 0),
  nineScenarioDeviceGates: input.mode !== "TECHNICAL" || scenarios.length === 9,
};
const approved = scenarios.every((scenario) => scenario.status === "PASS")
  && Object.values(checks).every(Boolean);
await writeFile(outputPath, JSON.stringify({
  contractVersion: "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
  mode: input.mode,
  decision: approved ? "APPROVED" : "BLOCKED",
  sourceReference: input.sourceReference,
  productId: input.productId,
  productSlug: input.productSlug,
  publicUrl: input.sourceUrl,
  prototypeVersion: input.prototypeVersion,
  trafficClass: "AGENT_VALIDATION",
  internalMarker: "mh_internal_test",
  startedAt: startedAt.toISOString(),
  finishedAt: finishedAt.toISOString(),
  durationSeconds: Math.max(0, Math.ceil((finishedAt - startedAt) / 1000)),
  devices: devicesResult,
  scenarios,
  checks,
  artifacts,
  fixtureContract: "PDE_STATIC_RESULT_FIXTURES_V1",
  sideEffects: {
    paymentEnabled: false,
    published: false,
    campaignCreated: false,
    mediaSpendBrl: 0,
  },
  humanEvidenceClaimed: false,
  commercialEvidenceClaimed: false,
  evidence: scenarios.map((scenario) => scenario.evidenceId),
}), "utf8");

/** Executa um cenário em um dispositivo com sessão e evidência exclusivas. */
async function executeScenario(scenarioCode, deviceProfile) {
  const scenarioStartedAt = new Date();
  const session = await internalApi("/internal/sessions", {
    method: "POST",
    body: JSON.stringify({ sourceReference: input.sourceReference, scenarioCode }),
  });
  if (!session.mhInternalTest || session.trafficClass !== "AGENT_VALIDATION") {
    throw new Error("A API não abriu uma sessão Alcyone segregada.");
  }
  const context = await browser.newContext({
    ...profiles[deviceProfile],
    locale: "pt-BR",
    timezoneId: "UTC",
  });
  const page = await context.newPage();
  const pageErrors = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await page.addInitScript(
    ([key, value]) => window.sessionStorage.setItem(key, value),
    ["alcyone-agent-validation-session", session.sessionToken],
  );
  let resumed = false;
  let recovered = false;
  let safetyBlocked = false;
  let staticFixturesValid = scenarioCode === "SAFETY";
  let resultReadyAt = null;
  try {
    await page.goto(input.sourceUrl, { waitUntil: "domcontentloaded", timeout: 45_000 });
    await page.getByRole("heading", { name: "Conte o mínimo necessário" }).waitFor();
    await page.getByTestId("agent-validation-mode").waitFor();
    await fillInput(page, scenarioCode === "SAFETY");
    await page.getByRole("button", { name: "Salvar entrada segura" }).click();
    if (scenarioCode === "RECOVERY") {
      await page.route(`**${apiBase}/generate`, (route) => route.abort("failed"), { times: 1 });
      await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
      await page.getByRole("alert").waitFor();
      await page.reload({ waitUntil: "domcontentloaded" });
      resumed = true;
      if ((await page.getByLabel("Ocasião", { exact: true }).inputValue()) !== "Jantar de formatura") {
        throw new Error("A retomada não preservou a entrada aceita.");
      }
    }
    await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
    if (scenarioCode === "SAFETY") {
      await page.getByRole("heading", { name: "Este pedido ficou fora do protótipo" }).waitFor();
      if (await page.locator(".alcyone-look-grid").count()) {
        throw new Error("O cenário de segurança apresentou um pacote visual.");
      }
      await page.getByRole("button", { name: "Registrar bloqueio seguro" }).click();
      await page.getByRole("button", { name: "Concluir cenário de segurança" }).click();
      safetyBlocked = true;
    } else {
      await page.getByRole("heading", { name: "Três combinações para sua ocasião" }).waitFor();
      resultReadyAt = new Date();
      staticFixturesValid = await validateFixtures(page);
      await page.getByLabel("O que tornou o pacote útil?").fill(
        "As três opções reduziram a dúvida e são aplicáveis à ocasião.",
      );
      await page.getByRole("button", { name: "Este pacote resolve minha decisão" }).click();
      await page.getByRole("button", { name: "Escolher combinação 1" }).click();
      await page.getByLabel("Por que prefere este pacote à pesquisa gratuita?").fill(
        "É mais direto e coerente do que pesquisar referências soltas.",
      );
      await page.getByRole("button", { name: "Prefiro isto à pesquisa gratuita" }).click();
      await page.getByRole("button", { name: "Abrir checkout simulado de R$ 79" }).click();
      await page.getByRole("heading", { name: "Simulação concluída — nenhuma cobrança realizada" }).waitFor();
      if (scenarioCode === "RECOVERY") {
        await page.getByRole("button", { name: "Confirmar retomada do mesmo pacote" }).click();
        recovered = true;
      }
      await page.getByRole("button", { name: "Concluir cenário interno" }).click();
    }
    await page.getByRole("heading", { name: "Homologação concluída" }).waitFor();
    const dimensions = await page.evaluate(() => ({
      width: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
      lang: document.documentElement.lang,
      url: window.location.href,
      text: document.body.innerText,
      controlsNamed: Array.from(document.querySelectorAll("input, textarea, button, a[href]"))
        .filter((control) => control.getAttribute("type") !== "hidden")
        .every((control) => Boolean(
          control.getAttribute("aria-label")?.trim()
          || Array.from(control.labels || []).some((label) => label.textContent?.trim())
          || control.textContent?.trim(),
        )),
      touchTargets: Array.from(document.querySelectorAll("button"))
        .every((button) => button.getBoundingClientRect().height >= 44),
    }));
    const screenshotPath = resolve(
      evidenceDirectory,
      `${scenarioCode.toLowerCase()}-${deviceProfile.toLowerCase()}.png`,
    );
    await page.screenshot({ path: screenshotPath, fullPage: true });
    const evidenceKey = `${scenarioCode}-${deviceProfile}-FULL_PAGE`;
    artifacts.push({
      captureSessionId: input.captureSessionId,
      evidenceKey,
      evidenceType: "FULL_PAGE",
      deviceProfile,
      pageNumber: 1,
      foldNumber: null,
      viewportWidth: profiles[deviceProfile].viewport.width,
      viewportHeight: profiles[deviceProfile].viewport.height,
      pageHeightPx: await page.evaluate(() => document.documentElement.scrollHeight),
      scrollY: 0,
      sourceUrl: input.sourceUrl,
      finalUrl: dimensions.url,
      capturedAt: new Date().toISOString(),
      localPath: screenshotPath,
    });
    const evidence = await internalApi(`/internal/evidence/${session.evidenceId}`);
    if (!evidence.finished || evidence.providerCalls !== 0 || evidence.providerCostUsd !== 0) {
      throw new Error("A evidência não comprovou encerramento e custo externo zero.");
    }
    const expectedEvents = scenarioCode === "SAFETY"
      ? ["EXPERIENCE_STARTED", "SAFETY_LIMIT_BLOCKED", "AGENT_SCENARIO_COMPLETED"]
      : [
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED",
          ...(scenarioCode === "RECOVERY" ? ["RECOVERY_COMPLETED"] : []),
          "AGENT_SCENARIO_COMPLETED",
        ];
    if (JSON.stringify([...evidence.events].sort()) !== JSON.stringify([...expectedEvents].sort())) {
      throw new Error("A trilha persistida diverge dos eventos realmente executados.");
    }
    return {
      scenarioCode,
      deviceProfile,
      status: "PASS",
      evidenceId: evidence.evidenceId,
      prototypeVersion: evidence.prototypeVersion,
      trafficClass: evidence.trafficClass,
      mhInternalTest: evidence.mhInternalTest,
      readingId: evidence.readingId,
      executionId: evidence.executionId,
      events: evidence.events,
      resumed,
      recovered,
      safetyBlocked,
      staticFixturesValid,
      providerCalls: evidence.providerCalls,
      resultReadySeconds: resultReadyAt
        ? Math.max(0, Math.ceil((resultReadyAt - scenarioStartedAt) / 1000))
        : 0,
      accessibilityBasic:
        dimensions.lang === "pt-BR" && dimensions.controlsNamed && dimensions.touchTargets,
      noHorizontalOverflow: dimensions.width <= dimensions.viewport + 1,
      privacyPreserved:
        !dimensions.url.includes(session.sessionToken)
        && !dimensions.text.includes(session.sessionToken),
      sideEffects: evidence.sideEffects,
      humanEvidenceClaimed: evidence.humanEvidenceClaimed,
      commercialEvidenceClaimed: evidence.commercialEvidenceClaimed,
      screenshotEvidenceKeys: [evidenceKey],
    };
  } finally {
    await context.close();
  }
}

/** Preenche somente referências sintéticas e ativa o limite proibido em SAFETY. */
async function fillInput(page, unsafe) {
  await page.getByLabel("Ocasião", { exact: true }).fill("Jantar de formatura");
  await page.getByLabel("Data da ocasião").fill("2026-12-15");
  await page.getByLabel("Preferências").fill(unsafe ? "quero enviar foto corporal" : "linhas simples, tons frios");
  await page.getByLabel("Restrições práticas").fill(unsafe ? "quero comprar uma roupa nova" : "clima ameno, sem salto alto");
  await page.getByLabel("Referências isoladas das peças").fill(
    "piece-fixture-01\npiece-fixture-02\npiece-fixture-03\npiece-fixture-04",
  );
}

/** Valida assinatura, hash, dimensões e carregamento das três fixtures geradas por código. */
async function validateFixtures(page) {
  const manifestResponse = await fetch(new URL("/assets/alcyone/manifest.json", input.sourceUrl));
  if (!manifestResponse.ok) return false;
  const manifest = await manifestResponse.json();
  if (
    manifest.contractVersion !== "PDE_STATIC_RESULT_FIXTURES_V1"
    || manifest.prototypeVersion !== input.prototypeVersion
    || manifest.providerCalls !== 0
    || manifest.artifacts?.length !== 3
  ) return false;
  for (const artifact of manifest.artifacts) {
    const response = await fetch(new URL(artifact.path, input.sourceUrl));
    if (!response.ok) return false;
    const bytes = Buffer.from(await response.arrayBuffer());
    if (
      createHash("sha256").update(bytes).digest("hex") !== artifact.sha256
      || bytes.subarray(0, 8).toString("hex") !== "89504e470d0a1a0a"
      || bytes.readUInt32BE(16) !== 1024
      || bytes.readUInt32BE(20) !== 1024
    ) return false;
  }
  return page.locator(".alcyone-look-grid img").evaluateAll((images) =>
    images.length === 3
    && images.every((image) => image.complete && image.naturalWidth === 1024 && image.naturalHeight === 1024));
}

/** Chama somente as rotas internas protegidas da mesma superfície. */
async function internalApi(path, init = {}) {
  const response = await fetch(new URL(`${apiBase}${path}`, input.sourceUrl), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Internal-Token": internalToken,
      ...(init.headers || {}),
    },
    signal: AbortSignal.timeout(30_000),
  });
  if (!response.ok) throw new Error(`Alcyone harness ${path}: HTTP ${response.status}`);
  return response.json();
}

/** Seleciona o dispositivo canônico para uma revisão isolada de Psique. */
function profileFor(scenarioCode) {
  if (scenarioCode === "ADHERENT") return "DESKTOP_1440";
  if (scenarioCode === "RECOVERY") return "IPHONE_15_PRO";
  if (scenarioCode === "SAFETY") return "PIXEL_7";
  throw new Error(`Cenário não suportado: ${scenarioCode}`);
}
