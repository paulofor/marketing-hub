import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";
import { chromium, devices } from "playwright-core";

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
if (!inputPath || !outputPath || !evidenceDirectory) {
  throw new Error(
    "Uso: node alcyone-agent-validation-harness.mjs <input> <output> <evidencias>",
  );
}
const input = JSON.parse(await readFile(inputPath, "utf8"));
const internalToken = process.env.PDE_INTERNAL_API_TOKEN?.trim();
if (!internalToken)
  throw new Error("PDE_INTERNAL_API_TOKEN não foi configurado para Alcyone.");
await mkdir(evidenceDirectory, { recursive: true });

const apiBase = "/api/pde/agent-validation/v1/products/pde-planejado-46";
const profiles = {
  DESKTOP_1440: { viewport: { width: 1440, height: 900 } },
  IPHONE_15_PRO: {
    ...devices["iPhone 15 Pro"],
    defaultBrowserType: "chromium",
  },
  PIXEL_7: { ...devices["Pixel 7"] },
};
const plans =
  input.mode === "TECHNICAL"
    ? ["ADHERENT", "RECOVERY", "SAFETY"].flatMap((scenario) =>
        Object.keys(profiles).map((profile) => [scenario, profile]),
      )
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
  sameVersion: scenarios.every(
    (scenario) => scenario.prototypeVersion === input.prototypeVersion,
  ),
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
  safetyOutcomeExplained: scenarios
    .filter((scenario) => scenario.scenarioCode === "SAFETY")
    .every((scenario) => scenario.safetyOutcomeExplained),
  accessibilityBasic: scenarios.every(
    (scenario) => scenario.accessibilityBasic,
  ),
  contrastAa: scenarios.every((scenario) => scenario.contrastAa),
  keyboardNavigation: scenarios.every(
    (scenario) => scenario.keyboardNavigation,
  ),
  focusVisible: scenarios.every((scenario) => scenario.focusVisible),
  zoom200: scenarios.every((scenario) => scenario.zoom200),
  reducedMotion: scenarios.every((scenario) => scenario.reducedMotion),
  mobileKeyboardSafeArea: scenarios.every(
    (scenario) => scenario.mobileKeyboardSafeArea,
  ),
  responsiveLayout: scenarios.every(
    (scenario) => scenario.noHorizontalOverflow,
  ),
  privacyPreserved: scenarios.every((scenario) => scenario.privacyPreserved),
  internalTrafficSegregated: scenarios.every(
    (scenario) =>
      scenario.trafficClass === "AGENT_VALIDATION" && scenario.mhInternalTest,
  ),
  paymentDisabled: scenarios.every(
    (scenario) => scenario.sideEffects.paymentEnabled === false,
  ),
  publicationDisabled: scenarios.every(
    (scenario) => scenario.sideEffects.published === false,
  ),
  campaignDisabled: scenarios.every(
    (scenario) => scenario.sideEffects.campaignCreated === false,
  ),
  zeroMediaSpend: scenarios.every(
    (scenario) => scenario.sideEffects.mediaSpendBrl === 0,
  ),
  staticFixturesValid: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.staticFixturesValid),
  providerCallsZero: scenarios.every(
    (scenario) => scenario.providerCalls === 0,
  ),
  nineScenarioDeviceGates: input.mode !== "TECHNICAL" || scenarios.length === 9,
  versionedPolicyAcknowledged: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.versionedPolicyAcknowledged),
  credentialRotated: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.credentialRotated),
  expiredSessionRejected: scenarios
    .filter((scenario) => scenario.scenarioCode === "RECOVERY")
    .every((scenario) => scenario.expiredSessionRejected),
  crossSessionPackageDenied: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.crossSessionPackageDenied),
  resultUnavailableRecovered: scenarios
    .filter((scenario) => scenario.scenarioCode === "RECOVERY")
    .every((scenario) => scenario.resultUnavailableRecovered),
  authenticatedReturn: scenarios
    .filter((scenario) => scenario.scenarioCode !== "SAFETY")
    .every((scenario) => scenario.authenticatedReturn),
  consentBeforeInput: scenarios.every(
    (scenario) => scenario.consentBeforeInput,
  ),
  canonicalSignalsOnly: scenarios.every(
    (scenario) => scenario.canonicalSignalsOnly,
  ),
  nullableMilestonesPreserved: scenarios.every(
    (scenario) => scenario.nullableMilestonesPreserved,
  ),
  sixRecoveryStates:
    input.mode !== "TECHNICAL" ||
    scenarios
      .filter((scenario) => scenario.scenarioCode === "RECOVERY")
      .every(
        (scenario) =>
          scenario.errorStates.length === 6 &&
          scenario.errorStates.every((state) => state.proved),
      ),
};
const approved =
  scenarios.every((scenario) => scenario.status === "PASS") &&
  Object.values(checks).every(Boolean);
