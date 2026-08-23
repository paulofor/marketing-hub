import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { CODEX_SANDBOX, CONTRACTS, parseUsage, validateResult } from "./run-pde-construction-v2-agent.mjs";

test("separa os três agentes de revisão", () => {
  assert.equal(CONTRACTS.dedalo.expectedAgent, "DEDALO");
  assert.equal(CONTRACTS.temis.expectedAgent, "TEMIS");
  assert.equal(CONTRACTS.psique.expectedAgent, "PSIQUE");
});

test("permite auditoria no snapshot sem depender de namespace aninhado", () => {
  assert.equal(CODEX_SANDBOX, "danger-full-access");
});

test("preserva a última telemetria cumulativa", () => {
  const usage = parseUsage('{"usage":{"input_tokens":120,"cached_input_tokens":40,"output_tokens":20}}\n');
  assert.deepEqual(usage, { informed: true, inputTokens: 120, cachedInputTokens: 40, outputTokens: 20, serviceTier: null });
});

test("rejeita aprovação contraditória", () => {
  const result = {
    agent: "DEDALO",
    decision: "APPROVE",
    alternatives: [{}, {}, {}],
    evidence: ["um dado", "dois dados", "três dados", "quatro dados"],
    findings: [{ severity: "BLOCKER" }]
  };
  assert.throws(() => validateResult(result, CONTRACTS.dedalo), /contradiz/);
});

test("mantém o contrato canônico MUSA v7 idêntico no PDE e no Liquibase do Hub", async () => {
  const canonical = JSON.parse(await readFile(
    new URL("../pde-platform/backend/src/main/resources/contracts/musa-v7-product-v1.json", import.meta.url),
    "utf8",
  ));
  const migration = await readFile(
    new URL("../backend/ads-service/src/main/resources/db/changelog/changesets/2026-08-23-musa-v7-product-contract.sql", import.meta.url),
    "utf8",
  );
  const contractLiteral = migration.match(/SET @musa_v7_contract = '([^']+)';/)?.[1];
  assert.ok(contractLiteral, "contrato MUSA v7 ausente no changelog");
  assert.deepEqual(JSON.parse(contractLiteral), canonical);
});
