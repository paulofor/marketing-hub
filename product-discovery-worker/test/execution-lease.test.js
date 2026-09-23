import assert from "node:assert/strict";
import test from "node:test";
import {
  analysisAuditCallbackPayload,
  attachGapDeepeningReport,
  backendFailureMessage,
  createPollLock,
  failureCallbackPayload,
  postJson,
  researchPlanCallbackPayload,
  withExecutionLease,
} from "../src/worker.js";

test("relatório do aprofundamento agrega consultas e custo das duas tentativas", () => {
  const gap = (candidateName, publicQueries) => ({
    candidateName,
    pendingQuestion: `Pergunta de ${candidateName}`,
    appropriateSource: "Relato e oferta atual",
    evidenceNeeded: "Compra, desistência, preço e entrega",
    contraryEvidenceToSeek: "Alternativa gratuita suficiente",
    publicQueries,
  });
  const execution = {
    directedAttempts: [
      {
        attemptNumber: 1,
        directed: {
          mode: "CODEX",
          plan: {
            publicQueries: ["q1", "q2"],
            candidateGaps: [
              gap("Candidata A", ["q1"]),
              gap("Candidata B", ["q2"]),
            ],
          },
        },
      },
      {
        attemptNumber: 2,
        directed: {
          mode: "CODEX",
          plan: {
            publicQueries: ["q3", "q4"],
            candidateGaps: [
              gap("Candidata A", ["q3"]),
              gap("Candidata B", ["q4"]),
            ],
          },
        },
      },
    ],
    analysisAttempts: [
      { analysis: { mode: "CODEX" } },
      { analysis: { mode: "CODEX" } },
    ],
    report: {
      opportunities: [
        {
          name: "Candidata A",
          maturity: "DOSSIER_READY",
          evidenceJson: JSON.stringify({
            referencedEvidence: {
              publicEvidence: [
                { evidenceId: "P1", sourceQuery: "q1" },
                { evidenceId: "P3", sourceQuery: "q3" },
              ],
              customerInterviews: [{ evidenceId: "I1" }],
            },
          }),
        },
        { name: "Candidata B", maturity: "RESEARCHABLE" },
      ],
      evidenceReport: {
        marketExpansion: {
          attempts: [
            { attemptNumber: 1, outcome: "ADJUST_AND_CONTINUE" },
            { attemptNumber: 2, outcome: "DOSSIER_READY_FOUND" },
          ],
        },
      },
    },
  };
  const job = {
    previousCandidates: [{ name: "Candidata A" }, { name: "Candidata B" }],
    customerInterviews: Array.from({ length: 5 }, (_, index) => ({
      id: index + 1,
    })),
    gapResearchPolicy: {
      estimatedSearchCostPerRequestUsd: 0.005,
      maximumModelInvocations: 4,
      costCoverage: "ESTIMATED_SEARCH_ONLY",
      modelCostCoverage: "AGENT_TASK_AUDIT_AFTER_CALLBACK",
      pricingSource: "https://brave.com/search/api/",
      pricingObservedOn: "2026-09-23",
    },
  };

  const report = attachGapDeepeningReport(execution, job);

  assert.equal(report.evidenceReport.gapDeepening.actualSearchRequests, 4);
  assert.equal(report.evidenceReport.gapDeepening.plannedSearchRequests, 4);
  assert.deepEqual(report.evidenceReport.gapDeepening.unexecutedQueries, []);
  assert.equal(report.evidenceReport.gapDeepening.estimatedSearchCostUsd, 0.02);
  assert.equal(report.evidenceReport.gapDeepening.modelInvocationCount, 4);
  assert.equal(
    report.evidenceReport.gapDeepening.modelCostCoverage,
    "AGENT_TASK_AUDIT_AFTER_CALLBACK",
  );
  assert.deepEqual(
    report.evidenceReport.gapDeepening.resolvedGaps[0].executedQueries,
    ["q1", "q3"],
  );
  assert.deepEqual(
    report.evidenceReport.gapDeepening.resolvedGaps[0].resolutionQueries,
    ["q1", "q3"],
  );
  assert.equal(
    report.evidenceReport.gapDeepening.resolvedGaps[1].status,
    "STILL_OPEN",
  );
});

