/** Impede que uma nova amostragem do modelo altere a regra objetiva de comparação. */
export function validateFunctionalResult(agentRole, context, result) {
  if (agentRole !== "dedalo") return;
  const alternatives = context.argos?.alternatives || [];
  const expectedNames = alternatives.map((item) => item.name).sort();
  const actualNames = result.comparison.map((item) => item.name).sort();
  if (JSON.stringify(expectedNames) !== JSON.stringify(actualNames)) {
    throw new Error("Dédalo não preservou as três alternativas de Argos.");
  }
  for (const item of result.comparison) {
    const sum =
      item.evidenceScore +
      item.purchaseIntentScore +
      item.simplicityScore +
      item.riskSafetyScore +
      item.rapidValueScore;
    if (sum !== item.totalScore) {
      throw new Error(`Score total inconsistente em ${item.name}.`);
    }
  }
  const winner = [...result.comparison].sort(
    (left, right) =>
      right.totalScore - left.totalScore ||
      right.riskSafetyScore - left.riskSafetyScore ||
      left.name.localeCompare(right.name),
  )[0];
  if (result.chosenOpportunity.sourceAlternativeName !== winner.name) {
    throw new Error("Dédalo escolheu alternativa diferente do maior score auditável.");
  }
}
