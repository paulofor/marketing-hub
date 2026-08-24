import assert from "node:assert/strict";
import test from "node:test";
import {
  resolveOpportunityAgentTimeoutMs,
  resolveOpportunityModel,
} from "./agent-configuration.mjs";

test("ignora modelo genérico do ambiente e usa o modelo homologado", () => {
  assert.equal(resolveOpportunityModel({ OPENAI_MODEL: "gpt-5-codex" }), "gpt-5.6-terra");
});

test("aceita configuração específica homologada", () => {
  assert.equal(
    resolveOpportunityModel({ PDE_OPPORTUNITY_MODEL: "gpt-5.6-terra" }),
    "gpt-5.6-terra",
  );
});

test("bloqueia modelo específico sem contrato e custo versionados", () => {
  assert.throws(
    () => resolveOpportunityModel({ PDE_OPPORTUNITY_MODEL: "modelo-inexistente" }),
    /Modelo PDE não homologado/,
  );
});

test("usa timeout próprio e limitado para cada execução de agente", () => {
  assert.equal(resolveOpportunityAgentTimeoutMs({}), 600_000);
  assert.equal(
    resolveOpportunityAgentTimeoutMs({ PDE_OPPORTUNITY_AGENT_TIMEOUT_MS: "120000" }),
    120_000,
  );
});

test("bloqueia timeout ausente de limite operacional seguro", () => {
  assert.throws(
    () =>
      resolveOpportunityAgentTimeoutMs({ PDE_OPPORTUNITY_AGENT_TIMEOUT_MS: "0" }),
    /deve ser um inteiro entre 30000 e 900000/,
  );
  assert.throws(
    () =>
      resolveOpportunityAgentTimeoutMs({ PDE_OPPORTUNITY_AGENT_TIMEOUT_MS: "sem-limite" }),
    /deve ser um inteiro entre 30000 e 900000/,
  );
});
