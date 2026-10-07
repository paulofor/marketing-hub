import test from "node:test";
import assert from "node:assert/strict";
import { MiraCandidateMeasurements } from "../../main/resources/browser/mira-candidate-metrics.mjs";

test("mede o primeiro resultado sem datas de outro servidor nem atraso de confirmação", () => {
  let monotonic = 500;
  const metrics = new MiraCandidateMeasurements(() => monotonic);
  monotonic = 1500;
  metrics.outcome();
  monotonic = 90000;
  metrics.outcome();
  const report = metrics.report(2, 2);
  assert.equal(report.resultReadySeconds, 1);
  assert.equal(report.scenarioCompletedSeconds, 90);
  assert.equal(report.measurementClock, "MONOTONIC_WORKER");
});

test("separa mínimo, preenchimentos opcionais, objetivo e correções em ambas as condições", () => {
  for (const minimum of [2, 4]) {
    const metrics = new MiraCandidateMeasurements(() => 0);
    for (const id of ["name-1", "directions-1", "name-2", "directions-2"])
      metrics.filled(id, "PRODUCT");
    for (const id of ["source-1", "source-2"]) metrics.filled(id, "SOURCE");
    metrics.filled("objective", "OBJECTIVE");
    metrics.filled("name-1", "PRODUCT");
    metrics.outcome();
    assert.deepEqual(metrics.report(minimum, 2), {
      resultReadySeconds: 0,
      measurementClock: "MONOTONIC_WORKER",
      outcomeBoundary: "FIRST_VISIBLE_RESULT_OR_SAFE_BLOCK",
      scenarioCompletedSeconds: 0,
      minimumRequiredProductFields: minimum,
      requiredObjectiveFields: 1,
      objectivePrefilled: true,
      providedProductCount: 2,
      filledProductFields: 4,
      filledOptionalSourceFields: 2,
      editedObjectiveFields: 1,
      actualFilledFields: 7,
      corrections: 1,
    });
  }
});

test("não emite medida de resultado que ainda não aconteceu", () => {
  const metrics = new MiraCandidateMeasurements();
  assert.throws(() => metrics.report(2, 2), /não observado/);
});
