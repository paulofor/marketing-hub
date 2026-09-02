import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  analyzeSearchResults,
  buildSearchQueries,
  extractPublicComparableOffers,
  normalizeBraveResponse,
  normalizeSerpApiResponse,
  normalizeTavilyResponse,
  resolveSearchConfig,
  searchInternet,
  SEARCH_PROVIDERS,
} from "../src/research.js";

function candidateBlueprints(count = 3) {
  return Array.from({ length: count }, (_, index) => ({
    name: `Situação factual ${index + 1}`,
    primaryAudience: "Pessoa física em situação concreta",
    purchaseSituation: "Prazo próximo e tentativa anterior insuficiente.",
    rootPain: "A pessoa não consegue concluir a tarefa com segurança.",
    practicalPain: "Alternativas atuais exigem comparação e montagem manual.",
    emotionalPain: "A incerteza aumenta o receio de errar.",
    observedLanguage: ["não consigo", "vale a pena"],
    currentAlternatives: ["conteúdo gratuito", "serviço pago"],
    residualEffort: "Pesquisar, comparar e montar a resposta.",
    scaleEvidence: "Sinais públicos em fontes independentes.",
    unmetnessEvidence: "Relatos de solução confusa e demorada.",
    pdeValueBoundary: "Reduzir organização e montagem sem escolher o formato.",
    pdeDeliveryFit: {
      deliveryMode: "AI_DIGITAL_EXPERIENCE",
      minimumInput: "Uma resposta curta e uma imagem opcional.",
      aiBackstageWork: "Organizar evidências e personalizar a orientação.",
      readyDigitalOutcome: "Orientação individual pronta para aplicar.",
      physicalDependency: "NONE",
    },
    instagramFitEvidence: "A cena possui contraste visual observável.",
    commercialRisk: "Intenção comercial ainda precisa de gate factual.",
    evidenceIds: ["P1", "P2"],
    maturity: "RESEARCHABLE",
  }));
}

test("schema exige duas ou três candidatas factuais", () => {
  const schema = JSON.parse(
    readFileSync(
      new URL(
        "../prompts/productdiscovery.v1/research/response-schema.json",
        import.meta.url,
      ),
      "utf8",
    ),
  );
  const prompt = readFileSync(
    new URL(
      "../prompts/productdiscovery.v1/research/user.md",
      import.meta.url,
    ),
    "utf8",
  );

  assert.equal(schema.properties.candidates.minItems, 2);
  assert.equal(schema.properties.candidates.maxItems, 3);
  assert.equal(schema.properties.candidates.items.properties.name.maxLength, 191);
  assert.equal(
    schema.properties.candidates.items.properties.primaryAudience.maxLength,
    191,
  );
  assert.equal(schema.properties.decisionSummary.maxLength, 1500);
  assert.equal(
    schema.properties.candidates.items.properties.currentAlternatives.maxItems,
    8,
  );
  assert.match(prompt, /duas ou três candidatas factuais distintas/);
  assert.match(prompt, /sem repetir a mesma evidência/i);
});

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

test("buildSearchQueries inclui pesquisa de canal no recorte B2C Instagram", () => {
  const queries = buildSearchQueries({
    theme: "entrevista de emprego",
    targetAudience: "pessoa física buscando recolocação",
    acquisitionChannel: "Instagram",
    commercialConstraints: "B2C e mobile",
  });

  assert.ok(
    queries.some((query) =>
      query.includes("anúncio Instagram Reel demonstração"),
    ),
  );
  assert.ok(
    queries.some((query) =>
      query.includes("aplicativo preço review pessoa física"),
    ),
  );
});

