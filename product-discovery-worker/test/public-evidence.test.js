import test from "node:test";
import { createHash } from "node:crypto";
import assert from "node:assert/strict";
import { assessPublicEvidence } from "../src/public-evidence.js";

const evidence = [
  {
    evidenceId: "P7",
    url: "https://reviews.example.org/occasion",
    retrievedAt: "2026-09-24T12:00:00Z",
    snippet:
      "Comprei o serviço, mas ainda tive dificuldade para escolher uma combinação.",
  },
  {
    evidenceId: "P8",
    url: "https://community.example.net/report",
    retrievedAt: "2026-09-24T12:00:00Z",
    snippet:
      "Usei a alternativa gratuita e desisti da assinatura porque exigia muito esforço.",
  },
];
const observations = evidence.map((item, i) => ({
  evidenceId: item.evidenceId,
  sourceRole: "PUBLIC_CUSTOMER_REPORT",
  reportedAction: i ? "ABANDONMENT_REPORTED" : "PURCHASE_REPORTED",
  supportingExcerpt: item.snippet,
  limitation: "Relato público não conciliado; trecho da busca apenas.",
}));

test("confirma suporte em duas fontes sem converter relatos em vendas verificadas", () => {
  const result = assessPublicEvidence(observations, evidence, ["P7", "P8"]);
  assert.equal(result.ready, true);
  assert.equal(result.observations[0].sourceSha256.length, 64);
  assert.equal(result.observations[0].evidenceScope, "SEARCH_EXCERPT_ONLY");
});

test("domínios e subdomínios repetidos não multiplicam confirmação independente", () => {
  const sources = structuredClone(evidence);
  sources[1].url = "https://other.example.org/story";
  assert.equal(
    assessPublicEvidence(observations, sources, ["P7", "P8"]).ready,
    false,
  );
});

test("vendedor e intenção desconhecida não contam como comportamento passado", () => {
  for (const patch of [
    { sourceRole: "SELLER_CLAIM" },
    { reportedAction: "UNKNOWN" },
  ]) {
    const items = structuredClone(observations);
    Object.assign(items[1], patch);
    assert.equal(
      assessPublicEvidence(items, evidence, ["P7", "P8"]).ready,
      false,
    );
  }
});

test("ausência de relatos encerra com lacuna explícita sem fabricar entrevista", () => {
  assert.equal(assessPublicEvidence([], evidence, ["P7", "P8"]).ready, false);
});

test("recusa trecho fabricado, fonte ausente, duplicada ou de outra candidata", () => {
  const forged = structuredClone(observations);
  forged[0].supportingExcerpt = "Uma pessoa comprou e gostou muito do produto";
  assert.throws(() => assessPublicEvidence(forged, evidence, ["P7", "P8"]));
  assert.throws(() => assessPublicEvidence(observations, evidence, ["P8"]));
  assert.throws(() =>
    assessPublicEvidence([...observations, observations[0]], evidence, [
      "P7",
      "P8",
    ]),
  );
  assert.throws(() =>
    assessPublicEvidence(observations, evidence.slice(1), ["P7", "P8"]),
  );
});

test("recusa fonte sem data de coleta auditável", () => {
  const sources = structuredClone(evidence);
  sources[0].retrievedAt = null;
  assert.throws(() =>
    assessPublicEvidence(observations, sources, ["P7", "P8"]),
  );
});

test("persiste trecho e hash brutos quando o modelo decodifica HTML", () => {
  const sources = structuredClone(evidence);
  sources[0].snippet = "A opção &#x27;pronta&#x27; não resolveu a dificuldade.";
  const items = structuredClone(observations);
  items[0].supportingExcerpt = "A opção 'pronta' não resolveu a dificuldade.";
  const result = assessPublicEvidence(items, sources, ["P7", "P8"]);
  assert.equal(result.observations[0].supportingExcerpt, sources[0].snippet);
  assert.equal(
    result.observations[0].sourceSha256,
    createHash("sha256").update(sources[0].snippet).digest("hex"),
  );
  assert.equal(
    sources[0].snippet,
    "A opção &#x27;pronta&#x27; não resolveu a dificuldade.",
  );
  assert.equal(result.ready, true);
});

test("preserva os limites literais do backend depois do remapeamento HTML", () => {
  const sources = structuredClone(evidence);
  const items = structuredClone(observations);
  sources[0].snippet = "&#x27;".repeat(100);
  items[0].supportingExcerpt = "'".repeat(100);
  assert.throws(() => assessPublicEvidence(items, sources, ["P7", "P8"]));
  sources[0].snippet = "Texto com ' presente.";
  items[0].supportingExcerpt = "&#0000000000000039;";
  assert.throws(() => assessPublicEvidence(items, sources, ["P7", "P8"]));
});
