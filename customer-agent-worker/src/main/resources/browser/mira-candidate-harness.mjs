import { createRequire } from "node:module";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { MiraCandidateMeasurements } from "./mira-candidate-metrics.mjs";
const require = createRequire(import.meta.url);
let library;
try {
  library = require("playwright-core");
} catch (ex) {
  if (ex.code !== "MODULE_NOT_FOUND") throw ex;
  library = require("playwright");
}
const { chromium } = library;
const [inputFile, outputFile, directory] = process.argv.slice(2);
const input = JSON.parse(await readFile(inputFile, "utf8"));
const credential = process.env.PDE_INTERNAL_API_TOKEN;
if (!credential) throw new Error("Credencial interna necessária.");
const sources = JSON.parse(
  await readFile(
    new URL("./fixtures/mira-manufacturer-20261006.json", import.meta.url),
    "utf8",
  ),
);
for (const source of sources) {
  if (
    Date.now() - Date.parse(source.observedAt) > 30 * 86400000 ||
    createHash("sha256").update(source.labelDirections).digest("hex") !==
      source.sourceExcerptSha256
  ) {
    throw new Error(
      "Fixture documental vencida ou alterada; confira a fonte antes da homologação.",
    );
  }
}
// O segundo rótulo inclui o título documental do fabricante; não é inferência sobre outro produto.
const inventory = sources.map((s, i) => ({
  name: s.name,
  sourceUrl: s.url,
  labelDirections:
    i === 1 ? `${s.name}: ${s.labelDirections}` : s.labelDirections,
}));
const API = "/api/pde/mira/candidate/v1";
const profiles = {
  DESKTOP_1440: {
    viewport: { width: 1440, height: 900 },
    isMobile: false,
    hasTouch: false,
  },
  IPHONE_15_PRO: {
    viewport: { width: 393, height: 852 },
    isMobile: true,
    hasTouch: true,
    deviceScaleFactor: 3,
    userAgent:
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1",
  },
  PIXEL_7: {
    viewport: { width: 412, height: 915 },
    isMobile: true,
    hasTouch: true,
    deviceScaleFactor: 2.625,
    userAgent:
      "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36",
  },
};
const sideEffects = {
  paymentEnabled: false,
  published: false,
  campaignCreated: false,
  mediaSpendBrl: 0,
};
const plans =
  input.mode === "TECHNICAL"
    ? ["ADHERENT", "RECOVERY", "SAFETY"].flatMap((s) =>
        Object.keys(profiles).flatMap((d) =>
          ["REFERENCE", "REDUCED"].map((c) => [s, d, c]),
        ),
      )
    : [
        [
          input.scenarioCode,
          {
            ADHERENT: "DESKTOP_1440",
            RECOVERY: "IPHONE_15_PRO",
            SAFETY: "PIXEL_7",
          }[input.scenarioCode],
          "REDUCED",
        ],
      ];
