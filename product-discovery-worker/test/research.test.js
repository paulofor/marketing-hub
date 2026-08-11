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
    queries.some((query) => query.includes("comentário de consumidor")),
    "deve buscar linguagem de consumidor além de termos genéricos de mercado",
  );
  assert.ok(
    queries.some((query) => query.includes("scientific study mechanism")),
    "deve pesquisar artigos cientificos que apoiem o mecanismo",
  );
  assert.ok(
    queries.some((query) => query.includes("site:pubmed.ncbi.nlm.nih.gov")),
    "deve incluir fontes cientificas candidatas na pesquisa",
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

test("buildSearchQueries expands public sources and commercial language for MEI WhatsApp", () => {
  const queries = buildSearchQueries({
    theme: "MEI que sabe executar mas trava para vender pelo WhatsApp",
    targetAudience: "autônomos e prestadores de serviço",
  });

  assert.ok(queries.length >= 30);
  assert.ok(
    queries.includes("MEI não sabe vender pelo WhatsApp"),
    "deve buscar a dor específica de venda pelo WhatsApp",
  );
  assert.ok(
    queries.includes("autônomo cliente pergunta preço e some"),
    "deve buscar sinal de perda comercial no atendimento",
  );
  assert.ok(
    queries.some((query) => query.includes("site:reclameaqui.com.br")),
    "deve ampliar busca para fontes públicas de reclamação",
  );
  assert.ok(
    queries.some((query) => query.includes("site:youtube.com comentários")),
    "deve ampliar busca para linguagem de comentários",
  );
  assert.ok(
    queries.some((query) => query.includes("objeção preço")),
    "deve buscar objeções comerciais antes de decidir oferta",
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
      {
        title: "Behavior change intervention for online decision support",
        url: "https://pubmed.ncbi.nlm.nih.gov/123456/",
        snippet: "systematic review evidence based intervention",
      },
    ],
  );

  assert.equal(report.opportunities.length, 3);
  assert.equal(report.opportunities[0].decision, "APPROVE");
  assert.ok(report.opportunities[0].score >= 70);
  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.equal(evidence.scientificArticles.length, 1);
  assert.deepEqual(Object.keys(evidence.scientificArticles[0]), [
    "link",
    "originalTitle",
    "portugueseTitle",
    "summary",
    "mechanismApplication",
  ]);
});

test("analyzeSearchResults requires scientific articles before approving mechanism", () => {
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

  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
  assert.match(
    report.opportunities[0].commercialRisk,
    /[Ss]em sustentação científica.*mecanismo/,
  );
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
      minSearchQueries: 4,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url, options) => {
        calls.push({ url, options });
        const callNumber = calls.length;
        return {
          ok: true,
          json: async () => ({
            web: {
              results: [
                {
                  title: "Dificuldade para escolher roupa",
                  url: `https://forum.example/roupa-${callNumber}`,
                  description: "problema caro e complicado",
                },
                {
                  title: "Dificuldade para escolher roupa",
                  url: `https://forum.example/roupa-${callNumber}`,
                  description: "problema caro e complicado",
                },
              ],
            },
          }),
        };
      },
    },
  );

  assert.equal(calls.length, 4);
  assert.match(calls[0].url, /api\.search\.brave\.com/);
  assert.equal(
    calls[0].options.headers["X-Subscription-Token"],
    "brave-test-key",
  );
  assert.equal(results.length, 3);
});

test("searchInternet limits Brave query while preserving the research intent", async () => {
  const calls = [];
  const longAudience = Array.from(
    { length: 70 },
    (_, index) => `segmento${index}`,
  ).join(" ");

  await searchInternet(
    {
      theme: "plano adaptativo para concursos",
      targetAudience: longAudience,
    },
    {
      maxSearchResults: 1,
      minSearchQueries: 1,
      maxSearchQueries: 1,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url) => {
        calls.push(url);
        return { ok: true, json: async () => ({ web: { results: [] } }) };
      },
    },
  );

  const query = new URL(calls[0]).searchParams.get("q");
  assert.ok(Array.from(query).length <= 400);
  assert.ok(query.split(/\s+/).length <= 50);
  assert.match(query, /cliente pergunta preço e some$/);
});