test("não declara como executadas as consultas de uma lente repetida", () => {
  const gap = (candidateName, publicQueries) => ({
    candidateName,
    pendingQuestion: `Pergunta de ${candidateName}`,
    appropriateSource: "Relato e oferta atual",
    evidenceNeeded: "Compra, desistência, preço e entrega",
    contraryEvidenceToSeek: "Alternativa gratuita suficiente",
    publicQueries,
  });
  const execution = {
    directedAttempts: [
      {
        attemptNumber: 1,
        directed: {
          mode: "CODEX",
          plan: {
            publicQueries: ["q1", "q2"],
            candidateGaps: [
              gap("Candidata A", ["q1"]),
              gap("Candidata B", ["q2"]),
            ],
          },
        },
      },
      {
        attemptNumber: 2,
        directed: {
          mode: "CODEX",
          plan: {
            publicQueries: ["q3", "q4"],
            candidateGaps: [
              gap("Candidata A", ["q3"]),
              gap("Candidata B", ["q4"]),
            ],
          },
        },
      },
    ],
    analysisAttempts: [{ attemptNumber: 1, analysis: { mode: "CODEX" } }],
    report: {
      opportunities: [
        { name: "Candidata A", maturity: "RESEARCHABLE" },
        { name: "Candidata B", maturity: "RESEARCHABLE" },
      ],
      evidenceReport: {
        marketExpansion: {
          attempts: [
            { attemptNumber: 1, outcome: "ADJUST_AND_CONTINUE" },
            { attemptNumber: 2, outcome: "REPEATED_RESEARCH_LENS" },
          ],
        },
      },
    },
  };
  const job = {
    previousCandidates: [{ name: "Candidata A" }, { name: "Candidata B" }],
    customerInterviews: Array.from({ length: 5 }),
    gapResearchPolicy: {
      estimatedSearchCostPerRequestUsd: 0.005,
      maximumModelInvocations: 4,
      costCoverage: "ESTIMATED_SEARCH_ONLY",
      modelCostCoverage: "AGENT_TASK_AUDIT_AFTER_CALLBACK",
      pricingSource: "https://brave.com/search/api/",
      pricingObservedOn: "2026-09-23",
    },
  };

  const deepening = attachGapDeepeningReport(execution, job).evidenceReport
    .gapDeepening;

  assert.equal(deepening.plannedSearchRequests, 4);
  assert.equal(deepening.actualSearchRequests, 2);
  assert.deepEqual(deepening.executedQueries, ["q1", "q2"]);
  assert.deepEqual(deepening.unexecutedQueries, ["q3", "q4"]);
  assert.equal(deepening.estimatedSearchCostUsd, 0.01);
});

test("preserva na tela a causa segura devolvida pelo backend", async () => {
  const message = await backendFailureMessage(
    "POST",
    "http://backend/api/complete",
    {
      status: 400,
      text: async () =>
        JSON.stringify({
          message:
            "As partes do agente e da atividade devem compor o prompt integral.",
        }),
    },
  );

  assert.equal(
    message,
    "POST http://backend/api/complete failed with status 400: As partes do agente e da atividade devem compor o prompt integral.",
  );
});

test("não incorpora corpo não estruturado nem credencial em erro operacional", async () => {
  const message = await backendFailureMessage(
    "POST",
    "http://backend/api/complete",
    {
      status: 500,
      text: async () => "<html>sk-segredo-nao-deve-aparecer</html>",
    },
  );

  assert.equal(
    message,
    "POST http://backend/api/complete failed with status 500",
  );
});

test("repete o lease vigente em todo callback da descoberta", () => {
  const payload = withExecutionLease(
    { cycleId: 36, executionLeaseId: "lease-atual" },
    { decisionSummary: "Pesquisar mais", opportunities: [] },
  );

  assert.deepEqual(payload, {
    executionLeaseId: "lease-atual",
    decisionSummary: "Pesquisar mais",
    opportunities: [],
  });
});

test("propaga a auditoria da tentativa quando Argos bloqueia antes do plano", () => {
  const error = new Error("Plano dirigido fora do contrato");
  error.executionAudit = {
    executionMode: "MODEL",
    modelCode: "gpt-5.6-sol",
    reasoningEffort: "high",
    promptSent: "Núcleo de Argos.\n\nPesquise o mercado.",
    agentPromptPart: "Núcleo de Argos.",
    activityPromptPart: "Pesquise o mercado.",
    accessedUrls: [],
  };

  assert.deepEqual(failureCallbackPayload(error), {
    errorMessage: "Plano dirigido fora do contrato",
    executionAudit: error.executionAudit,
  });
});

test("impede polling sobreposto enquanto uma pesquisa ainda está em execução", () => {
  const lock = createPollLock();

  assert.equal(lock.tryAcquire(), true);
  assert.equal(lock.tryAcquire(), false);
  lock.release();
  assert.equal(lock.tryAcquire(), true);
});

test("repete o mesmo callback e lease enquanto o backend ainda recusa conexão", async () => {
  const calls = [];
  const waits = [];
  const warnings = [];
  let remainingFailures = 2;
  const payload = { executionLeaseId: "lease-296", planJson: "{}" };

  const result = await postJson("http://backend/api/plan", payload, {
    maxAttempts: 3,
    retryDelayMs: 7,
    fetchFn: async (_url, request) => {
      calls.push(request.body);
      if (remainingFailures > 0) {
        remainingFailures -= 1;
        const cause = Object.assign(new Error("connection refused"), {
          code: "ECONNREFUSED",
        });
        throw new TypeError("fetch failed", { cause });
      }
      return { ok: true, json: async () => ({ accepted: true }) };
    },
    sleepFn: async (delayMs) => waits.push(delayMs),
    logger: { warn: (message) => warnings.push(message) },
  });

  assert.deepEqual(result, { accepted: true });
  assert.equal(calls.length, 3);
  assert.ok(calls.every((body) => body === JSON.stringify(payload)));
  assert.deepEqual(waits, [7, 7]);
  assert.equal(warnings.length, 2);
});

