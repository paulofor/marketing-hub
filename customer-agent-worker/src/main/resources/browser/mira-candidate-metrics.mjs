/** Mede somente ações observadas pelo executor e intervalos no mesmo relógio monotônico. */
export class MiraCandidateMeasurements {
  constructor(clock = () => performance.now()) {
    this.clock = clock;
    this.started = clock();
    this.outcomeElapsed = null;
    this.fields = new Map();
    this.corrections = 0;
  }
  filled(id, kind) {
    if (this.fields.has(id)) this.corrections++;
    this.fields.set(id, kind);
  }
  outcome() {
    if (this.outcomeElapsed === null)
      this.outcomeElapsed = this.clock() - this.started;
  }
  report(minimumRequiredProductFields, providedProductCount) {
    if (this.outcomeElapsed === null)
      throw new Error("Primeiro resultado ou bloqueio não observado.");
    const count = (kind) =>
      [...this.fields.values()].filter((value) => value === kind).length;
    return {
      resultReadySeconds: Math.ceil(this.outcomeElapsed / 1000),
      measurementClock: "MONOTONIC_WORKER",
      outcomeBoundary: "FIRST_VISIBLE_RESULT_OR_SAFE_BLOCK",
      scenarioCompletedSeconds: Math.ceil((this.clock() - this.started) / 1000),
      minimumRequiredProductFields,
      requiredObjectiveFields: 1,
      objectivePrefilled: true,
      providedProductCount,
      filledProductFields: count("PRODUCT"),
      filledOptionalSourceFields: count("SOURCE"),
      editedObjectiveFields: count("OBJECTIVE"),
      actualFilledFields: this.fields.size,
      corrections: this.corrections,
    };
  }
}
