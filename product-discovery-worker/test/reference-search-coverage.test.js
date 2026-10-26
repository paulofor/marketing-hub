import test from "node:test";
import assert from "node:assert/strict";
import {
  buildSearchQueries,
  searchInternet,
  resolveSearchConfig,
} from "../src/research.js";
import { readFile } from "node:fs/promises";
const realPlan = JSON.parse(
  await readFile(
    new URL("./fixtures/research-plan-replay.json", import.meta.url),
    "utf8",
  ),
);
for (const domains of [
  ["whering.co", "myindyx.com"],
  ["alpha.example", "beta.example"],
]) {
  test(`fontes indicadas ${domains.join(",")} chegam à síntese mesmo com limite de resultados`, async () => {
    const job = {
      theme: "Modelagem de experiência própria",
      targetAudience: "Preferências pessoais",
      referenceSources: domains.map((d) => `https://www.${d}/`).join("\n"),
      directedQueries: realPlan.publicQueries,
    };
    const queries = buildSearchQueries(job);
    for (const domain of domains)
      assert.ok(
        queries.slice(0, 2).some((q) => q.includes(domain)),
        `Referência adiada: ${domain}`,
      );
    const calls = [];
    const results = await searchInternet(job, {
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "synthetic-test-only",
      }),
      maxSearchResults: 4,
      minSearchQueries: 4,
      maxSearchQueries: 12,
      maxResultsPerQuery: 5,
      logger: { info() {}, warn() {} },
      fetchFn: async (url) => {
        const q = new URL(url).searchParams.get("q");
        calls.push(q);
        const domain = domains.find((d) => q.includes(d));
        const items = domain
          ? [
              {
                title: "Referência internacional de teste",
                url: `https://${domain}/`,
                description: "Descrição sintética sem prova comercial.",
              },
            ]
          : [];
        for (let i = 0; i < 5; i++)
          items.push({
            title: "Curso online de teste",
            url: `https://oferta${calls.length}-${i}.example/curso`,
            description: "R$ 79,00 compre agora.",
          });
        return { ok: true, json: async () => ({ web: { results: items } }) };
      },
    });
    for (const domain of domains)
      assert.ok(
        results.some((r) => new URL(r.url).hostname === domain),
        `Fonte descartada: ${domain}`,
      );
    assert.ok(
      results.some((r) => r.url.includes("oferta")),
      "Ofertas continuam presentes",
    );
    assert.ok(calls.length <= 12, "Preserva teto de consultas");
  });
}

test("uma referência indisponível não é substituída por afirmação de sucesso", async () => {
  const results = await searchInternet(
    { theme: "Pesquisa", referenceSources: "https://ausente.example/" },
    {
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "synthetic-test-only",
      }),
      maxSearchQueries: 2,
      minSearchQueries: 2,
      maxSearchResults: 2,
      logger: { info() {}, warn() {} },
      fetchFn: async () => ({
        ok: true,
        json: async () => ({ web: { results: [] } }),
      }),
    },
  );
  assert.deepEqual(results, []);
});
