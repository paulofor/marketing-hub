import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import {
  validateArgosOutputSchemaContracts,
  validateStrictOutputSchema,
} from "../src/strict-output-schema.js";

test("todos os schemas de Argos atendem o subconjunto estrito antes do runtime", async () => {
  await validateArgosOutputSchemaContracts();
});

test("worker valida contratos antes de iniciar health e polling", async () => {
  const workerSource = await readFile(
    new URL("../src/worker.js", import.meta.url),
    "utf8",
  );
  const validationPosition = workerSource.indexOf(
    "await validateArgosOutputSchemaContracts()",
  );
  const healthPosition = workerSource.indexOf("startHealthServer({");
  const firstCyclePosition = workerSource.indexOf("await runCycle();");

  assert.ok(validationPosition >= 0);
  assert.ok(validationPosition < healthPosition);
  assert.ok(validationPosition < firstCyclePosition);
});

test("schema estrito rejeita propriedade declarada como opcional", () => {
  assert.throws(
    () =>
      validateStrictOutputSchema(
        {
          type: "object",
          additionalProperties: false,
          required: [],
          properties: {
            candidateGaps: { type: "array", items: { type: "string" } },
          },
        },
        "contrato de regressão",
      ),
    /ausentes=candidateGaps/,
  );
});

test("schema estrito rejeita palavra-chave incompatível em nó aninhado", () => {
  assert.throws(
    () =>
      validateStrictOutputSchema(
        {
          type: "object",
          additionalProperties: false,
          required: ["queries"],
          properties: {
            queries: {
              type: "array",
              uniqueItems: true,
              items: { type: "string" },
            },
          },
        },
        "contrato de regressão",
      ),
    /incompatível uniqueItems/,
  );
});
