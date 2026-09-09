import assert from "node:assert/strict";
import { selectResearchLibraryContext } from "./src/research-library.js";

// Lê a biblioteca empacotada pelo caminho produtivo sem consultar fontes externas.
const result = await selectResearchLibraryContext({
  theme: "produtividade com inteligência artificial",
  targetAudience: "consumidores individuais",
  acquisitionChannel: "instagram",
});
assert.ok(result.evidence.length > 0, "A imagem deve conter evidências factuais");
assert.ok(result.coverage.every((collection) => collection.status === "CONSULTED"));
for (const evidence of result.evidence) assert.match(evidence.sha256, /^[a-f0-9]{64}$/);
console.log(JSON.stringify({ evidenceCount: result.evidence.length,
  collections: result.coverage.map(({ collection, documentCount }) => ({ collection, documentCount })) }));