test("buildSearchQueries usa o foco amplo sem hipersegmentar toda busca comercial", () => {
  const themes = [
    [
      "Mulheres entre 35 e 60 anos foco em beleza e bem-estar.",
      "beleza e bem-estar",
    ],
    ["Homens entre 25 e 35 anos foco em finanças.", "finanças"],
    [
      "Mulheres entre 35 e 60 anos foco em relacionamentos.",
      "relacionamentos",
    ],
  ];

  for (const [theme, focus] of themes) {
    const queries = buildSearchQueries({
      theme,
      researchMode: "DISCOVER_MARKETS",
      marketType: "B2C",
      acquisitionChannel: "Instagram",
    });
    assert.ok(
      queries.some((query) => query === `${focus} curso online preço`),
    );
    assert.ok(
      queries.some((query) =>
        query.includes(`${focus} scientific study mechanism`),
      ),
    );
    assert.ok(queries.every((query) => !query.includes("anos foco em")));
  }
});

test("analyzeSearchResults não presume Instagram no ciclo B2C", () => {
  const report = analyzeSearchResults(
    {
      theme: "entrevista de emprego",
      targetAudience: "pessoa física buscando recolocação",
      acquisitionChannel: "Instagram",
      commercialConstraints: "B2C e mobile",
    },
    [
      {
        title: "Dificuldade em entrevistas",
        url: "https://forum.example/a",
        snippet: "problema insegurança caro complicado comprar curso review",
      },
      {
        title: "Interview training systematic review",
        url: "https://pubmed.ncbi.nlm.nih.gov/123456/",
        snippet: "systematic review interview training intervention",
      },
    ],
    null,
    { candidateBlueprints: candidateBlueprints() },
  );

  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
  assert.match(
    report.opportunities[0].commercialRisk,
    /cobertura Meta\/Instagram/i,
  );
  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.equal(evidence.instagramB2cRequired, true);
  assert.equal(evidence.instagramB2cGatePassed, false);
  assert.deepEqual(evidence.metaCoverage, []);
});

test("analyzeSearchResults vincula a cada candidata somente as evidências citadas", () => {
  const blueprints = candidateBlueprints(1);
  blueprints[0].evidenceIds = ["P1", "R1"];
  const repositoryEvidence = [
    {
      evidenceId: "R1",
      path: "pesquisas/mercados/climaterio.md",
      title: "Climatério e decisões de vestuário",
      excerpt: "Mudanças corporais alteram conforto e decisão de compra.",
    },
    {
      evidenceId: "R2",
      path: "pesquisas/mercados/viagem.md",
      title: "Viagem solo",
      excerpt: "Referência de outro mercado.",
    },
  ];
  const report = analyzeSearchResults(
    { theme: "guarda-roupa cápsula 40+" },
    [
      {
        evidenceId: "P1",
        title: "Dor de vestuário",
        url: "https://example.test/roupa",
        snippet: "Relato factual do esforço de escolha.",
      },
      {
        evidenceId: "P2",
        title: "Dor de viagem",
        url: "https://example.test/viagem",
        snippet: "Fonte de outro mercado.",
      },
    ],
    [],
    { candidateBlueprints: blueprints, repositoryEvidence },
  );

  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.equal(evidence.publicEvidence.length, 2);
  assert.equal(evidence.repositoryEvidence.length, 2);
  assert.deepEqual(
    evidence.referencedEvidence.publicEvidence.map((item) => item.evidenceId),
    ["P1"],
  );
  assert.deepEqual(
    evidence.referencedEvidence.repositoryEvidence.map(
      (item) => item.evidenceId,
    ),
    ["R1"],
  );
});

