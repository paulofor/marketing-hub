import test from "node:test";
import assert from "node:assert/strict";
import { createAutomaticExecutionControl } from "../src/automatic-execution-control.js";

test("Argos executes automatically only when backend reports PLAY", async () => {
  const control = createAutomaticExecutionControl({
    backendBaseUrl: "http://backend.test",
    fetchImpl: async () => ({
      ok: true,
      json: async () => ({ automaticExecutionEnabled: true }),
    }),
  });

  assert.equal(await control.allowsAutomaticExecution(), true);
});

test("Argos stays stopped when backend reports STOP", async () => {
  const control = createAutomaticExecutionControl({
    backendBaseUrl: "http://backend.test",
    fetchImpl: async () => ({
      ok: true,
      json: async () => ({ automaticExecutionEnabled: false }),
    }),
  });

  assert.equal(await control.allowsAutomaticExecution(), false);
});

test("Argos fails closed when PLAY cannot be confirmed", async () => {
  const errors = [];
  const control = createAutomaticExecutionControl({
    backendBaseUrl: "http://backend.test",
    fetchImpl: async () => {
      throw new Error("backend unavailable");
    },
    logger: { error: (...args) => errors.push(args) },
  });

  assert.equal(await control.allowsAutomaticExecution(), false);
  assert.equal(errors.length, 1);
});