test("não repete erro HTTP de contrato nem conexão interrompida após estabelecimento", async () => {
  let contractCalls = 0;
  let uncertainCalls = 0;
  const noWait = async () => assert.fail("não deveria aguardar nova tentativa");

  await assert.rejects(
    postJson(
      "http://backend/api/plan",
      {},
      {
        maxAttempts: 3,
        fetchFn: async () => {
          contractCalls += 1;
          return {
            ok: false,
            status: 400,
            text: async () => JSON.stringify({ message: "Contrato inválido" }),
          };
        },
        sleepFn: noWait,
      },
    ),
    /status 400: Contrato inválido/,
  );
  await assert.rejects(
    postJson(
      "http://backend/api/complete",
      {},
      {
        maxAttempts: 3,
        fetchFn: async () => {
          uncertainCalls += 1;
          const cause = Object.assign(new Error("connection reset"), {
            code: "ECONNRESET",
          });
          throw new TypeError("fetch failed", { cause });
        },
        sleepFn: noWait,
      },
    ),
    /fetch failed/,
  );

  assert.equal(contractCalls, 1);
  assert.equal(uncertainCalls, 1);
});

test("propaga prompt e tokens reais no callback auditável do plano", () => {
  const payload = researchPlanCallbackPayload({
    plan: { questions: ["Qual dor é urgente?"] },
    rawResponse: '{"questions":["Qual dor é urgente?"]}',
    model: "gpt-5.6-sol",
    mode: "CODEX",
    prompt: "Núcleo de Argos.\n\nPesquise a oportunidade.",
    agentPromptPart: "Núcleo de Argos.",
    activityPromptPart: "Pesquise a oportunidade.",
    reasoningEffort: "high",
    usage: { inputTokens: 100, cachedInputTokens: 20, outputTokens: 10 },
  });

  assert.deepEqual(payload, {
    planJson: '{"questions":["Qual dor é urgente?"]}',
    rawResponse: '{"questions":["Qual dor é urgente?"]}',
    model: "gpt-5.6-sol",
    executionMode: "CODEX",
    promptSent: "Núcleo de Argos.\n\nPesquise a oportunidade.",
    agentPromptPart: "Núcleo de Argos.",
    activityPromptPart: "Pesquise a oportunidade.",
    reasoningEffort: "high",
    inputTokens: 100,
    cachedInputTokens: 20,
    outputTokens: 10,
  });
});

test("agrega as duas chamadas de Argos sem perder URLs nem resposta bruta", () => {
  const payload = analysisAuditCallbackPayload(
    {
      mode: "CODEX",
      prompt: "agente do plano\n\natividade do plano",
      agentPromptPart: "agente do plano",
      activityPromptPart: "atividade do plano",
      reasoningEffort: "high",
      usage: { inputTokens: 100, cachedInputTokens: 20, outputTokens: 10 },
    },
    {
      rawResponse: '{"candidates":[]}',
      model: "gpt-5.6-sol",
      mode: "CODEX",
      prompt: "agente da síntese\n\natividade da síntese",
      agentPromptPart: "agente da síntese",
      activityPromptPart: "atividade da síntese",
      reasoningEffort: "high",
      usage: { inputTokens: 300, cachedInputTokens: 100, outputTokens: 80 },
      accessedUrls: [
        {
          url: "https://example.com/fonte",
          label: "Fonte",
          accessMethod: "WEB_SEARCH",
          accessedAt: "2026-08-30T17:00:00.000Z",
        },
      ],
    },
  );

  assert.equal(payload.executionMode, "MODEL");
  assert.equal(payload.inputTokens, 400);
  assert.equal(payload.cachedInputTokens, 120);
  assert.equal(payload.outputTokens, 90);
  assert.match(payload.promptSent, /PLANEJAMENTO/);
  assert.match(payload.promptSent, /SÍNTESE FACTUAL/);
  assert.match(
    payload.promptSent,
    /PLANEJAMENTO ---\nagente do plano\n\natividade do plano/,
  );
  assert.match(
    payload.promptSent,
    /SÍNTESE FACTUAL ---\nagente da síntese\n\natividade da síntese/,
  );
  assert.match(payload.agentPromptPart, /agente do plano/);
  assert.match(payload.agentPromptPart, /agente da síntese/);
  assert.match(payload.activityPromptPart, /atividade do plano/);
  assert.match(payload.activityPromptPart, /atividade da síntese/);
  assert.equal(payload.accessedUrls.length, 1);
  assert.equal(payload.rawResponse, '{"candidates":[]}');
});
