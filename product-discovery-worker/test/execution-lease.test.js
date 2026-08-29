import assert from "node:assert/strict";
import test from "node:test";
import {
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
    promptSent: "Prompt integral enviado a Argos.",
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
    prompt: "Contexto integral enviado a Argos.",
    reasoningEffort: "high",
    usage: { inputTokens: 100, cachedInputTokens: 20, outputTokens: 10 },
  });

  assert.deepEqual(payload, {
    planJson: '{"questions":["Qual dor é urgente?"]}',
    rawResponse: '{"questions":["Qual dor é urgente?"]}',
    model: "gpt-5.6-sol",
    executionMode: "CODEX",
    promptSent: "Contexto integral enviado a Argos.",
    reasoningEffort: "high",
    inputTokens: 100,
    cachedInputTokens: 20,
    outputTokens: 10,
  });
});
