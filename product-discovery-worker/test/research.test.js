import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  analyzeSearchResults,
  buildSearchQueries,
  normalizeBraveResponse,
  normalizeSerpApiResponse,
  normalizeTavilyResponse,
  resolveSearchConfig,
  searchInternet,
  SEARCH_PROVIDERS,
} from "../src/research.js";

test("buildSearchQueries creates pain-oriented queries", () => {
  const queries = buildSearchQueries({
    theme: "mulheres que compram roupa online",
    targetAudience: "mulheres 30+",
  });

  assert.ok(queries.length >= 10);
  assert.ok(
    queries.includes("comprei roupa online e ficou ruim no corpo"),
    "deve pesquisar linguagem real de arrependimento de compra",
  );
  assert.ok(
    queries.some((query) => query.includes("não consigo resolver")),
    "deve manter fallback de dor em primeira pessoa para temas futuros",
  );
  assert.ok(
    queries.some((query) => query.includes("30 anos ou mais")),
    "deve normalizar público com 30+ para termo aceito pelo provedor",
  );
  assert.ok(
    queries.every((query) => !query.includes("30+")),
    "nenhuma query deve preservar 30+ cru",
  );
});

test("buildSearchQueries uses domain language for style and routine cycles", () => {
  const styleQueries = buildSearchQueries({
    theme: "PDE diagnóstico de estilo acessível",
    targetAudience: "mulheres 30+ imagem pessoal",
  });
  const routineQueries = buildSearchQueries({
    theme: "PDE montador de looks para rotina real",
    targetAudience: "mulheres 30+ rotina corrida",
  });

  assert.ok(styleQueries.includes("consultoria de estilo é cara"));
  assert.ok(styleQueries.includes("me sinto apagada com minhas roupas"));
  assert.ok(routineQueries.includes("não sei montar looks para trabalhar"));
  assert.ok(routineQueries.includes("guarda roupa cheio e nada para vestir"));
});

test("analyzeSearchResults approves strong non-sensitive PDE opportunity", () => {
  const report = analyzeSearchResults(
    {
      theme: "mulheres que compram roupa online",
      targetAudience: "mulheres 30+",
    },
    [
      {
        title: "Dificuldade para escolher roupa online",
        url: "https://forum.example/a",
        snippet: "problema insegurança caro complicado como fazer",
      },
      {
        title: "Review de consultoria de estilo",
        url: "https://reviews.example/b",
        snippet: "demorado confuso não resolve reclamação",
      },
      {
        title: "Perguntas frequentes sobre estilo",
        url: "https://questions.example/c",
        snippet: "não consigo decidir e tenho medo de errar",
      },
    ],
  );

  assert.equal(report.opportunities.length, 1);
  assert.equal(report.opportunities[0].decision, "APPROVE");
  assert.ok(report.opportunities[0].score >= 70);
});

test("resolveSearchConfig prefers Brave when key is available", () => {
  const config = resolveSearchConfig({
    BRAVE_SEARCH_API_KEY: "brave-test-key",
    TAVILY_API_KEY: "tavily-test-key",
  });

  assert.equal(config.provider, SEARCH_PROVIDERS.BRAVE);
  assert.equal(config.braveApiKey, "brave-test-key");
});

test("resolveSearchConfig reads Brave key from file when env value is absent", () => {
  const dir = mkdtempSync(join(tmpdir(), "product-discovery-worker-"));
  const keyPath = join(dir, "brave_api_key");
  writeFileSync(keyPath, "brave-file-key\n", "utf8");

  try {
    const config = resolveSearchConfig({
      PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
      BRAVE_SEARCH_API_KEY_FILE: keyPath,
    });

    assert.equal(config.provider, SEARCH_PROVIDERS.BRAVE);
    assert.equal(config.braveApiKey, "brave-file-key");
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("resolveSearchConfig selects Brave automatically when only key file is available", () => {
  const dir = mkdtempSync(join(tmpdir(), "product-discovery-worker-"));
  const keyPath = join(dir, "brave_api_key");
  writeFileSync(keyPath, "brave-file-key\n", "utf8");

  try {
    const config = resolveSearchConfig({
      BRAVE_SEARCH_API_KEY_FILE: keyPath,
    });

    assert.equal(config.provider, SEARCH_PROVIDERS.BRAVE);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("normalizeBraveResponse maps web results to public evidence", () => {
  const results = normalizeBraveResponse({
    web: {
      results: [
        {
          title: "Dificuldade para escolher roupa",
          url: "https://forum.example/roupa",
          description: "problema caro e complicado",
        },
      ],
    },
  });

  assert.deepEqual(results, [
    {
      title: "Dificuldade para escolher roupa",
      url: "https://forum.example/roupa",
      snippet: "problema caro e complicado",
    },
  ]);
});

test("normalizeTavilyResponse maps agent search results to public evidence", () => {
  const results = normalizeTavilyResponse({
    results: [
      {
        title: "Review de consultoria",
        url: "https://reviews.example/consultoria",
        content: "demorado confuso não resolve",
      },
    ],
  });

  assert.equal(results[0].title, "Review de consultoria");
  assert.equal(results[0].snippet, "demorado confuso não resolve");
});

test("normalizeSerpApiResponse maps organic results to public evidence", () => {
  const results = normalizeSerpApiResponse({
    organic_results: [
      {
        title: "Perguntas sobre estilo",
        link: "https://questions.example/estilo",
        snippet: "não consigo decidir e tenho medo de errar",
      },
    ],
  });

  assert.equal(results[0].url, "https://questions.example/estilo");
  assert.match(results[0].snippet, /não consigo/);
});

test("searchInternet calls configured Brave API and deduplicates results", async () => {
  const calls = [];
  const results = await searchInternet(
    {
      theme: "mulheres que compram roupa online",
      targetAudience: "mulheres 30+",
    },
    {
      maxSearchResults: 3,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url, options) => {
        calls.push({ url, options });
        return {
          ok: true,
          json: async () => ({
            web: {
              results: [
                {
                  title: "Dificuldade para escolher roupa",
                  url: "https://forum.example/roupa",
                  description: "problema caro e complicado",
                },
                {
                  title: "Dificuldade para escolher roupa",
                  url: "https://forum.example/roupa",
                  description: "problema caro e complicado",
                },
              ],
            },
          }),
        };
      },
    },
  );

  assert.equal(calls.length, 2);
  assert.match(calls[0].url, /api\.search\.brave\.com/);
  assert.equal(calls[0].options.headers["X-Subscription-Token"], "brave-test-key");
  assert.equal(results.length, 1);
});

test("searchInternet falls back when search provider rejects every query", async () => {
  const results = await searchInternet(
    {
      theme: "diagnostico de estilo acessivel",
      targetAudience: "mulheres 30+ estilo imagem pessoal",
    },
    {
      maxSearchResults: 3,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { warn() {}, info() {} },
      fetchFn: async () => ({
        ok: false,
        status: 422,
        json: async () => ({}),
      }),
    },
  );

  assert.equal(results.length, 1);
  assert.match(results[0].title, /Pesquisa inicial/);
});
