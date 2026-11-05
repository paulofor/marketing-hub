import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import test from "node:test";
import { validateManufacturerSources } from "../../main/resources/browser/mira-manufacturer-fixture.mjs";

const observed = Date.parse("2026-11-05T12:00:00Z");
const day = 86400000;
const source = {
  url: "https://manufacturer.example/synthetic-test-only",
  labelDirections: "Orientação sintética para testar identidade da fonte.",
  observedAt: new Date(observed).toISOString(),
};
source.sourceExcerptSha256 = createHash("sha256").update(source.labelDirections).digest("hex");

test("preserva fonte e data originais no limite inclusivo de trinta dias", () => {
  const snapshot = structuredClone(source);
  assert.deepEqual(validateManufacturerSources([snapshot], observed + 30 * day), [source]);
});
test("recusa vencimento com fonte e limite exatos, sem renovar evidência", () => {
  const snapshot = structuredClone(source);
  assert.throws(() => validateManufacturerSources([snapshot], observed + 30 * day + 1),
    /manufacturer\.example.*2026-12-05T12:00:00.000Z.*Reconsulte/);
  assert.deepEqual(snapshot, source);
});
test("não aceita datas inválidas ou futuras", () => {
  assert.throws(() => validateManufacturerSources([{ ...source, observedAt: "invalid" }], observed), /sem validade/);
  assert.throws(() => validateManufacturerSources([source], observed - 1), /sem validade/);
});
test("recusa alteração do trecho mesmo dentro do prazo", () => {
  assert.throws(() => validateManufacturerSources([{ ...source, labelDirections: "Outro texto" }], observed), /alterada/);
});
test("recusa coleção vazia ou relógio indefinido", () => {
  assert.throws(() => validateManufacturerSources([], observed), /vazia/);
  assert.throws(() => validateManufacturerSources([source], Number.NaN), /relógio inválido/);
});