test("searchInternet retries Brave 422 with the documented minimal contract", async () => {
  const calls = [];
  const warnings = [];

  const results = await searchInternet(
    { theme: "assistente de execução diária", targetAudience: "especialistas" },
    {
      maxSearchResults: 1,
      minSearchQueries: 1,
      maxSearchQueries: 1,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: {
        info() {},
        warn(...args) {
          warnings.push(args);
        },
      },
      fetchFn: async (url) => {
        calls.push(url);
        if (calls.length === 1) {
          return {
            ok: false,
            status: 422,
            text: async () =>
              JSON.stringify({ error: { detail: "invalid search_lang" } }),
          };
        }
        return {
          ok: true,
          json: async () => ({
            web: {
              results: [
                {
                  title: "Execução assistida por IA",
                  url: "https://evidence.example/execucao",
                  description: "review de uma solução paga",
                },
              ],
            },
          }),
        };
      },
    },
  );

  assert.equal(calls.length, 2);
  assert.deepEqual(
    [...new URL(calls[0]).searchParams.keys()],
    ["q", "country", "search_lang", "count"],
  );
  assert.deepEqual([...new URL(calls[1]).searchParams.keys()], ["q"]);
  assert.equal(results.length, 1);
  assert.ok(
    warnings.some((entry) =>
      entry.some((value) => String(value).includes("invalid search_lang")),
    ),
    "deve preservar o diagnóstico sanitizado do provider",
  );
});

test("searchInternet uses stronger default search depth before stopping", async () => {
  const calls = [];
  const results = await searchInternet(
    {
      theme: "MEI que sabe executar mas trava para vender pelo WhatsApp",
      targetAudience: "autônomos",
    },
    {
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url) => {
        calls.push(url);
        const callNumber = calls.length;
        return {
          ok: true,
          json: async () => ({
            web: {
              results: [
                {
                  title: `Sinal ${callNumber} A`,
                  url: `https://fonte${callNumber}a.example/sinal`,
                  description: "problema caro complicado",
                },
                {
                  title: `Sinal ${callNumber} B`,
                  url: `https://fonte${callNumber}b.example/sinal`,
                  description: "dificuldade confuso não consigo",
                },
                {
                  title: `Sinal ${callNumber} C`,
                  url: `https://fonte${callNumber}c.example/sinal`,
                  description: "reclamação demorado não resolve",
                },
              ],
            },
          }),
        };
      },
    },
  );

  assert.equal(calls.length, 6);
  assert.equal(results.length, 12);
  assert.ok(
    calls.some((url) =>
      new URL(url).searchParams
        .get("q")
        ?.includes("cliente pergunta preço e some"),
    ),
    "deve executar consultas comerciais específicas antes de parar",
  );
  assert.ok(
    calls.some((url) =>
      new URL(url).searchParams
        .get("q")
        ?.includes("scientific study mechanism"),
    ),
    "deve executar consulta científica antes de encerrar pelo volume de resultados",
  );
});

test("searchInternet fails operationally when every provider query fails", async () => {
  await assert.rejects(
    () =>
      searchInternet(
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
            text: async () => JSON.stringify({ error: { detail: "invalid" } }),
          }),
        },
      ),
    /Todas as consultas externas falharam; provider=brave; tentativas=14; status=422/,
  );
});

test("analyzeSearchResults blocks approval without commercial intent", () => {
  const report = analyzeSearchResults(
    { theme: "organização pessoal", targetAudience: "adultos" },
    [
      {
        title: "Dificuldade de organização",
        url: "https://forum.example/a",
        snippet: "problema confuso não consigo",
      },
      {
        title: "Relato sobre rotina",
        url: "https://community.example/b",
        snippet: "dificuldade demorado",
      },
      {
        title: "Behavior change systematic review",
        url: "https://pubmed.ncbi.nlm.nih.gov/42",
        snippet: "systematic review intervention",
      },
    ],
  );
  assert.equal(report.opportunities.length, 3);
  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
  assert.match(report.opportunities[0].commercialRisk, /intenção de compra/);
});

test("scientific and commercial queries are inside the operational query limit", () => {
  const queries = buildSearchQueries({
    theme: "tema novo",
    targetAudience: "público",
  }).slice(0, 14);
  assert.ok(
    queries.some((query) => query.includes("scientific study mechanism")),
  );
  assert.ok(
    queries.some(
      (query) => query.includes("preço") || query.includes("resposta pronta"),
    ),
  );
});
