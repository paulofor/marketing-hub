import assert from "node:assert/strict";
import test from "node:test";
import {
  analysisAuditCallbackPayload,
  createPollLock,
  failureCallbackPayload,
  researchPlanCallbackPayload,
  withExecutionLease,
} from "../src/worker.js";

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
      prompt: "prompt do plano",
      agentPromptPart: "agente do plano",
      activityPromptPart: "atividade do plano",
      reasoningEffort: "high",
      usage: { inputTokens: 100, cachedInputTokens: 20, outputTokens: 10 },
    },
    {
      rawResponse: '{"candidates":[]}',
      model: "gpt-5.6-sol",
      mode: "CODEX",
      prompt: "prompt da síntese",
      agentPromptPart: "agente da síntese",
      activityPromptPart: "atividade da síntese",
      reasoningEffort: "high",
      usage: { inputTokens: 300, cachedInputTokens: 100, outputTokens: 80 },
      accessedUrls: [
        {
          url: "https://example.com/fonte",
          label: "Fonte",
          accessMethod: "PUBLIC_SEARCH",
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
  assert.equal(payload.accessedUrls.length, 1);
  assert.equal(payload.rawResponse, '{"candidates":[]}');
});
