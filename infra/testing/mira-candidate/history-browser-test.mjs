import { createRequire } from "node:module";
import { readFile, writeFile } from "node:fs/promises";
import assert from "node:assert/strict";

const require = createRequire(
  new URL("../../../customer-agent-worker/package.json", import.meta.url),
);
let library;
try {
  library = require("playwright-core");
} catch (ex) {
  if (ex.code !== "MODULE_NOT_FOUND") throw ex;
  library = require("playwright");
}
const [inputFile, reportFile] = process.argv.slice(2);
const input = JSON.parse(await readFile(inputFile, "utf8"));
const credential = process.env.PDE_INTERNAL_API_TOKEN;
if (!credential) throw new Error("Credencial local necessária.");
const api = new URL("/api/pde/mira/candidate/v1", input.sourceUrl);
const createdResponse = await fetch(api + "/internal/sessions", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-PDE-Internal-Token": credential,
  },
  body: JSON.stringify({
    cycleId: input.cycleId,
    sourceReference: input.sourceReference,
    prototypeVersion: input.prototypeVersion,
    condition: "REDUCED",
    scenarioCode: "ADHERENT",
    deviceProfile: "DESKTOP_1440",
  }),
});
assert.equal(createdResponse.status, 200);
const created = await createdResponse.json();
const browser = await library.chromium.launch({
  headless: true,
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
});
try {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
  });
  await context.addInitScript(
    (value) => sessionStorage.setItem("mira-candidate-v1-session", value),
    created.sessionToken,
  );
  const page = await context.newPage();
  await page.goto(input.sourceUrl);
  await page
    .getByLabel("Nome do produto 1 *", { exact: true })
    .fill("Produto documental de teste");
  await page
    .getByLabel("Orientação do rótulo 1 *", { exact: true })
    .fill("Limpar e enxaguar.");
  await page
    .getByRole("button", { name: "Gerar rotina segura", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "Sua rotina organizada", exact: true })
    .waitFor();
  assert.equal(
    await page.getByRole("button", { name: "Confirmar retomada" }).count(),
    0,
  );
  await page.locator(".mira-internal-controls summary").click();
  assert.equal(
    await page
      .getByRole("button", { name: "Concluir cenário", exact: true })
      .isDisabled(),
    true,
  );
  await page.locator(".mira-internal-controls summary").click();
  await page
    .getByRole("button", { name: "Consultar organização", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Consultar organização", exact: true })
    .click();
  const consultedResponse = await fetch(api + "/session", {
    headers: { "X-Mira-Session": created.sessionToken },
  });
  const consulted = await consultedResponse.json();
  assert.equal(consulted.organizationsUsed, 1);
  assert.equal(
    consulted.events.filter((event) => event === "READY_RESULT_USED").length,
    1,
  );
  assert.ok(
    (await page.locator(".mira-source").innerText()).includes(
      "texto do rótulo",
    ),
  );
  await page
    .getByRole("button", { name: "Preparar segunda organização" })
    .click();
  await page
    .getByLabel("O que você quer organizar? *", { exact: true })
    .fill("Diagnosticar manchas");
  await page
    .getByRole("button", { name: "Gerar rotina segura", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "Como seguir com segurança" })
    .waitFor();
  await page.reload();
  const firstResult = page
    .locator("details")
    .filter({ hasText: "Consultar organização anterior 1" });
  await firstResult.locator("summary").click();
  await firstResult
    .getByRole("heading", { name: "Produto documental de teste" })
    .waitFor();
  await page
    .getByLabel("O que você quer organizar? *", { exact: true })
    .fill("Organizar meus cuidados ao acordar");
  await page
    .getByRole("button", { name: "Gerar rotina segura", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "Sua rotina organizada", exact: true })
    .waitFor();
  assert.equal(
    await page
      .getByRole("button", { name: "Preparar segunda organização" })
      .count(),
    0,
  );
  await page.reload();
  await page
    .getByRole("heading", { name: "Sua rotina organizada", exact: true })
    .waitFor();
  const response = await fetch(api + "/session", {
    headers: { "X-Mira-Session": created.sessionToken },
  });
  const session = await response.json();
  assert.equal(session.organizationsUsed, 2);
  assert.equal(session.previousResults.length, 1);
  assert.equal(
    session.previousResults[0].routine[0].productName,
    "Produto documental de teste",
  );
  await writeFile(
    reportFile,
    JSON.stringify(
      {
        status: "PASS",
        trafficClass: "AGENT_VALIDATION",
        historicalResultPreserved: true,
        consultationIdempotent: true,
        recoveryHiddenWithoutFailure: true,
        completionRequiresConsultation: true,
        blockedSecondInputConsumedOrganization: false,
        organizationsUsed: session.organizationsUsed,
        providerCalls: 0,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
