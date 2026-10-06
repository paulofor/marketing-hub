import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { chromium, devices, expect } from "@playwright/test";

const base = "http://127.0.0.1:15173";
const backend = "http://127.0.0.1:18091";
const api = "/api/business-process-chains/learning-cycles/v1";
const internal =
  "/api/internal/business-process-chains/learning-cycles/v1/decision/stage-executions";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/capella-autonomy/browser";
await mkdir(output, { recursive: true });
async function request(path, body, method = "POST") {
  const result = await fetch(backend + path, {
    method: body === undefined ? "GET" : method,
    headers: {
      "Content-Type": "application/json",
      "X-Learning-Decision-Contract": "LEARNING_CYCLE_DECISION_PROPOSAL_V2",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await result.text();
  assert(result.ok, `${result.status} ${text}`);
  return text ? JSON.parse(text) : null;
}
const schema = JSON.parse(
  await readFile(
    "experiment-strategist-worker/src/main/resources/prompts/learning-cycle/v2/decision-schema.json",
    "utf8",
  ),
);
async function seed(approved = false, inconclusive = false) {
  await request("/fixture/reset", {});
  await request("/fixture/experiments/91001/legacy-publication", {});
  const cycle = await request(`${api}/products/91001`, {
    requestKey: crypto.randomUUID(),
    chainDefinitionId: 91002,
    experimentId: 91001,
    baseline: true,
    productVersion: "fixture-v1",
    hypothesis: "Valor claro facilita compra",
    mainChange: "Clareza",
    successCriterion: "Contribuição após aquisição",
    audience: "Profissionais locais",
    offer: "Kit sintético",
    acquisition: "Canal local",
    budgetLimitBrl: 100,
    windowStart: new Date(Date.now() - 86400000).toISOString(),
    windowEnd: new Date(Date.now() + 86400000).toISOString(),
    sampleTarget: 10,
    minimumNetSales: 5,
    operatorName: "Fixture segregada",
  });
  const [job] = await request(`${internal}/pending`);
  await request(
    `${internal}/${job.proposalId}/request`,
    {
      leaseToken: job.leaseToken,
      prompt: "Simulação local sem modelo externo",
      schema,
      model: "fixture-no-external-model",
      serviceTier: "flex",
      serviceTierReason: null,
    },
    "PUT",
  );
  const target = job.context.returnTargets.find(
    (t) => t.activityId === "rework",
  );
  const proposal = Object.fromEntries(
    [
      "summary",
      "rootCause",
      "learning",
      "nextHypothesis",
      "evidenceLimits",
      "correctionPlan",
      "scaleHypothesis",
    ].map((key) => [
      key,
      "Hipótese sintética: amostra insuficiente, sem prova de mercado.",
    ]),
  );
  Object.assign(proposal, {
    contractVersion: job.context.contractVersion,
    action: inconclusive ? "INCONCLUSIVE" : "ADJUST",
    returnProcessId: inconclusive ? null : target.processDefinitionId,
    returnActivityId: inconclusive ? null : target.activityId,
    selectedAlternative: 0,
    evidenceEventIds: [job.context.measurementEventId],
    alternatives: ["KEEP_FOCUS", "ADJACENT_SEGMENTS", "BROAD_PROBLEM"].map(
      (marketScope) => ({
        marketScope,
        option: marketScope,
        benefit: "Valor verificável",
        risk: "Amostra limitada",
        effort: "Baixo",
        salesImpact: "Hipótese a medir",
      }),
    ),
    marketReview: {
      recommendedScope: "KEEP_FOCUS",
      currentAudience: "Profissionais",
      proposedAudience: "Mesmo foco",
      sharedProblem: "Esforço",
      deliveryReadiness: "UNKNOWN",
      requiredAdaptations: "Comprovar entrega",
      excludedAudiences: "Outros públicos",
      evidenceLimits: "Dados sintéticos",
      primaryMetric: "NET_CONTRIBUTION_AFTER_ACQUISITION",
      continueWhen: "Venda e contribuição",
      adjustWhen: "Falha demonstrada",
      stopWhen: "Limite atingido",
      requiresNewCycle: true,
    },
  });
  await request(`${internal}/${job.proposalId}/result`, {
    leaseToken: job.leaseToken,
    rawResponse: JSON.stringify(proposal),
    error: null,
    costUsd: null,
  });
  if (approved) {
    await request(`${api}/products/91001/${cycle.id}/commands`, {
      requestKey: crypto.randomUUID(),
      expectedRevision: cycle.revision,
      action: inconclusive ? "INCONCLUSIVE" : "ADJUST",
      operatorName: "Aprovação sintética anterior",
      summary: "Decisão anterior preservada",
      evidenceReference: "internal://fixture/decision",
      evidence: inconclusive
        ? { decisionProposalId: job.proposalId, humanApproved: true }
        : {
            ...proposal,
            decisionProposalId: job.proposalId,
            humanApproved: true,
          },
    });
  }
  return cycle;
}
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const cycle = await seed(name !== "desktop", name === "pixel");
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const errors = [],
      writes = [],
      external = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.origin);
        return route.abort();
      }
      if (route.request().method() === "POST") writes.push(url.pathname);
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(api) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      return route.continue();
    });
    await page.goto(
      `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`,
    );
    const button = page.getByRole("button", {
      name: "Preparar continuidade sem gasto",
    });
    await expect(button).toBeVisible();
    assert.equal(writes.length, 0);
    await expect(page.getByLabel("Responsável pela decisão *")).toHaveCount(0);
    const response = page.waitForResponse(
      (r) =>
        r.url().endsWith("/prepare-successor") &&
        r.request().method() === "POST",
    );
    await button.click();
    const received = await response;
    assert.equal(received.status(), 200, await received.text());
    const prepared = await received.json();
    assert.equal(prepared.stage, "PLANNING");
    assert.equal(prepared.previousCycleId, cycle.id);
    await expect(page).toHaveURL(new RegExp(`cycleId=${prepared.id}`));
    await expect(
      page.getByText("Primeira janela comercial do sucessor", { exact: true }),
    ).toBeVisible();
    await expect(page.locator('[name="startDate"]')).toHaveValue("");
    await expect(
      page.getByRole("link", { name: "Abrir atividade orientada" }),
    ).toBeVisible();
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 2,
      ),
      false,
    );
    await page.screenshot({
      path: `${output}/${name}-successor.png`,
      fullPage: true,
    });
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    assert.equal(writes.length, 1);
    await page.reload();
    assert.equal(writes.length, 1);
    await context.close();
    console.log(
      `PASS ${name}: leitura sem efeito, preparação única, planejamento e janela ausente`,
    );
  }
} finally {
  await browser.close();
}
