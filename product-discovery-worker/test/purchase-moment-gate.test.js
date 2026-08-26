import assert from "node:assert/strict";
import test from "node:test";
import { buildPurchaseMomentResearchGate } from "../src/purchase-moment-gate.js";

test("abre protótipo privado quando as fontes B2C estão atuais e nominais", () => {
  const gate = buildPurchaseMomentResearchGate(
    b2cJob(),
    [offer("1", "Treino para entrevista", "2026-08-25T00:00:00Z")],
    { evaluatedAt: "2026-08-26T00:00:00Z", maxSourceAgeDays: 30 },
  );

  assert.equal(gate.status, "WAITING_PRIVATE_PROTOTYPE");
  assert.equal(gate.sourceQualityPassed, true);
  assert.equal(gate.finalPrioritizationEligible, false);
  assert.equal(gate.minimumIndependentReadings, 2);
});

test("bloqueia placeholder e oferta comercial vencida", () => {
  const gate = buildPurchaseMomentResearchGate(
    b2cJob(),
    [
      offer("1", "Oferta 1", "2026-08-25T00:00:00Z"),
      offer("2", "Treino real", "2026-06-01T00:00:00Z"),
    ],
    { evaluatedAt: "2026-08-26T00:00:00Z", maxSourceAgeDays: 30 },
  );

  assert.equal(gate.status, "WAITING_SOURCE_QUALITY");
  assert.match(gate.reasons.join(" "), /placeholder/);
  assert.match(gate.reasons.join(" "), /vencida/);
});

test("bloqueia oferta Hotmart sem preço nem sinal de tração", () => {
  const incomplete = offer("1", "Treino real", "2026-08-25T00:00:00Z");
  delete incomplete.price;

  const gate = buildPurchaseMomentResearchGate(b2cJob(), [incomplete], {
    evaluatedAt: "2026-08-26T00:00:00Z",
    maxSourceAgeDays: 30,
  });

  assert.equal(gate.status, "WAITING_SOURCE_QUALITY");
  assert.match(gate.reasons.join(" "), /preço nem sinal de tração/);
});

test("não exige o gate no ciclo que não declara B2C e Instagram", () => {
  const gate = buildPurchaseMomentResearchGate(
    { acquisitionChannel: "SEO", targetAudience: "empresas" },
    [],
  );

  assert.equal(gate.status, "NOT_REQUIRED");
  assert.equal(gate.finalPrioritizationEligible, true);
});

function b2cJob() {
  return {
    acquisitionChannel: "Instagram",
    targetAudience: "pessoa física com entrevista marcada",
    commercialConstraints: "B2C e mobile",
  };
}

function offer(referenceId, title, collectedAt) {
  return {
    marketplace: "HOTMART",
    referenceId,
    title,
    url: `https://hotmart.com/${referenceId}`,
    price: "R$ 29,00",
    collectedAt,
  };
}
