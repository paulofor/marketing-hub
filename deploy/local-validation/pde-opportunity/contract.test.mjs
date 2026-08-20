import assert from "node:assert/strict";
import test from "node:test";
import { validateFunctionalResult } from "./contract.mjs";

const context = {
  argos: { alternatives: [{ name: "A" }, { name: "B" }, { name: "C" }] },
};

function comparison(name, totalScore, riskSafetyScore = 10) {
  return {
    name,
    evidenceScore: totalScore - 40,
    purchaseIntentScore: 10,
    simplicityScore: 10,
    riskSafetyScore,
    rapidValueScore: 20 - riskSafetyScore,
    totalScore,
  };
}

test("aceita a maior soma preservando as alternativas de Argos", () => {
  assert.doesNotThrow(() =>
    validateFunctionalResult("dedalo", context, {
      comparison: [comparison("A", 80), comparison("B", 70), comparison("C", 60)],
      chosenOpportunity: { sourceAlternativeName: "A" },
    }),
  );
});

test("bloqueia score cuja soma não corresponde ao total", () => {
  const invalid = comparison("A", 80);
  invalid.totalScore = 79;
  assert.throws(
    () =>
      validateFunctionalResult("dedalo", context, {
        comparison: [invalid, comparison("B", 70), comparison("C", 60)],
        chosenOpportunity: { sourceAlternativeName: "A" },
      }),
    /Score total inconsistente/,
  );
});

test("bloqueia escolha diferente do maior score auditável", () => {
  assert.throws(
    () =>
      validateFunctionalResult("dedalo", context, {
        comparison: [comparison("A", 80), comparison("B", 70), comparison("C", 60)],
        chosenOpportunity: { sourceAlternativeName: "B" },
      }),
    /alternativa diferente/,
  );
});
