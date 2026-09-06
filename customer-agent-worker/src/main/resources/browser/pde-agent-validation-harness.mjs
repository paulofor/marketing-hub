import { chromium } from "playwright-core";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
if (!inputPath || !outputPath || !evidenceDirectory) {
  throw new Error("Uso: node pde-agent-validation-harness.mjs <input> <output> <evidencias>");
}

const input = JSON.parse(await readFile(inputPath, "utf8"));
const internalToken = process.env.PDE_INTERNAL_API_TOKEN?.trim();
if (!internalToken) throw new Error("PDE_INTERNAL_API_TOKEN não foi configurado para o harness.");
await mkdir(evidenceDirectory, { recursive: true });

const profiles = {
  DESKTOP_1440: {
    viewport: { width: 1440, height: 900 },
    userAgent: "MarketingHubAgentValidation/1.0 Desktop Chromium",
    isMobile: false,
    hasTouch: false,
  },
  IPHONE_15_PRO: {
    viewport: { width: 393, height: 852 },
    userAgent:
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 MarketingHubAgentValidation/1.0",
    isMobile: true,
    hasTouch: true,
  },
  PIXEL_7: {
    viewport: { width: 412, height: 915 },
    userAgent:
      "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36 MarketingHubAgentValidation/1.0",
    isMobile: true,
    hasTouch: true,
  },
};