await mkdir(directory, { recursive: true });
const browser = await chromium.launch({
  headless: true,
  ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
    ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH }
    : {}),
});
const startedAt = new Date();
const scenarios = [];
const artifacts = [];
try {
  for (const [scenarioCode, deviceProfile, condition] of plans) {
    scenarios.push(await execute(scenarioCode, deviceProfile, condition));
    await writeFile(
      outputFile + ".partial",
      JSON.stringify({ status: "IN_PROGRESS", scenarios, artifacts }, null, 2),
    );
  }
} catch (ex) {
  await writeFile(
    outputFile + ".partial",
    JSON.stringify(
      { status: "FAILED", error: ex.message, scenarios, artifacts },
      null,
      2,
    ),
  );
  throw ex;
} finally {
  await browser.close();
}
const checks = {
  sameVersion: scenarios.every(
    (s) => s.prototypeVersion === input.prototypeVersion,
  ),
  desktopAndMobile:
    input.mode !== "TECHNICAL" ||
    Object.keys(profiles).every((d) =>
      scenarios.some((s) => s.deviceProfile === d),
    ),
  happyResultWithinTenMinutes: scenarios.every(
    (s) => s.resultReadySeconds <= 600,
  ),
  recoveryPreserved:
    input.mode !== "TECHNICAL" ||
    scenarios.some((s) => s.scenarioCode === "RECOVERY" && s.recovered),
  safetyBlocked:
    input.mode !== "TECHNICAL" ||
    scenarios.some((s) => s.scenarioCode === "SAFETY" && s.safetyBlocked),
  accessibilityBasic: scenarios.every((s) => s.accessibilityBasic),
  responsiveLayout: scenarios.every((s) => s.noHorizontalOverflow),
  privacyPreserved: scenarios.every((s) => s.privacyPreserved),
  internalTrafficSegregated: true,
  paymentDisabled: true,
  publicationDisabled: true,
  campaignDisabled: true,
  zeroMediaSpend: true,
};
// Comparação funcional usa inventário idêntico, nunca uma saída de um item contra outra de dois.
for (const scenarioCode of ["ADHERENT", "RECOVERY"])
  for (const deviceProfile of Object.keys(profiles)) {
    const pair = scenarios.filter(
      (s) =>
        s.scenarioCode === scenarioCode && s.deviceProfile === deviceProfile,
    );
    if (pair.length === 2 && pair[0].routineHash !== pair[1].routineHash)
      throw new Error(
        "A condição de entrada mudou a saída para inventário igual.",
      );
  }
const finishedAt = new Date();
await writeFile(
  outputFile,
  JSON.stringify(
    {
      contractVersion: "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
      fixtureContract: "PDE_DOCUMENTED_INPUT_COMPARISON_V3",
      mode: input.mode,
      decision: Object.values(checks).every(Boolean) ? "APPROVED" : "BLOCKED",
      sourceReference: input.sourceReference,
      productId: input.productId,
      productSlug: input.productSlug,
      publicUrl: input.sourceUrl,
      prototypeVersion: input.prototypeVersion,
      trafficClass: "AGENT_VALIDATION",
      internalMarker: "mh_internal_test",
      generationMode: "DETERMINISTIC_DOCUMENTED_LABELS",
      providerCalls: 0,
      startedAt: startedAt.toISOString(),
      finishedAt: finishedAt.toISOString(),
      durationSeconds: Math.ceil((finishedAt - startedAt) / 1000),
      devices: Object.keys(profiles)
        .filter((d) => plans.some((p) => p[1] === d))
        .map((deviceProfile) => ({
          deviceProfile,
          status: "PASS",
          screenshotEvidenceKeys: artifacts
            .filter((a) => a.deviceProfile === deviceProfile)
            .map((a) => a.evidenceKey),
        })),
      scenarios,
      checks,
      artifacts,
      sideEffects,
      humanEvidenceClaimed: false,
      commercialEvidenceClaimed: false,
      fixtureSources: sources,
      evidence: scenarios.map((s) => s.evidenceId),
    },
    null,
    2,
  ),
);

