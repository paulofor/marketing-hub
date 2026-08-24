import assert from "node:assert/strict";
import test from "node:test";
import { createPollLock, withExecutionLease } from "../src/worker.js";

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

test("impede polling sobreposto enquanto uma pesquisa ainda está em execução", () => {
  const lock = createPollLock();

  assert.equal(lock.tryAcquire(), true);
  assert.equal(lock.tryAcquire(), false);
  lock.release();
  assert.equal(lock.tryAcquire(), true);
});