test("analyzeSearchResults aceita o gate de canal somente com cobertura Meta Instagram atual", () => {
  const job = {
    theme: "entrevista de emprego",
    targetAudience: "pessoa física buscando recolocação",
    acquisitionChannel: "Instagram",
    commercialConstraints: "B2C e mobile",
  };
  const results = [
    {
      title: "Dificuldade em entrevistas",
      url: "https://forum.example/a",
      snippet: "problema insegurança caro complicado comprar curso review",
    },
    {
      title: "Interview training systematic review",
      url: "https://pubmed.ncbi.nlm.nih.gov/123456/",
      snippet: "systematic review interview training intervention",
    },
  ];
  const offers = Array.from({ length: 10 }, (_, index) => ({
    marketplace: "HOTMART",
    referenceId: String(index),
    title: `Treino entrevista emprego ${index}`,
    url: `https://hotmart.com/${index}`,
    price: "R$ 29,00",
    collectedAt: "2026-08-25T00:00:00Z",
  }));
  const report = analyzeSearchResults(job, results, offers, {
    candidateBlueprints: candidateBlueprints(),
    minimumComparableOffers: 10,
    metaAdEvidence: [
      {
        referenceId: "ad-1",
        title: "Treino entrevista emprego",
        active: true,
        publisherPlatforms: ["INSTAGRAM"],
      },
    ],
    metaCoverage: [
      {
        publisherPlatform: "INSTAGRAM",
        sourceStatus: "OBSERVED",
        activeAds: 1,
        advertisersObserved: 1,
      },
    ],
    sourceEvaluatedAt: "2026-08-26T00:00:00Z",
  });

  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.equal(evidence.instagramB2cGatePassed, true);
  assert.equal(evidence.marketplaceOffers.length, 10);
  assert.equal(evidence.metaAdEvidence.length, 1);
  assert.equal(evidence.metaCoverage[0].advertisersObserved, 1);
  assert.equal(evidence.purchaseMomentGate.status, "WAITING_PRIVATE_PROTOTYPE");
  assert.equal(evidence.purchaseMomentGate.finalPrioritizationEligible, false);
  assert.ok(
    evidence.purchaseMomentGate.requiredObservedSignals.includes(
      "READY_RESULT_USED",
    ),
  );
  assert.equal(
    evidence.purchaseMomentGate.humanValueDeliveryRequirements
      .requiresManualAssembly,
    false,
  );
  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
});