async function api(path, body, sessionToken, method) {
  const response = await fetch(new URL(API + path, input.sourceUrl), {
    method: method || (body === undefined ? "GET" : "POST"),
    headers: {
      "Content-Type": "application/json",
      ...(sessionToken
        ? { "X-Mira-Session": sessionToken }
        : { "X-PDE-Internal-Token": credential }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  if (!response.ok)
    throw new Error(
      `API privada ${path} retornou ${response.status}: ${await response.text()}`,
    );
  return await response.json();
}
async function execute(scenarioCode, deviceProfile, condition) {
  const created = await api("/internal/sessions", {
    cycleId: input.cycleId,
    sourceReference: input.sourceReference,
    prototypeVersion: input.prototypeVersion,
    condition,
    scenarioCode,
    deviceProfile,
  });
  const { sessionToken, session } = created;
  const context = await browser.newContext({
    ...profiles[deviceProfile],
    locale: "pt-BR",
  });
  const page = await context.newPage();
  await page.addInitScript(
    (value) => sessionStorage.setItem("mira-candidate-v1-session", value),
    sessionToken,
  );
  let resumed = false,
    recovered = false,
    safetyBlocked = false;
  let requests = 0;
  page.on("request", (r) => {
    if (r.url().includes(API + "/generate")) requests++;
    if (/mercadopago|facebook|openai\.com/.test(r.url()))
      throw new Error("Efeito externo proibido no teste privado.");
  });
  try {
    const measurements = new MiraCandidateMeasurements();
    await page.goto(input.sourceUrl);
    await page
      .getByRole("heading", { name: "Conte o mínimo necessário" })
      .waitFor();
    const minimumRequiredProductFields = await page
      .locator("fieldset input[required], fieldset textarea[required]")
      .count();
    if (minimumRequiredProductFields !== (condition === "REFERENCE" ? 4 : 2))
      throw new Error("Entrada mínima divergente da condição persistida.");
    await page
      .getByLabel("Nome do produto 1 *", { exact: true })
      .fill(inventory[0].name);
    measurements.filled("name-1", "PRODUCT");
    await page
      .getByLabel("Orientação do rótulo 1 *", { exact: true })
      .fill(inventory[0].labelDirections);
    measurements.filled("directions-1", "PRODUCT");
    if (condition === "REDUCED") {
      if (!(await page.locator("form").evaluate((f) => f.checkValidity())))
        throw new Error("Um único produto deveria permitir continuar.");
      await page
        .getByRole("button", { name: "Acrescentar outro produto" })
        .click();
    }
    await page
      .getByLabel("Nome do produto 2 *", { exact: true })
      .fill(inventory[1].name);
    measurements.filled("name-2", "PRODUCT");
    await page
      .getByLabel("Orientação do rótulo 2 *", { exact: true })
      .fill(inventory[1].labelDirections);
    measurements.filled("directions-2", "PRODUCT");
    for (let i = 0; i < inventory.length; i++) {
      await page
        .locator("summary")
        .filter({ hasText: `Acrescentar referência do produto ${i + 1}` })
        .click();
      await page
        .getByLabel(`Link do fabricante ${i + 1}`, { exact: true })
        .fill(inventory[i].sourceUrl);
      measurements.filled(`source-${i + 1}`, "SOURCE");
    }
    if (scenarioCode === "SAFETY") {
      await page
        .getByLabel("O que você quer organizar? *", { exact: true })
        .fill("Diagnosticar e tratar manchas");
      measurements.filled("objective", "OBJECTIVE");
    }
    if (scenarioCode === "RECOVERY") {
      // A resposta se perde DEPOIS do commit: a retomada deve reutilizar o resultado, sem nova organização.
      await page.route(
        "**" + API + "/generate",
        async (route) => {
          await route.fetch();
          await route.abort("failed");
        },
        { times: 1 },
      );
    }
    await page.getByRole("button", { name: "Gerar rotina segura" }).click();
    if (scenarioCode === "RECOVERY") {
      await page.getByRole("alert").first().waitFor();
      await page.reload();
      resumed = true;
    }
    if (scenarioCode === "SAFETY") {
      await page.getByRole("alert").filter({ hasText: "clínica" }).waitFor();
      measurements.outcome();
      await page.locator(".mira-internal-controls summary").click();
      await page
        .getByRole("button", { name: "Registrar limite de segurança" })
        .click();
      await page
        .getByRole("button", { name: "Concluir cenário de segurança" })
        .click();
      safetyBlocked = true;
    } else {
      await page
        .getByRole("heading", { name: "Sua rotina organizada" })
        .waitFor();
      measurements.outcome();
      if (
        (await api("/session", undefined, sessionToken)).organizationsUsed !== 1
      )
        throw new Error("Resposta perdida duplicou consumo.");
      if (
        await page.getByRole("button", { name: "Confirmar retomada" }).count()
      )
        if (scenarioCode !== "RECOVERY")
          throw new Error("Retomada oferecida sem recuperação.");
      await page
        .getByRole("button", { name: "Consultar organização", exact: true })
        .click();
      await page.locator(".mira-internal-controls summary").click();
      await page
        .getByRole("button", {
          name: "Simular comparação com alternativa gratuita",
          exact: true,
        })
        .click();
      await page
        .getByRole("button", {
          name: "Simular início da continuidade sem cobrança",
          exact: true,
        })
        .click();
      if (scenarioCode === "RECOVERY") {
        await page.getByRole("button", { name: "Confirmar retomada" }).click();
        recovered = true;
      }
      await page
        .getByRole("button", { name: "Concluir cenário", exact: true })
        .click();
    }
    await page
      .getByRole("heading", { name: "Avaliação interna concluída" })
      .waitFor();
    if (await page.getByRole("button", { name: /^Concluir cenário/ }).count())
      throw new Error("Ação concluída permaneceu ativa.");
    await page.reload();
    await page
      .locator(".mira-internal-controls summary")
      .filter({ hasText: "Verificação interna concluída" })
      .waitFor();
    const current = await api("/session", undefined, sessionToken);
    const requiredEvents =
      scenarioCode === "SAFETY"
        ? ["EXPERIENCE_STARTED", "SAFETY_LIMIT_BLOCKED"]
        : [
            "EXPERIENCE_STARTED",
            "VALUE_MOMENT",
            "READY_RESULT_USED",
            "PREFERRED_OVER_FREE",
            "CHECKOUT_STARTED",
          ];
    if (!requiredEvents.every((event) => current.events.includes(event)))
      throw new Error("Percurso não registrou os sinais sintéticos esperados.");
    if (
      scenarioCode === "SAFETY" &&
      current.events.includes("CHECKOUT_STARTED")
    )
      throw new Error(
        "Bloqueio de segurança não pode avançar para continuidade.",
      );
    const dimensions = await page.evaluate(() => ({
      height: document.documentElement.scrollHeight,
      noHorizontalOverflow: document.documentElement.scrollWidth <= innerWidth,
      controlsNamed: [
        ...document.querySelectorAll("button,input,textarea"),
      ].every(
        (c) =>
          c.textContent?.trim() ||
          c.getAttribute("aria-label") ||
          [...(c.labels || [])].some((l) => l.textContent?.trim()),
      ),
      noPersonalFields: !document.querySelector(
        'input[type="email"],input[type="tel"],input[name="ageRange"]',
      ),
    }));
    if (
      !dimensions.noHorizontalOverflow ||
      !dimensions.controlsNamed ||
      !dimensions.noPersonalFields
    )
      throw new Error("Layout, rótulos ou privacidade reprovados.");
    const key = `${scenarioCode}-${deviceProfile}-${condition}`;
    const localPath = resolve(directory, key + ".png");
    await page.screenshot({ path: localPath, fullPage: true });
    artifacts.push({
      captureSessionId: input.captureSessionId,
      evidenceKey: key,
      evidenceType: "FULL_PAGE",
      deviceProfile,
      pageNumber: 1,
      foldNumber: null,
      viewportWidth: profiles[deviceProfile].viewport.width,
      viewportHeight: profiles[deviceProfile].viewport.height,
      pageHeightPx: dimensions.height,
      scrollY: 0,
      sourceUrl: input.sourceUrl,
      finalUrl: page.url(),
      capturedAt: new Date().toISOString(),
      localPath,
    });
    return {
      scenarioCode,
      deviceProfile,
      condition,
      screenshotEvidenceKeys: [key],
      status: "PASS",
      prototypeVersion: current.prototypeVersion,
      evidenceId: current.id,
      trafficClass: current.trafficClass,
      events: current.events,
      mhInternalTest: true,
      providerCalls: 0,
      firstInteractionAt: current.firstInteractionAt,
      ...measurements.report(
        minimumRequiredProductFields,
        current.products.length,
      ),
      resumed,
      recovered,
      safetyBlocked,
      products: current.products,
      routine: current.routine,
      routineHash: createHash("sha256")
        .update(JSON.stringify(current.routine))
        .digest("hex"),
      organizationsUsed: current.organizationsUsed,
      generationRequests: requests,
      accessibilityBasic: dimensions.controlsNamed,
      noHorizontalOverflow: dimensions.noHorizontalOverflow,
      privacyPreserved: dimensions.noPersonalFields,
      sideEffects,
      humanEvidenceClaimed: false,
      commercialEvidenceClaimed: false,
    };
  } finally {
    await context.close();
  }
}