const plans =
  input.mode === "TECHNICAL"
    ? [
        ["ADHERENT", "DESKTOP_1440"],
        ["ADHERENT", "IPHONE_15_PRO"],
        ["ADHERENT", "PIXEL_7"],
        ["RECOVERY", "IPHONE_15_PRO"],
        ["SAFETY", "PIXEL_7"],
      ]
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
const deviceResults = Object.keys(profiles)
  .filter((device) => plans.some(([, plannedDevice]) => plannedDevice === device))
  .map((deviceProfile) => ({
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
const scenarioCodes = new Set(scenarios.map((scenario) => scenario.scenarioCode));
const checks = {
  sameVersion: new Set(scenarios.map((scenario) => scenario.prototypeVersion)).size === 1,
  desktopAndMobile:
    input.mode !== "TECHNICAL" ||
    ["DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"].every((device) =>
      deviceResults.some((result) => result.deviceProfile === device && result.status === "PASS"),
    ),
  happyResultWithinTenMinutes: scenarios
    .filter((scenario) => scenario.scenarioCode === "ADHERENT")
    .every((scenario) => scenario.resultReadySeconds <= 600),
  recoveryPreserved:
    input.mode !== "TECHNICAL" ||
    scenarios.some(
      (scenario) => scenario.scenarioCode === "RECOVERY" && scenario.resumed && scenario.recovered,
    ),
  safetyBlocked:
    input.mode !== "TECHNICAL" ||
    scenarios.some((scenario) => scenario.scenarioCode === "SAFETY" && scenario.safetyBlocked),
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
};
const completeScenarioSet =
  input.mode !== "TECHNICAL" || ["ADHERENT", "RECOVERY", "SAFETY"].every((code) => scenarioCodes.has(code));
const approved =
  completeScenarioSet &&
  scenarios.every((scenario) => scenario.status === "PASS") &&
  Object.values(checks).every(Boolean);
const output = {
  contractVersion: "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
  mode: input.mode,
  decision: approved ? "APPROVED" : "BLOCKED",
  sourceReference: input.sourceReference,
  productId: input.productId,
  productSlug: input.productSlug,
  publicUrl: input.sourceUrl,
  prototypeVersion: scenarios[0]?.prototypeVersion,
  trafficClass: "AGENT_VALIDATION",
  internalMarker: "mh_internal_test",
  startedAt: startedAt.toISOString(),
  finishedAt: finishedAt.toISOString(),
  durationSeconds: Math.max(0, Math.ceil((finishedAt.getTime() - startedAt.getTime()) / 1000)),
  devices: deviceResults,
  scenarios,
  checks,
  artifacts,
  sideEffects: {
    paymentEnabled: false,
    published: false,
    campaignCreated: false,
    mediaSpendBrl: 0,
  },
  humanEvidenceClaimed: false,
  commercialEvidenceClaimed: false,
  evidence: scenarios.map((scenario) => scenario.evidenceId),
};
await writeFile(outputPath, JSON.stringify(output), "utf8");

async function executeScenario(scenarioCode, deviceProfile) {
  const scenarioStartedAt = new Date();
  const session = await api("/internal/agent-validations/sessions", {
    method: "POST",
    body: JSON.stringify({ sourceReference: input.sourceReference, scenarioCode }),
  });
  if (!session.agentValidation || session.trafficClass !== "AGENT_VALIDATION") {
    throw new Error("A API não abriu uma sessão multiagente segregada.");
  }
  const profile = profiles[deviceProfile];
  const context = await browser.newContext({
    ...profile,
    locale: "pt-BR",
    timezoneId: "UTC",
  });
  const page = await context.newPage();
  await page.addInitScript(
    ([key, value]) => window.sessionStorage.setItem(key, value),
    ["mira-private-session", session.sessionToken],
  );
  let resumed = false;
  let recovered = false;
  let safetyBlocked = false;
  let resultReadyAt = null;
  try {
    await page.goto(input.sourceUrl, { waitUntil: "domcontentloaded", timeout: 45_000 });
    await page.getByRole("heading", { name: "Conte o mínimo necessário" }).waitFor();
    await page.getByTestId("agent-validation-mode").waitFor();
    if (scenarioCode === "ADHERENT") {
      await fillValidInput(page);
      await page.getByRole("button", { name: "Gerar rotina segura" }).click();
      await page.getByRole("heading", { name: "Uma ordem simples para consultar" }).waitFor();
      resultReadyAt = new Date();
      await page.getByRole("button", { name: "Marcar uma parte como consultada" }).click();
      await page.getByRole("button", { name: "Concluir cenário interno" }).click();
    } else if (scenarioCode === "RECOVERY") {
      if (await page.getByRole("button", { name: "Gerar rotina segura" }).isEnabled()) {
        throw new Error("Entrada vazia ficou habilitada no cenário de recuperação.");
      }
      await fillValidInput(page);
      await page.route("**/api/pde/mira/private/v1/generate", (route) => route.abort("failed"), {
        times: 1,
      });
      await page.getByRole("button", { name: "Gerar rotina segura" }).click();
      await page.getByRole("alert").waitFor();
      await page.reload({ waitUntil: "domcontentloaded" });
      resumed = true;
      await page.getByRole("heading", { name: "Conte o mínimo necessário" }).waitFor();
      await page.getByRole("button", { name: "Gerar rotina segura" }).click();
      await page.getByRole("heading", { name: "Uma ordem simples para consultar" }).waitFor();
      resultReadyAt = new Date();
      recovered = true;
      await page.getByRole("button", { name: "Marcar uma parte como consultada" }).click();
      await sessionEvent(session.sessionToken, "RECOVERY_COMPLETED");
      await page.reload({ waitUntil: "domcontentloaded" });
      await page.getByRole("button", { name: "Concluir cenário interno" }).click();
    } else if (scenarioCode === "SAFETY") {
      await fillValidInput(page, "Diagnosticar e tratar manchas da pele");
      await page.getByRole("button", { name: "Gerar rotina segura" }).click();
      await page.getByRole("alert").filter({ hasText: "conclusão clínica" }).waitFor();
      await sessionEvent(session.sessionToken, "SAFETY_LIMIT_BLOCKED");
      await page.reload({ waitUntil: "domcontentloaded" });
      await page.getByRole("button", { name: "Concluir cenário de segurança" }).click();
      safetyBlocked = true;
    } else {
      throw new Error(`Cenário não suportado: ${scenarioCode}`);
    }
    await page.getByRole("heading", { name: /concluída|encerrada/i }).waitFor();
    const dimensions = await page.evaluate(() => ({
      bodyWidth: document.body.scrollWidth,
      viewportWidth: window.innerWidth,
      labels: document.querySelectorAll("label").length,
      controls: document.querySelectorAll("input, textarea, select, button").length,
      bodyText: document.body.innerText,
      currentUrl: window.location.href,
    }));
    const screenshotPath = resolve(
      evidenceDirectory,
      `${scenarioCode.toLowerCase()}-${deviceProfile.toLowerCase()}.png`,
    );
    await page.screenshot({ path: screenshotPath, fullPage: true });
    const capturedAt = new Date().toISOString();
    const pageHeightPx = await page.evaluate(() => document.documentElement.scrollHeight);
    const evidenceKey = `${scenarioCode}-${deviceProfile}-FULL_PAGE`;
    artifacts.push({
      captureSessionId: input.captureSessionId,
      evidenceKey,
      evidenceType: "FULL_PAGE",
      deviceProfile,
      pageNumber: 1,
      foldNumber: null,
      viewportWidth: profile.viewport.width,
      viewportHeight: profile.viewport.height,
      pageHeightPx,
      scrollY: 0,
      sourceUrl: input.sourceUrl,
      finalUrl: dimensions.currentUrl,
      capturedAt,
      localPath: screenshotPath,
    });
    const evidence = await api(`/internal/agent-validations/evidence/${session.evidenceId}`);
    const scenarioFinishedAt = new Date();
    return {
      scenarioCode,
      deviceProfile,
      status: "PASS",
      evidenceId: evidence.evidenceId,
      prototypeVersion: evidence.prototypeVersion,
      trafficClass: evidence.trafficClass,
      mhInternalTest: evidence.mhInternalTest,
      events: evidence.events,
      blocker: evidence.blocker,
      resumed,
      recovered,
      safetyBlocked,
      resultReadySeconds: resultReadyAt
        ? Math.max(0, Math.ceil((resultReadyAt.getTime() - scenarioStartedAt.getTime()) / 1000))
        : 0,
      accessibilityBasic: dimensions.controls === 0 || dimensions.labels > 0,
      privacyPreserved:
        !dimensions.currentUrl.includes(session.sessionToken) &&
        !dimensions.bodyText.includes(session.sessionToken) &&
        !dimensions.bodyText.includes("Mira"),
      noHorizontalOverflow: dimensions.bodyWidth <= dimensions.viewportWidth + 1,
      startedAt: scenarioStartedAt.toISOString(),
      finishedAt: scenarioFinishedAt.toISOString(),
      durationSeconds: Math.max(
        0,
        Math.ceil((scenarioFinishedAt.getTime() - scenarioStartedAt.getTime()) / 1000),
      ),
      sideEffects: evidence.sideEffects,
      humanEvidenceClaimed: evidence.humanEvidenceClaimed,
      commercialEvidenceClaimed: evidence.commercialEvidenceClaimed,
      screenshotEvidenceKeys: [evidenceKey],
    };
  } finally {
    await context.close();
  }
}

async function fillValidInput(page, objective = "Organizar meus produtos em uma rotina simples") {
  await page.getByLabel("Objetivo de autocuidado").fill(objective);
  const names = page.getByLabel("Nome");
  const directions = page.getByLabel("Como o rótulo orienta usar");
  await names.nth(0).fill("Limpador suave");
  await directions.nth(0).fill("Usar para limpar e enxaguar");
  await names.nth(1).fill("Hidratante diário");
  await directions.nth(1).fill("Aplicar após a limpeza para hidratar");
}

async function sessionEvent(sessionToken, eventType) {
  return api("/events", {
    method: "POST",
    headers: { "X-Mira-Session": sessionToken },
    body: JSON.stringify({ eventType }),
  });
}

async function api(path, init = {}) {
  const response = await fetch(new URL(`/api/pde/mira/private/v1${path}`, input.sourceUrl), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Internal-Token": internalToken,
      ...(init.headers || {}),
    },
  });
  const body = await response.json();
  if (!response.ok) throw new Error(body.error || `API PDE respondeu ${response.status}.`);
  return body;
}

function profileFor(scenarioCode) {
  if (scenarioCode === "ADHERENT") return "DESKTOP_1440";
  if (scenarioCode === "RECOVERY") return "IPHONE_15_PRO";
  if (scenarioCode === "SAFETY") return "PIXEL_7";
  throw new Error(`Cenário não suportado: ${scenarioCode}`);
}