test("analyzeSearchResults approves strong non-sensitive PDE opportunity", () => {
  const blueprints = candidateBlueprints();
  blueprints[0].maturity = "DOSSIER_READY";
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
    null,
    { candidateBlueprints: blueprints },
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
    null,
    { candidateBlueprints: candidateBlueprints() },
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

test("resolveSearchConfig accepts the sandbox Brave credential alias", () => {
  const config = resolveSearchConfig({
    BRAVE_API_KEY: "sandbox-brave-key",
  });

  assert.equal(config.provider, SEARCH_PROVIDERS.BRAVE);
  assert.equal(config.braveApiKey, "sandbox-brave-key");
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

test("extrai alternativas públicas pagas sem contar conteúdo editorial", () => {
  const offers = extractPublicComparableOffers([
    {
      title: "Gerador de propostas para prestadores",
      url: "https://produto-a.example/propostas",
      snippet: "Software com planos a partir de R$ 29 por mês.",
    },
    {
      title: "Outro plano do mesmo gerador",
      url: "https://produto-a.example/precos",
      snippet: "Plataforma de orçamento com assinatura mensal.",
    },
    {
      title: "App de orçamento profissional",
      url: "https://produto-b.example/",
      snippet: "Comece grátis e contrate o plano para criar propostas.",
    },
    {
      title: "Como fazer uma proposta comercial",
      url: "https://conteudo.example/blog/proposta",
      snippet: "Artigo com modelo gratuito e dicas.",
    },
    {
      title: "Planejador de orçamento pessoal",
      url: "https://financeiro.example/orcamento",
      snippet: "Aplicativo grátis para controlar despesas pessoais.",
    },
    {
      title: "Modelo de proposta comercial em PDF",
      url: "https://prefeitura.gov.br/modelo-proposta.pdf",
      snippet: "Documento público gratuito para orçamento.",
    },
  ]);

  assert.deepEqual(
    offers.map((offer) => offer.referenceId),
    ["produto-a.example", "produto-b.example"],
  );
  assert.equal(offers[0].marketplace, "PUBLIC_WEB");
  assert.equal(offers[0].price, "R$ 29");
  assert.match(offers[0].signalDisclaimer, /não vendas/);
});

test("conta apps oficiais por produto e não transforma notícia com preço em oferta", () => {
  const offers = extractPublicComparableOffers([
    {
      title: "Skin Bliss: Skincare Routines",
      url: "https://play.google.com/store/apps/details?id=com.getskinbliss.skinbliss&hl=pt",
      snippet: "Aplicativo com versão gratuita e recursos Premium por assinatura.",
    },
    {
      title: "Skinive: análise de pele",
      url: "https://play.google.com/store/apps/details?id=com.skinive.App&hl=pt_BR",
      snippet: "Opções gratuitas e Premium dentro do app.",
    },
    {
      title: "IA Derma: Exame da Pele",
      url: "https://apps.apple.com/br/app/pele-digital-exame-ia-derma/id1633219654",
      snippet: "Recursos gratuitos e Premium com compras dentro do app.",
    },
    {
      title: "Beleza impacta autoestima e gasto chega a R$ 500",
      url: "https://jornal.example/cotidiano/2026/beleza-autoestima",
      snippet: "Matéria relata preços pagos em salões e consultorias.",
    },
  ]);

  assert.deepEqual(
    offers.map((offer) => offer.referenceId),
    [
      "play.google.com:com.getskinbliss.skinbliss",
      "play.google.com:com.skinive.App",
      "apps.apple.com:id1633219654",
    ],
  );
});

test("mecanismo de propostas exige ciência sobre decisão, clareza e confiança", () => {
  const report = analyzeSearchResults(
    {
      theme: "propostas e orçamentos para prestadores",
      targetAudience: "autônomos",
    },
    [
      {
        title: "Critérios de seleção e priorização de propostas de projetos",
        url: "https://researchgate.net/publication/1",
        snippet: "Artigo com propostas para selecionar projetos.",
      },
      {
        title: "Information overload and online purchase decision",
        url: "https://frontiersin.org/journals/psychology/articles/1/full",
        snippet:
          "Information clarity reduces cognitive load and purchase decision difficulty.",
      },
      {
        title: "Software de proposta preço",
        url: "https://produto.example/propostas",
        snippet:
          "Comprar plano por R$ 29 para resolver problema confuso e manual.",
      },
    ],
    null,
    { candidateBlueprints: candidateBlueprints() },
  );

  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.deepEqual(
    evidence.scientificArticles.map((article) => article.originalTitle),
    ["Information overload and online purchase decision"],
  );
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
  assert.match(query, /curso online preço$/);
});

test("searchInternet sends the canonical Brazilian locale accepted by Brave", async () => {
  const calls = [];

  await searchInternet(
    {
      theme: "solução pronta de IA",
      targetAudience: "profissionais brasileiros",
    },
    {
      maxSearchResults: 1,
      minSearchQueries: 1,
      maxSearchQueries: 1,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        PRODUCT_DISCOVERY_SEARCH_LANGUAGE: "pt-br",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url) => {
        calls.push(url);
        return { ok: true, json: async () => ({ web: { results: [] } }) };
      },
    },
  );

  assert.equal(new URL(calls[0]).searchParams.get("search_lang"), "pt-br");
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
        ?.includes("curso online preço"),
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

test("descoberta ampla preserva ofertas e ciência antes de encerrar por volume", async () => {
  const calls = [];
  const results = await searchInternet(
    {
      theme: "beleza e bem-estar",
      targetAudience: "mulheres de 35 a 60 anos",
      researchMode: "DISCOVER_MARKETS",
      marketType: "B2C",
      acquisitionChannel: "Instagram",
    },
    {
      maxSearchResults: 6,
      minSearchQueries: 2,
      maxSearchQueries: 8,
      maxResultsPerQuery: 3,
      minimumPublicComparableOffers: 4,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url) => {
        calls.push(url);
        const query = new URL(url).searchParams.get("q");
        const callNumber = calls.length;
        if (/scientific study mechanism/.test(query)) {
          return {
            ok: true,
            json: async () => ({
              web: {
                results: [
                  {
                    title: "Systematic review of wellbeing intervention",
                    url: "https://pubmed.ncbi.nlm.nih.gov/12345/",
                    description: "Peer reviewed systematic review intervention.",
                  },
                ],
              },
            }),
          };
        }
        return {
          ok: true,
          json: async () => ({
            web: {
              results: [
                {
                  title: `Programa online de bem-estar ${callNumber}`,
                  url: `https://oferta${callNumber}.example/curso/programa`,
                  description: `Curso online por R$ ${40 + callNumber},00; compre agora.`,
                },
                {
                  title: `Relato público ${callNumber}`,
                  url: `https://relato${callNumber}.example/post`,
                  description: "Dificuldade, insegurança e alternativa manual.",
                },
              ],
            },
          }),
        };
      },
    },
  );

  assert.equal(calls.length, 5);
  assert.equal(extractPublicComparableOffers(results).length, 4);
  assert.ok(
    results.some((item) => item.url.includes("pubmed.ncbi.nlm.nih.gov")),
  );
});

test("descoberta ampla não encerra com apenas nove ofertas públicas", async () => {
  const calls = [];
  const offers = Array.from({ length: 9 }, (_, index) => ({
    title: `Programa digital ${index + 1}`,
    url: `https://oferta${index + 1}.example/curso/programa`,
    description: `Curso online por R$ ${49 + index},00; compre agora.`,
  }));
  const science = {
    title: "Systematic review of decision support",
    url: "https://pubmed.ncbi.nlm.nih.gov/99999/",
    description: "Peer reviewed systematic review intervention.",
  };

  await searchInternet(
    {
      theme: "finanças",
      researchMode: "DISCOVER_MARKETS",
      marketType: "B2C",
      acquisitionChannel: "Instagram",
    },
    {
      maxSearchResults: 5,
      minSearchQueries: 2,
      maxSearchQueries: 5,
      maxResultsPerQuery: 10,
      config: resolveSearchConfig({
        PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
        BRAVE_SEARCH_API_KEY: "brave-test-key",
      }),
      logger: { info() {} },
      fetchFn: async (url) => {
        calls.push(url);
        return {
          ok: true,
          json: async () => ({ web: { results: [...offers, science] } }),
        };
      },
    },
  );

  assert.equal(calls.length, 5);
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
    null,
    { candidateBlueprints: candidateBlueprints() },
  );
  assert.equal(report.opportunities.length, 3);
  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
  assert.match(report.opportunities[0].commercialRisk, /intenção de compra/);
});

test("analyzeSearchResults preserva candidatas imaturas sem aprová-las", () => {
  const report = analyzeSearchResults(
    { theme: "leads", targetAudience: "manicures" },
    [
      {
        title: "Preço curso leads",
        url: "https://example.com/a",
        snippet: "curso comprar preço",
      },
      {
        title: "Estudo",
        url: "https://pubmed.ncbi.nlm.nih.gov/1",
        snippet: "scientific study",
      },
    ],
    Array.from({ length: 9 }, (_, index) => ({
      marketplace: "HOTMART",
      referenceId: String(index),
      title: `Oferta ${index}`,
      url: `https://hotmart.com/${index}`,
    })),
    { candidateBlueprints: candidateBlueprints() },
  );
  assert.match(report.decisionSummary, /9 ofertas comparáveis/);
  assert.equal(report.opportunities.length, 3);
  assert.ok(
    report.opportunities.every(
      (opportunity) => opportunity.decision === "RESEARCH_MORE",
    ),
  );
  assert.match(report.decisionSummary, /Maturidade factual: RESEARCH_MORE/);
});

test("analyzeSearchResults rebaixa dossiê declarado pronto sem ofertas e Meta", () => {
  const blueprints = candidateBlueprints();
  blueprints[0].maturity = "DOSSIER_READY";

  const report = analyzeSearchResults(
    {
      theme: "rotina visual de mulheres 40+",
      targetAudience: "mulheres 40+",
      acquisitionChannel: "Instagram",
      marketType: "B2C",
    },
    [
      {
        evidenceId: "P1",
        title: "Dor observada",
        url: "https://forum.example/dor",
        snippet: "não consigo resolver e procuro uma alternativa paga",
      },
      {
        evidenceId: "P2",
        title: "Tentativa insuficiente",
        url: "https://reviews.example/tentativa",
        snippet: "a solução é confusa, cara e demorada",
      },
    ],
    [],
    {
      candidateBlueprints: blueprints,
      metaAdEvidence: [],
      metaCoverage: [],
    },
  );

  assert.equal(report.opportunities[0].maturity, "RESEARCHABLE");
  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
});

test("analyzeSearchResults explica falha Meta sem expor códigos técnicos no relatório", () => {
  const report = analyzeSearchResults(
    {
      theme: "beleza e bem-estar",
      targetAudience: "mulheres 40+",
      acquisitionChannel: "Instagram",
      marketType: "B2C",
    },
    [
      {
        title: "Relato público",
        url: "https://instagram.com/exemplo",
        snippet: "dificuldade e alternativa paga",
      },
    ],
    [],
    {
      candidateBlueprints: candidateBlueprints(),
      metaCoverage: [
        { sourceStatus: "UNAVAILABLE" },
        { sourceStatus: "UNAVAILABLE" },
        { sourceStatus: "UNAVAILABLE" },
      ],
    },
  );

  assert.match(
    report.decisionSummary,
    /cobertura Meta\/Instagram não executada em 3 tentativa\(s\) por falha de integração/,
  );
  assert.doesNotMatch(report.decisionSummary, /UNAVAILABLE/);
  assert.doesNotMatch(report.opportunities[0].commercialRisk, /UNAVAILABLE/);
});

test("analyzeSearchResults exige revisão humana sem liberar dossiê sensível", () => {
  const blueprints = candidateBlueprints();
  blueprints[0].maturity = "DOSSIER_READY";
  blueprints[0].pdeDeliveryFit.readyDigitalOutcome =
    "Diagnóstico médico individual pronto para seguir.";
  const offers = Array.from({ length: 10 }, (_, index) => ({
    marketplace: "HOTMART",
    referenceId: String(index),
    title: `Oferta sensível ${index}`,
    url: `https://hotmart.com/sensivel-${index}`,
  }));

  const report = analyzeSearchResults(
    {
      theme: "aconselhamento médico para mulheres",
      targetAudience: "mulheres 40+",
      acquisitionChannel: "Instagram",
      marketType: "B2C",
    },
    [
      {
        evidenceId: "P1",
        title: "Risco de diagnóstico",
        url: "https://forum.example/diagnostico",
        snippet: "diagnóstico médico caro e difícil",
      },
      {
        evidenceId: "P2",
        title: "Revisão científica",
        url: "https://pubmed.ncbi.nlm.nih.gov/42",
        snippet: "systematic review intervention",
      },
    ],
    offers,
    {
      candidateBlueprints: blueprints,
      minimumComparableOffers: 10,
      metaAdEvidence: [
        {
          referenceId: "ad-sensitive-1",
          title: "Oferta sensível",
          active: true,
          publisherPlatforms: ["INSTAGRAM"],
        },
      ],
      metaCoverage: [
        {
          publisherPlatform: "INSTAGRAM",
          sourceStatus: "OBSERVED",
          activeAds: 1,
        },
      ],
    },
  );

  assert.equal(report.opportunities[0].decision, "HUMAN_REVIEW");
  assert.equal(report.opportunities[0].maturity, "HUMAN_REVIEW");
});

test("analyzeSearchResults promove somente candidata segura com evidência própria", () => {
  const blueprints = candidateBlueprints();
  blueprints[0].evidenceIds = ["P1", "P2", "O1", "M1"];
  blueprints[1].evidenceIds = ["P1", "P2", "O2"];
  blueprints[2].evidenceIds = ["P1", "P2", "O3", "M1"];
  blueprints[2].pdeDeliveryFit.readyDigitalOutcome =
    "Recomendação de investimento com retorno garantido.";
  const offers = Array.from({ length: 10 }, (_, index) => ({
    evidenceId: `O${index + 1}`,
    marketplace: "HOTMART",
    referenceId: String(index),
    title: `Alternativa paga ${index}`,
    url: `https://hotmart.com/alternativa-${index}`,
    price: "R$ 49,00",
    collectedAt: "2026-09-01T00:00:00Z",
  }));

  const report = analyzeSearchResults(
    {
      theme: "beleza e bem-estar",
      targetAudience: "mulheres de 35 a 60 anos",
      acquisitionChannel: "Instagram",
      marketType: "B2C",
      researchMode: "DISCOVER_MARKETS",
    },
    [
      {
        evidenceId: "P1",
        title: "Situação de compra observada",
        url: "https://relatos.example/situacao",
        snippet:
          "Problema caro e complicado; consumidor procura comprar alternativa pronta.",
      },
      {
        evidenceId: "P2",
        title: "Comparação pública independente",
        url: "https://reviews.example/comparacao",
        snippet: "Review de curso pago que não resolve e exige montagem manual.",
      },
      {
        evidenceId: "P3",
        title: "Revisão sobre mecanismo e tratamento médico",
        url: "https://pubmed.ncbi.nlm.nih.gov/123456/",
        snippet:
          "Systematic review que também descreve limites de tratamento médico.",
      },
    ],
    offers,
    {
      candidateBlueprints: blueprints,
      minimumComparableOffers: 10,
      metaAdEvidence: [
        {
          evidenceId: "M1",
          referenceId: "ad-1",
          title: "Alternativa digital observada",
          active: true,
          publisherPlatforms: ["INSTAGRAM"],
        },
      ],
      metaCoverage: [
        {
          publisherPlatform: "INSTAGRAM",
          sourceStatus: "OBSERVED",
          activeAds: 1,
          advertisersObserved: 1,
        },
      ],
      sourceEvaluatedAt: "2026-09-02T00:00:00Z",
    },
  );

  assert.equal(report.opportunities[0].maturity, "DOSSIER_READY");
  assert.equal(report.opportunities[0].decision, "RESEARCH_MORE");
  assert.equal(report.opportunities[1].maturity, "RESEARCHABLE");
  assert.equal(report.opportunities[2].maturity, "HUMAN_REVIEW");
  assert.equal(report.opportunities[2].decision, "HUMAN_REVIEW");
  assert.match(report.decisionSummary, /1 DOSSIER_READY/);
  const evidence = JSON.parse(report.opportunities[0].evidenceJson);
  assert.equal(evidence.candidateReadiness.independentPublicPaths, 2);
  assert.equal(evidence.candidateReadiness.referencedComparableOffers, 1);
  assert.equal(evidence.candidateReadiness.referencedActiveInstagramAds, 1);
  assert.equal(evidence.candidateReadiness.highRiskHits, 0);
  assert.ok(
    evidence.publicEvidence.some((item) =>
      /tratamento médico/i.test(item.snippet),
    ),
  );
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
  assert.ok(
    queries.some(
      (query) =>
        query.includes("site:apps.apple.com") ||
        query.includes("site:play.google.com/store/apps"),
    ),
  );
});

test("plano dirigido extenso não elimina pesquisa científica e comercial", () => {
  const queries = buildSearchQueries({
    theme: "propostas comerciais para prestadores",
    targetAudience: "prestadores locais",
    directedQueries: Array.from(
      { length: 16 },
      (_, index) => `consulta dirigida ${index}`,
    ),
  }).slice(0, 14);

  assert.ok(
    queries.some(
      (query) =>
        query.includes("purchase intention") ||
        query.includes("purchase decision") ||
        query.includes("decision support"),
    ),
  );
  assert.ok(
    queries.some(
      (query) => query.includes("preço") || query.includes("resposta pronta"),
    ),
  );
});