await writeFile(
  outputPath,
  JSON.stringify({
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
  }),
  "utf8",
);

/** Executa um cenário em um dispositivo com sessão e evidência exclusivas. */
async function executeScenario(scenarioCode, deviceProfile) {
  const scenarioStartedAt = new Date();
  const contract = await publicApi("/contract");
  if (
    contract.instrumentationEvents?.join("|") !==
    "EXPERIENCE_STARTED|VALUE_MOMENT|READY_RESULT_USED|PREFERRED_OVER_FREE|CHECKOUT_STARTED"
  ) {
    throw new Error(
      "O contrato Alcyone não expõe exatamente os cinco sinais canônicos.",
    );
  }
  const session = await internalApi("/internal/sessions", {
    method: "POST",
    body: JSON.stringify({
      sourceReference: input.sourceReference,
      scenarioCode,
    }),
  });
  if (!session.mhInternalTest || session.trafficClass !== "AGENT_VALIDATION") {
    throw new Error("A API não abriu uma sessão Alcyone segregada.");
  }
  const context = await browser.newContext({
    ...profiles[deviceProfile],
    locale: "pt-BR",
    timezoneId: "UTC",
    reducedMotion: "reduce",
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
  let safetyOutcomeExplained = scenarioCode !== "SAFETY";
  let staticFixturesValid = scenarioCode === "SAFETY";
  let resultReadyAt = null;
  let versionedPolicyAcknowledged = scenarioCode === "SAFETY";
  let credentialRotated = scenarioCode === "SAFETY";
  let expiredSessionRejected = scenarioCode !== "RECOVERY";
  let crossSessionPackageDenied = scenarioCode === "SAFETY";
  let resultUnavailableRecovered = scenarioCode !== "RECOVERY";
  let authenticatedReturn = scenarioCode === "SAFETY";
  let consentBeforeInput = false;
  let canonicalSignalsOnly = false;
  let nullableMilestonesPreserved = false;
  let accessibility = null;
  const errorStates = [];
  const observedCredentials = [session.sessionToken];
  try {
    if (scenarioCode === "RECOVERY") {
      const invalidAccess = await agentApiResult(
        "/session",
        "invalid-agent-session-token",
      );
      errorStates.push(
        errorProof(contract, "ACCESS_INVALID", invalidAccess.status === 403),
      );
    }
    await page.goto(input.sourceUrl, {
      waitUntil: "domcontentloaded",
      timeout: 45_000,
    });
    await page.getByTestId("agent-validation-mode").waitFor();
    await page.getByTestId("intake-consent-step").waitFor();
    consentBeforeInput =
      (await page
        .getByRole("heading", { name: "Conte o mínimo necessário" })
        .count()) === 0 && !session.consentedAt;
    await page.getByRole("checkbox").check();
    await page
      .getByRole("button", { name: "Autorizar entrada sintética" })
      .click();
    await page
      .getByRole("heading", { name: "Conte o mínimo necessário" })
      .waitFor();
    accessibility = await validateAccessibility(page, deviceProfile);
    if (scenarioCode === "RECOVERY") {
      const incompleteInput = await agentApiResult(
        "/input",
        session.sessionToken,
        {
          method: "PUT",
          body: JSON.stringify({
            occasion: "",
            eventDate: "2026-12-15",
            preferences: [],
            constraints: [],
            pieceReferences: [],
          }),
        },
      );
      errorStates.push(
        errorProof(
          contract,
          "INPUT_INCOMPLETE",
          incompleteInput.status === 400,
        ),
      );
    }
    await fillInput(page, scenarioCode === "SAFETY");
    await page.getByRole("button", { name: "Salvar entrada segura" }).click();
    if (scenarioCode === "RECOVERY") {
      await page.route(
        `**${apiBase}/generate`,
        (route) => route.abort("failed"),
        { times: 1 },
      );
      await page
        .getByRole("button", { name: "Gerar três combinações estáticas" })
        .click();
      const harnessAlert = page.getByRole("alert");
      await harnessAlert.waitFor();
      errorStates.push(
        errorProof(
          contract,
          "HARNESS_FAILURE",
          (await harnessAlert.textContent())?.includes("HARNESS_FAILURE"),
        ),
      );
      await page.reload({ waitUntil: "domcontentloaded" });
      resumed = true;
      if (
        (await page.getByLabel("Ocasião", { exact: true }).inputValue()) !==
        "Jantar de formatura"
      ) {
        throw new Error("A retomada não preservou a entrada aceita.");
      }
    }
    await page
      .getByRole("button", { name: "Gerar três combinações estáticas" })
      .click();
    if (scenarioCode === "SAFETY") {
      await page
        .getByRole("heading", { name: "Este pedido ficou fora do protótipo" })
        .waitFor();
      if (await page.locator(".alcyone-look-grid").count()) {
        throw new Error("O cenário de segurança apresentou um pacote visual.");
      }
      await page
        .getByRole("button", { name: "Concluir cenário de segurança" })
        .click();
      safetyBlocked = true;
    } else {
      await page
        .getByRole("heading", { name: "Três combinações para sua ocasião" })
        .waitFor();
      resultReadyAt = new Date();
      staticFixturesValid = await validateFixtures(page);
      await page.getByLabel("O que tornou o pacote útil?").waitFor();
      await page
        .getByLabel("O que tornou o pacote útil?")
        .fill("As três opções reduziram a dúvida e são aplicáveis à ocasião.");
      await page
        .getByRole("button", { name: "Este pacote resolve minha decisão" })
        .click();
      await page.getByRole("button", { name: "Escolher combinação 1" }).click();
      await page
        .getByLabel("Por que vale preservar esta decisão?")
        .fill("Quero retornar ao mesmo pacote sem refazer a decisão.");
      await page
        .getByRole("button", { name: "Quero preservar esta decisão" })
        .click();
      await page.getByRole("checkbox").check();
      await page
        .getByRole("button", { name: "Criar acesso de retorno" })
        .click();
      await page.getByTestId("authenticated-return-step").waitFor();
      const beforeReturn = await page.evaluate(() => ({
        sessionToken: window.sessionStorage.getItem(
          "alcyone-agent-validation-session",
        ),
        continuity: JSON.parse(
          window.localStorage.getItem(
            "alcyone-agent-validation-continuity-v1",
          ) || "{}",
        ),
      }));
      if (
        !beforeReturn.sessionToken ||
        !beforeReturn.continuity.credential ||
        !beforeReturn.continuity.resultPackageId ||
        beforeReturn.continuity.policyVersion !== "ALCYONE_AGENT_CONTINUITY_V1"
      ) {
        throw new Error(
          "A credencial versionada de continuidade não foi criada.",
        );
      }
      observedCredentials.push(beforeReturn.continuity.credential);
      versionedPolicyAcknowledged = true;

      const outsider = await internalApi("/internal/sessions", {
        method: "POST",
        body: JSON.stringify({
          sourceReference: input.sourceReference,
          scenarioCode: "ADHERENT",
        }),
      });
      const forbiddenPackageStatus = await agentApiStatus(
        `/packages/${beforeReturn.continuity.resultPackageId}`,
        outsider.sessionToken,
      );
      crossSessionPackageDenied = forbiddenPackageStatus === 403;
      if (!crossSessionPackageDenied) {
        throw new Error(
          "Uma sessão diferente conseguiu acessar o pacote preservado.",
        );
      }

      if (scenarioCode === "RECOVERY") {
        const failedResume = await publicApiResult("/continuity/resume", {
          method: "POST",
          body: JSON.stringify({
            continuationCredential: "invalid-continuation-credential",
            policyVersion: "ALCYONE_AGENT_CONTINUITY_V1",
          }),
        });
        errorStates.push(
          errorProof(contract, "RESUME_FAILED", failedResume.status === 403),
        );
        await internalApi("/internal/session-expiration", {
          method: "POST",
          body: JSON.stringify({ sessionToken: beforeReturn.sessionToken }),
        });
        const expiredSession = await agentApiResult(
          "/session",
          beforeReturn.sessionToken,
        );
        expiredSessionRejected = expiredSession.status === 403;
        errorStates.push(
          errorProof(contract, "SESSION_EXPIRED", expiredSessionRejected),
        );
        if (!expiredSessionRejected)
          throw new Error("A sessão expirada continuou autorizada.");
        await page.route(
          `**${apiBase}/packages/*`,
          (route) => route.abort("failed"),
          { times: 1 },
        );
      }

      await page
        .getByRole("button", { name: "Retomar pacote autenticado" })
        .click();
      if (scenarioCode === "RECOVERY") {
        const unavailableAlert = page.getByRole("alert");
        await unavailableAlert.waitFor();
        errorStates.push(
          errorProof(
            contract,
            "RESULT_UNAVAILABLE",
            (await unavailableAlert.textContent())?.includes(
              "RESULT_UNAVAILABLE",
            ),
          ),
        );
        await page
          .getByRole("button", { name: "Retomar pacote autenticado" })
          .click();
        resultUnavailableRecovered = true;
      }
      await page.getByTestId("return-complete-step").waitFor();
      const afterReturn = await page.evaluate(() => ({
        sessionToken: window.sessionStorage.getItem(
          "alcyone-agent-validation-session",
        ),
        continuity: JSON.parse(
          window.localStorage.getItem(
            "alcyone-agent-validation-continuity-v1",
          ) || "{}",
        ),
      }));
      observedCredentials.push(
        afterReturn.sessionToken,
        afterReturn.continuity.credential,
      );
      credentialRotated = Boolean(
        afterReturn.sessionToken &&
        afterReturn.sessionToken !== beforeReturn.sessionToken &&
        afterReturn.continuity.credential &&
        afterReturn.continuity.credential !==
          beforeReturn.continuity.credential,
      );
      authenticatedReturn =
        credentialRotated &&
        (await agentApiStatus("/session", beforeReturn.sessionToken)) === 403;
      if (!authenticatedReturn) {
        throw new Error(
          "O retorno não rotacionou e invalidou as credenciais anteriores.",
        );
      }
      await page
        .getByLabel("Por que prefere este pacote à pesquisa gratuita?")
        .fill("É mais direto e coerente do que pesquisar referências soltas.");
      await page
        .getByRole("button", { name: "Prefiro isto à pesquisa gratuita" })
        .click();
      await page
        .getByRole("button", { name: "Abrir checkout simulado de R$ 79" })
        .click();
      await page
        .getByRole("heading", {
          name: "Simulação concluída — nenhuma cobrança realizada",
        })
        .waitFor();
      if (scenarioCode === "RECOVERY") {
        recovered = true;
      }
      await page
        .getByRole("button", { name: "Concluir cenário interno" })
        .click();
    }
    await page
      .getByRole("heading", {
        name:
          scenarioCode === "SAFETY"
            ? "Pedido bloqueado com segurança"
            : "Homologação concluída",
      })
      .waitFor();
    const dimensions = await page.evaluate(() => ({
      width: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
      lang: document.documentElement.lang,
      url: window.location.href,
      text: document.body.innerText,
      controlsNamed: Array.from(
        document.querySelectorAll("input, textarea, button, a[href]"),
      )
        .filter((control) => control.getAttribute("type") !== "hidden")
        .every((control) =>
          Boolean(
            control.getAttribute("aria-label")?.trim() ||
            Array.from(control.labels || []).some((label) =>
              label.textContent?.trim(),
            ) ||
            control.textContent?.trim(),
          ),
        ),
      touchTargets: Array.from(document.querySelectorAll("button")).every(
        (button) => button.getBoundingClientRect().height >= 44,
      ),
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
      pageHeightPx: await page.evaluate(
        () => document.documentElement.scrollHeight,
      ),
      scrollY: 0,
      sourceUrl: input.sourceUrl,
      finalUrl: dimensions.url,
      capturedAt: new Date().toISOString(),
      localPath: screenshotPath,
    });
    const evidence = await internalApi(
      `/internal/evidence/${session.evidenceId}`,
    );
    if (
      !evidence.finished ||
      evidence.providerCalls !== 0 ||
      evidence.providerCostUsd !== 0
    ) {
      throw new Error(
        "A evidência não comprovou encerramento e custo externo zero.",
      );
    }
    if (scenarioCode === "SAFETY") {
      const outcome = evidence.safetyOutcome;
      safetyOutcomeExplained = Boolean(
        outcome &&
          outcome.code === "OUT_OF_SCOPE" &&
          outcome.reason === evidence.blocker &&
          outcome.reason &&
          outcome.noResultMessage ===
            "Nenhuma combinação foi criada e nenhuma chamada externa aconteceu." &&
          outcome.safeAction?.includes("Inicie uma nova execução") &&
          outcome.safeAction?.includes("referências isoladas") &&
          outcome.resultGenerated === false &&
          outcome.providerCalled === false &&
          dimensions.text.includes(outcome.reason) &&
          dimensions.text.includes(outcome.noResultMessage) &&
          dimensions.text.includes(outcome.safeAction) &&
          !dimensions.text.includes("Homologação concluída"),
      );
    }
    consentBeforeInput =
      consentBeforeInput &&
      evidence.consentVersion === "ALCYONE_AGENT_INTAKE_CONSENT_V1" &&
      Boolean(evidence.consentedAt) &&
      Boolean(evidence.milestones.inputAcceptedAt) &&
      new Date(evidence.consentedAt) <=
        new Date(evidence.milestones.inputAcceptedAt);
    const expectedEvents =
      scenarioCode === "SAFETY"
        ? ["EXPERIENCE_STARTED"]
        : [
            "EXPERIENCE_STARTED",
            "VALUE_MOMENT",
            "READY_RESULT_USED",
            "PREFERRED_OVER_FREE",
            "CHECKOUT_STARTED",
          ];
    if (
      JSON.stringify([...evidence.events].sort()) !==
      JSON.stringify([...expectedEvents].sort())
    ) {
      throw new Error(
        "A trilha persistida diverge dos eventos realmente executados.",
      );
    }
    canonicalSignalsOnly =
      evidence.eventAudit.length === expectedEvents.length &&
      evidence.eventAudit.every((event) =>
        contract.instrumentationEvents.includes(event.eventType),
      );
    nullableMilestonesPreserved =
      scenarioCode === "SAFETY"
        ? Boolean(
            evidence.consentedAt &&
            evidence.milestones.inputAcceptedAt &&
            evidence.milestones.safetyBlockedAt &&
            evidence.milestones.finishedAt &&
            !evidence.milestones.resultReadyAt &&
            !evidence.milestones.resultPresentedAt &&
            !evidence.milestones.saveInterestAt &&
            !evidence.milestones.continuityCredentialCreatedAt &&
            !evidence.milestones.accessAuthenticatedAt &&
            !evidence.milestones.accessCompletedAt &&
            !evidence.milestones.returnedAt,
          )
        : Boolean(
            evidence.consentedAt &&
            evidence.milestones.inputAcceptedAt &&
            evidence.milestones.resultReadyAt &&
            evidence.milestones.resultPresentedAt &&
            evidence.milestones.saveInterestAt &&
            evidence.milestones.continuityCredentialCreatedAt &&
            evidence.milestones.accessAuthenticatedAt &&
            evidence.milestones.accessCompletedAt &&
            evidence.milestones.returnedAt &&
            evidence.milestones.finishedAt,
          );
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
      safetyOutcomeExplained,
      safetyOutcome:
        scenarioCode === "SAFETY" ? evidence.safetyOutcome : null,
      staticFixturesValid,
      versionedPolicyAcknowledged:
        versionedPolicyAcknowledged &&
        evidence.continuityPolicyVersion === "ALCYONE_AGENT_CONTINUITY_V1" &&
        Boolean(evidence.policyAcknowledgedAt) &&
        evidence.credentialStoredAsHash === true,
      credentialRotated,
      expiredSessionRejected,
      crossSessionPackageDenied,
      resultUnavailableRecovered,
      authenticatedReturn,
      consentBeforeInput,
      canonicalSignalsOnly,
      nullableMilestonesPreserved,
      errorStates,
      providerCalls: evidence.providerCalls,
      resultReadySeconds: resultReadyAt
        ? Math.max(0, Math.ceil((resultReadyAt - scenarioStartedAt) / 1000))
        : 0,
      accessibilityBasic:
        dimensions.lang === "pt-BR" &&
        dimensions.controlsNamed &&
        dimensions.touchTargets,
      contrastAa: accessibility.contrastAa,
      keyboardNavigation: accessibility.keyboardNavigation,
      focusVisible: accessibility.focusVisible,
      zoom200: accessibility.zoom200,
      reducedMotion: accessibility.reducedMotion,
      mobileKeyboardSafeArea: accessibility.mobileKeyboardSafeArea,
      noHorizontalOverflow: dimensions.width <= dimensions.viewport + 1,
      privacyPreserved: observedCredentials
        .filter(Boolean)
        .every(
          (credential) =>
            !dimensions.url.includes(credential) &&
            !dimensions.text.includes(credential),
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

/** Preenche somente referências sintéticas e ativa o limite proibido em SAFETY. */
async function fillInput(page, unsafe) {
  await page.getByLabel("Ocasião", { exact: true }).fill("Jantar de formatura");
  await page.getByLabel("Data da ocasião").fill("2026-12-15");
  await page
    .getByLabel("Preferências")
    .fill(unsafe ? "quero enviar foto corporal" : "linhas simples, tons frios");
  await page
    .getByLabel("Restrições práticas")
    .fill(
      unsafe ? "quero comprar uma roupa nova" : "clima ameno, sem salto alto",
    );
  await page
    .getByLabel("Referências isoladas das peças")
    .fill(
      "piece-fixture-01\npiece-fixture-02\npiece-fixture-03\npiece-fixture-04",
    );
}

/** Valida assinatura, hash, dimensões e carregamento das três fixtures geradas por código. */
async function validateFixtures(page) {
  const manifestResponse = await fetch(
    new URL("/assets/alcyone/manifest.json", input.sourceUrl),
  );
  if (!manifestResponse.ok) return false;
  const manifest = await manifestResponse.json();
  if (
    manifest.contractVersion !== "PDE_STATIC_RESULT_FIXTURES_V1" ||
    manifest.prototypeVersion !== input.prototypeVersion ||
    manifest.providerCalls !== 0 ||
    manifest.artifacts?.length !== 3
  )
    return false;
  for (const artifact of manifest.artifacts) {
    const response = await fetch(new URL(artifact.path, input.sourceUrl));
    if (!response.ok) return false;
    const bytes = Buffer.from(await response.arrayBuffer());
    if (
      createHash("sha256").update(bytes).digest("hex") !== artifact.sha256 ||
      bytes.subarray(0, 8).toString("hex") !== "89504e470d0a1a0a" ||
      bytes.readUInt32BE(16) !== 1024 ||
      bytes.readUInt32BE(20) !== 1024
    )
      return false;
  }
  return page
    .locator(".alcyone-look-grid img")
    .evaluateAll(
      (images) =>
        images.length === 3 &&
        images.every(
          (image) =>
            image.complete &&
            image.naturalWidth === 1024 &&
            image.naturalHeight === 1024,
        ),
    );
}

/** Comprova contraste, teclado, foco, zoom, redução de movimento e área útil móvel. */
async function validateAccessibility(page, deviceProfile) {
  await page.keyboard.press("Tab");
  const initialFocus = await page.evaluate(() => {
    const active = document.activeElement;
    const style = active ? getComputedStyle(active) : null;
    return {
      tag: active?.tagName || "",
      outlineWidth: Number.parseFloat(style?.outlineWidth || "0"),
      outlineStyle: style?.outlineStyle || "none",
    };
  });
  const reached = new Set();
  for (let step = 0; step < 8; step += 1) {
    await page.keyboard.press("Tab");
    reached.add(
      await page.evaluate(() => {
        const active = document.activeElement;
        const label = Array.from(active?.labels || [])
          .map((item) => item.textContent?.trim())
          .filter(Boolean)
          .join("|");
        return `${active?.tagName || ""}:${active?.getAttribute("type") || ""}:${label || active?.textContent?.trim() || active?.getAttribute("name") || ""}`;
      }),
    );
  }
  const visual = await page.evaluate(() => {
    const parse = (value) => {
      const match = value.match(/[\d.]+/g)?.map(Number) || [];
      return match.length >= 3 ? match.slice(0, 3) : [255, 255, 255];
    };
    const luminance = ([red, green, blue]) => {
      const channels = [red, green, blue].map((channel) => {
        const normalized = channel / 255;
        return normalized <= 0.03928
          ? normalized / 12.92
          : ((normalized + 0.055) / 1.055) ** 2.4;
      });
      return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
    };
    const ratio = (foreground, background) => {
      const lighter = Math.max(luminance(foreground), luminance(background));
      const darker = Math.min(luminance(foreground), luminance(background));
      return (lighter + 0.05) / (darker + 0.05);
    };
    const backgroundOf = (element) => {
      let current = element;
      while (current) {
        const color = getComputedStyle(current).backgroundColor;
        if (color && color !== "rgba(0, 0, 0, 0)" && color !== "transparent") {
          return parse(color);
        }
        current = current.parentElement;
      }
      return [235, 231, 223];
    };
    const candidates = Array.from(
      document.querySelectorAll(
        "h1, h2, h3, p, label, button, small, dt, dd, .alcyone-kicker, .alcyone-status span, .alcyone-status strong",
      ),
    ).filter((element) => {
      const style = getComputedStyle(element);
      return (
        element.textContent?.trim() &&
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        element.getBoundingClientRect().width > 0
      );
    });
    const contrastAa = candidates.every((element) => {
      const style = getComputedStyle(element);
      const fontSize = Number.parseFloat(style.fontSize);
      const fontWeight = Number.parseInt(style.fontWeight, 10) || 400;
      const large = fontSize >= 24 || (fontSize >= 18.66 && fontWeight >= 700);
      return (
        ratio(parse(style.color), backgroundOf(element)) >= (large ? 3 : 4.5)
      );
    });
    const reducedMotion =
      matchMedia("(prefers-reduced-motion: reduce)").matches &&
      Array.from(document.querySelectorAll("button")).every(
        (element) => getComputedStyle(element).transitionDuration === "0s",
      );
    return { contrastAa, reducedMotion };
  });

  await page.evaluate(() => {
    document.documentElement.style.fontSize = "200%";
  });
  const zoom200 = await page.evaluate(
    () => document.documentElement.scrollWidth <= window.innerWidth + 1,
  );
  await page.evaluate(() => {
    document.documentElement.style.fontSize = "";
  });

  let mobileKeyboardSafeArea = true;
  if (deviceProfile !== "DESKTOP_1440") {
    const original = page.viewportSize();
    await page.setViewportSize({
      width: original.width,
      height: Math.max(360, Math.floor(original.height * 0.58)),
    });
    const input = page.getByLabel("Referências isoladas das peças");
    await input.focus();
    await input.evaluate((element) =>
      element.scrollIntoView({ block: "center" }),
    );
    mobileKeyboardSafeArea = await input.evaluate((element) => {
      const bounds = element.getBoundingClientRect();
      return bounds.top >= 0 && bounds.bottom <= window.innerHeight;
    });
    await page.setViewportSize(original);
  }

  return {
    contrastAa: visual.contrastAa,
    keyboardNavigation: reached.size >= 4,
    focusVisible:
      initialFocus.tag !== "BODY" &&
      initialFocus.outlineStyle !== "none" &&
      initialFocus.outlineWidth >= 2,
    zoom200,
    reducedMotion: visual.reducedMotion,
    mobileKeyboardSafeArea,
  };
}

/** Constrói a prova de um estado previsto usando mensagem e recuperação do contrato. */
function errorProof(contract, code, observed) {
  const definition = contract.errorStates?.find((state) => state.code === code);
  return {
    code,
    cause: `${code} reproduzido de forma determinística pelo harness`,
    participantMessage: definition?.message || "",
    recoveryAction: definition?.recoveryAction || "",
    proved: Boolean(
      observed &&
      definition?.message?.trim() &&
      definition?.recoveryAction?.trim(),
    ),
  };
}

/** Chama uma rota pública da superfície e exige JSON bem-sucedido. */
async function publicApi(path, init = {}) {
  const result = await publicApiResult(path, init);
  if (result.status < 200 || result.status >= 300) {
    throw new Error(`Alcyone harness ${path}: HTTP ${result.status}`);
  }
  return result.body;
}

/** Retorna status e JSON de uma rota pública para comprovar recusas esperadas. */
async function publicApiResult(path, init = {}) {
  const response = await fetch(new URL(`${apiBase}${path}`, input.sourceUrl), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init.headers || {}),
    },
    signal: AbortSignal.timeout(30_000),
  });
  let body = {};
  try {
    body = await response.json();
  } catch {
    body = {};
  }
  return { status: response.status, body };
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
  if (!response.ok)
    throw new Error(`Alcyone harness ${path}: HTTP ${response.status}`);
  return response.json();
}

/** Consulta uma rota de sessão sem lançar erro para permitir comprovar negações 403. */
async function agentApiStatus(path, sessionToken) {
  const response = await fetch(new URL(`${apiBase}${path}`, input.sourceUrl), {
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Agent-Session": sessionToken,
    },
    signal: AbortSignal.timeout(30_000),
  });
  return response.status;
}

/** Consulta status e corpo de uma operação de sessão sem transformar recusa em exceção. */
async function agentApiResult(path, sessionToken, init = {}) {
  const response = await fetch(new URL(`${apiBase}${path}`, input.sourceUrl), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Agent-Session": sessionToken,
      ...(init.headers || {}),
    },
    signal: AbortSignal.timeout(30_000),
  });
  let body = {};
  try {
    body = await response.json();
  } catch {
    body = {};
  }
  return { status: response.status, body };
}

/** Seleciona o dispositivo canônico para uma revisão isolada de Psique. */
function profileFor(scenarioCode) {
  if (scenarioCode === "ADHERENT") return "DESKTOP_1440";
  if (scenarioCode === "RECOVERY") return "IPHONE_15_PRO";
  if (scenarioCode === "SAFETY") return "PIXEL_7";
  throw new Error(`Cenário não suportado: ${scenarioCode}`);
}
