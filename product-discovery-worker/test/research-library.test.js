import assert from "node:assert/strict";
import test from "node:test";
import { selectResearchLibraryContext } from "../src/research-library.js";

test("consulta coleções vivas obrigatórias e inclui momentos de compra no B2C Instagram", async () => {
  const context = await selectResearchLibraryContext({
    researchMode: "DISCOVER_MARKETS",
    marketType: "B2C",
    theme: "mercados femininos com experiência visual",
    targetAudience: "mulheres 40+",
    acquisitionChannel: "Instagram",
  });

  assert.deepEqual(
    context.coverage.map((item) => item.collection),
    ["gartner", "ia-aplicada", "momentos-de-compra-b2c"],
  );
  assert.ok(context.coverage.every((item) => item.status === "CONSULTED"));
  assert.ok(context.coverage.every((item) => item.documentCount > 0));
  assert.ok(context.evidence.some((item) => item.collection === "gartner"));
  assert.ok(context.evidence.some((item) => item.collection === "ia-aplicada"));
  assert.ok(
    context.evidence.some(
      (item) => item.collection === "momentos-de-compra-b2c",
    ),
  );
  assert.ok(context.evidence.every((item) => !item.path.endsWith("/ini.md")));
  assert.ok(context.evidence.every((item) => /^[a-f0-9]{64}$/.test(item.sha256)));
});
