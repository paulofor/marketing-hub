import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';

// Confere contratos sem chamar modelo, mudar parecer ou consumir orçamento.
const schemas = [
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/pde-agent-validation-review-v4-schema.json",
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/pde-private-validation-review-v2-schema.json",
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/creative-commercial-review-schema.json",
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/landing-commercial-review-schema.json",
  "customer-agent-worker/src/main/resources/prompts/bpm/v5/pde-agent-validation-scenario-review-schema.json",
  "customer-agent-worker/src/main/resources/prompts/bpm/v7/pde-kit-agent-validation-scenario-review-schema.json",
  "customer-agent-worker/src/main/resources/prompts/bpm/v4/pde-private-validation-experience-review-schema.json",
  "customer-agent-worker/src/main/resources/prompts/bpm/v3/creative-customer-review-schema.json",
  "experiment-strategist-worker/src/main/resources/prompts/pde-commercial-plan/v13/market-strategy-schema.json",
  "experiment-strategist-worker/src/main/resources/prompts/pde-commercial-plan/v10/market-strategy-schema.json",
  "financial-agent-worker/src/main/resources/prompts/pde-commercial-plan/v6/economics-schema.json",
  "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v2/journey-schema.json",
  "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v2/deliverables-schema.json",
  "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v2/access-schema.json",
  "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v3/prototype-correction-schema.json",
  "communication-agent-worker/src/main/resources/prompts/iris/v1/output-schema.json"
];
const cores = [
  "meta-ad-approver-worker/src/main/resources/prompts/temis/v1/agent-core.md",
  "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v4.md",
  "experiment-strategist-worker/src/main/resources/prompts/experiment-strategist/v1/agent-core.md",
  "financial-agent-worker/src/main/resources/prompts/financial-agent/v1/agent-core.md",
  "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v1/agent-core.md",
  "communication-agent-worker/src/main/resources/prompts/iris/v1/behavioral-core.md",
  "product-discovery-worker/prompts/productdiscovery.v1/research/system.md",
  "growth-operator-worker/src/main/resources/prompts/growth-operator/v2/agent-core.md"
];
for (const file of schemas) {
  test(`resumo curto e obrigatório: ${file}`, async () => {
    const schema = JSON.parse(await readFile(new URL('../' + file, import.meta.url), 'utf8'));
    assert.ok(schema.required.includes('humanSummary'));
    assert.equal(schema.properties.humanSummary.type, 'string');
    assert.equal(schema.properties.humanSummary.minLength, 20);
    assert.equal(schema.properties.humanSummary.maxLength, 420);
    assert.equal(schema.additionalProperties, false);
    assert.deepEqual(new Set(schema.required), new Set(Object.keys(schema.properties)));
  });
}
for (const file of cores) {
  test(`orientação humana carregada pelo agente: ${file}`, async () => {
    const prompt = await readFile(new URL('../' + file, import.meta.url), 'utf8');
    for (const phrase of ['humanSummary', '20 a 420', 'até três frases', 'não diga que o produto falhou', 'vêm do backend']) assert.ok(prompt.includes(phrase));
  });
}

// O Catálogo Vivo fixa estes contratos por hash; a síntese usa os campos existentes.
for (const [file, expected] of [
  ["customer-agent-worker/src/main/resources/prompts/bpm/v3/pde-commercial-homologation-customer-review-schema.json", "9925d5739180644d289db1d38fe41d305aa29f3a8c3b3cdd8fde9fcd7b687e03"],
  ["meta-ad-approver-worker/src/main/resources/prompts/bpm/pde-commercial-homologation-independent-review-schema.json", "7cbd2cc0b35aeb286280b76a77da87ecf7ba16431df54ecb53c4cdfb05b4073d"]
]) {
  test(`schema do catálogo permanece compatível: ${file}`, async () => {
    const source = await readFile(new URL('../' + file, import.meta.url));
    assert.equal(createHash('sha256').update(source).digest('hex'), expected);
    const schema = JSON.parse(source);
    assert.equal((schema.properties.commercialRationale ?? schema.properties.customerPerspective).type, 'string');
  });
}
