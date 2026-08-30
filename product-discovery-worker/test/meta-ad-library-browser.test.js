import assert from "node:assert/strict";
import test from "node:test";
import { chromium } from "playwright-core";
import {
  classifyVisibleMetaLibrary,
  collectPublicMetaAdLibrary,
  parseMetaAdLibraryCard,
  prepareMetaAdLibraryUrl,
  readVisibleMetaCards,
} from "../src/meta-ad-library-browser.js";

const FILTER_TEXT = `
Plataforma: Instagram
Status online: Anúncios ativos
Brasil
`;

test("força a fonte oficial e os filtros Brasil, Instagram e ativos", () => {
  const prepared = new URL(
    prepareMetaAdLibraryUrl(
      "https://www.facebook.com/ads/library/?q=guarda+roupa+capsula",
      "BR",
      "INSTAGRAM",
    ),
  );

  assert.equal(prepared.hostname, "www.facebook.com");
  assert.equal(prepared.pathname, "/ads/library/");
  assert.equal(prepared.searchParams.get("active_status"), "active");
  assert.equal(prepared.searchParams.get("country"), "BR");
  assert.deepEqual(
    prepared.searchParams.getAll("publisher_platforms[0]"),
    ["instagram"],
  );
  assert.throws(
    () =>
      prepareMetaAdLibraryUrl(
        "https://example.com/ads/library/?q=mercado",
        "BR",
        "INSTAGRAM",
      ),
    /não pertence/,
  );
});

test("só reconhece vazio quando os três filtros estão visíveis", () => {
  assert.deepEqual(
    classifyVisibleMetaLibrary(`${FILTER_TEXT}\nNenhum anúncio corresponde à sua pesquisa`),
    { state: "EMPTY", filtersConfirmed: true, cardCount: 0 },
  );
  assert.equal(
    classifyVisibleMetaLibrary("Nenhum anúncio corresponde à sua pesquisa").state,
    "PENDING",
  );
  assert.equal(
    classifyVisibleMetaLibrary("Faça login para continuar").state,
    "BLOCKED",
  );
});

test("extrai cards pela interface renderizada em Chromium real", async (t) => {
  const executablePath =
    process.env.ARGOS_META_BROWSER_EXECUTABLE_PATH ||
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    process.env.CHROMIUM_BIN ||
    "/usr/bin/chromium";
  const browser = await chromium.launch({ headless: true, executablePath });
  t.after(() => browser.close());
  const page = await browser.newPage();
  await page.setContent(`
    <main>
      <div>Plataforma: Instagram</div><div>Status online: Anúncios ativos</div><div>Brasil</div>
      <article>
        <div>Ativo</div><div>Identificação da biblioteca: 987654321</div>
        <section data-testid="ad-library-ad-carousel-container">
          <div>Marca Exemplo</div><div>Patrocinado</div>
          <div>Seu guarda-roupa cápsula em 7 dias por R$ 49.</div>
          <a href="https://l.facebook.com/l.php?u=https%3A%2F%2Fmarca.example%2Foferta">Comprar agora</a>
          <img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==">
        </section>
      </article>
    </main>
  `);

  const snapshot = await readVisibleMetaCards(page, 12);
  assert.equal(snapshot.cards.length, 1);
  const observation = parseMetaAdLibraryCard(snapshot.cards[0]);
  assert.equal(observation.metaAdId, "987654321");
  assert.equal(observation.advertiserName, "Marca Exemplo");
  assert.equal(observation.destinationUrl, "https://marca.example/oferta");
  assert.equal(observation.commercialSignal, true);
  assert.deepEqual(observation.publisherPlatforms, ["INSTAGRAM"]);
});

test("converte bloqueio do browser em fallback humano sem fabricar ausência", async () => {
  let closed = false;
  const page = fakePage("Faça login para continuar", 403);
  const result = await collectPublicMetaAdLibrary(
    {
      cycleId: 42,
      investigationId: 91,
      searchUrl: "https://www.facebook.com/ads/library/?q=viagem+solo",
      country: "BR",
      publisherPlatform: "INSTAGRAM",
    },
    {
      chromium: {
        async launch() {
          return {
            async newContext() {
              return { async newPage() { return page; } };
            },
            async close() { closed = true; },
          };
        },
      },
      delay: async () => {},
      timeoutMs: 1_000,
      now: sequentialClock(),
      logger: silentLogger(),
    },
  );

  assert.equal(result.outcome, "FALLBACK_REQUIRED");
  assert.match(result.errorMessage, /verificação|login|bloqueou/);
  assert.equal(result.observations.length, 0);
  assert.equal(closed, true);
});

test("aplica o menor limite entre ambiente e pedido do ciclo", async () => {
  const page = {
    ...fakePage(`${FILTER_TEXT}\nIdentificação da biblioteca: 1`, 403),
    async evaluate(_fn, limit) {
      assert.equal(limit, 2);
      return { cards: [] };
    },
  };
  const result = await collectPublicMetaAdLibrary(
    {
      cycleId: 43,
      investigationId: 92,
      searchUrl: "https://www.facebook.com/ads/library/?q=moda+madura",
      country: "BR",
      publisherPlatform: "INSTAGRAM",
      maxAds: 2,
    },
    {
      maxAds: 12,
      chromium: {
        async launch() {
          return {
            async newContext() {
              return { async newPage() { return page; } };
            },
            async close() {},
          };
        },
      },
      delay: async () => {},
      timeoutMs: 1_000,
      now: sequentialClock(),
      logger: silentLogger(),
    },
  );

  assert.equal(result.outcome, "FALLBACK_REQUIRED");
});

function fakePage(body, status) {
  return {
    async route() {},
    async goto() { return { status: () => status }; },
    locator() { return { async innerText() { return body; } }; },
    async title() { return "Biblioteca de Anúncios"; },
  };
}

function sequentialClock() {
  const values = [
    new Date("2026-08-30T12:00:00Z"),
    new Date("2026-08-30T12:00:01Z"),
  ];
  return () => values.shift() || values.at(-1);
}

function silentLogger() {
  return { info() {}, warn() {}, error() {} };
}
