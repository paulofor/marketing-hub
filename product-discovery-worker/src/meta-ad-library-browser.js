import { chromium as defaultChromium } from "playwright-core";

const ALLOWED_HOSTS = new Set(["www.facebook.com", "business.facebook.com"]);
const LIBRARY_PATH = "/ads/library/";
const DEFAULT_TIMEOUT_MS = 45_000;
const DEFAULT_MAX_ADS = 12;
const DEFAULT_POLL_INTERVAL_MS = 500;

/** Observa uma única consulta pública da Meta sem login, cookies persistentes ou evasão. */
export async function collectPublicMetaAdLibrary(
  request,
  options = {},
) {
  const startedAt = currentDate(options).toISOString();
  const enabled =
    String(options.enabled ?? process.env.ARGOS_META_BROWSER_ENABLED ?? "true") ===
    "true";
  const searchUrl = prepareMetaAdLibraryUrl(
    request.searchUrl,
    request.country,
    request.publisherPlatform,
  );
  const logger = options.logger || console;
  const configuredMaxAds = clamp(
    Number(
      options.maxAds ||
        process.env.ARGOS_META_BROWSER_MAX_ADS ||
        DEFAULT_MAX_ADS,
    ),
    1,
    25,
  );
  const requestedMaxAds = clamp(Number(request.maxAds || configuredMaxAds), 1, 25);
  const maxAds = Math.min(configuredMaxAds, requestedMaxAds);
  const timeoutMs = clamp(
    Number(
      options.timeoutMs ||
        process.env.ARGOS_META_BROWSER_TIMEOUT_MS ||
        DEFAULT_TIMEOUT_MS,
    ),
    1_000,
    120_000,
  );
  const baseResult = {
    searchUrl: request.searchUrl,
    startedAt,
    finishedAt: startedAt,
    httpStatus: null,
    pageTitle: null,
    platformFilterConfirmed: false,
    observations: [],
  };
  if (!enabled) {
    return {
      ...baseResult,
      outcome: "FALLBACK_REQUIRED",
      errorMessage: "Navegador público de Argos está desabilitado.",
    };
  }

  logger.info?.(
    `[product-discovery-worker] Meta public browser request cycle=${request.cycleId} investigation=${request.investigationId} url=${searchUrl}`,
  );
  let browser;
  try {
    const chromium = options.chromium || defaultChromium;
    const executablePath =
      options.executablePath ||
      process.env.ARGOS_META_BROWSER_EXECUTABLE_PATH ||
      undefined;
    browser = await chromium.launch({
      headless: true,
      ...(executablePath ? { executablePath } : {}),
      args: ["--disable-dev-shm-usage"],
    });
    const context = await browser.newContext({
      locale: "pt-BR",
      timezoneId: "America/Sao_Paulo",
      viewport: { width: 1440, height: 1000 },
      serviceWorkers: "block",
    });
    const page = await context.newPage();
    await page.route("**/*", async (route) => {
      if (["font", "image", "media"].includes(route.request().resourceType())) {
        await route.abort();
        return;
      }
      await route.continue();
    });
    const response = await page.goto(searchUrl, {
      waitUntil: "domcontentloaded",
      timeout: timeoutMs,
    });
    const httpStatus = response?.status() ?? null;
    const visible = await waitForVisibleMetaOutcome(page, {
      timeoutMs,
      pollIntervalMs:
        options.pollIntervalMs || DEFAULT_POLL_INTERVAL_MS,
      delay: options.delay,
    });
    const pageTitle = truncate(await page.title(), 255) || null;
    const finishedAt = currentDate(options).toISOString();
    const shared = {
      ...baseResult,
      finishedAt,
      httpStatus,
      pageTitle,
      platformFilterConfirmed: visible.filtersConfirmed,
    };

    if (visible.state === "EMPTY") {
      logger.info?.(
        `[product-discovery-worker] Meta public browser response cycle=${request.cycleId} investigation=${request.investigationId} outcome=EMPTY status=${httpStatus ?? "none"} ads=0`,
      );
      return { ...shared, outcome: "EMPTY", errorMessage: null };
    }
    if (visible.state !== "CARDS") {
      const errorMessage = fallbackReason(visible);
      logger.warn?.(
        `[product-discovery-worker] Meta public browser response cycle=${request.cycleId} investigation=${request.investigationId} outcome=FALLBACK_REQUIRED status=${httpStatus ?? "none"} reason=${errorMessage}`,
      );
      return {
        ...shared,
        outcome: "FALLBACK_REQUIRED",
        errorMessage,
      };
    }

    const snapshot = await readVisibleMetaCards(page, maxAds);
    const observations = snapshot.cards
      .map((card) => parseMetaAdLibraryCard(card))
      .filter(Boolean)
      .slice(0, maxAds);
    if (observations.length === 0) {
      const errorMessage =
        "A Biblioteca exibiu cards, mas nenhum pôde ser estruturado com ID, anunciante e texto.";
      logger.warn?.(
        `[product-discovery-worker] Meta public browser response cycle=${request.cycleId} investigation=${request.investigationId} outcome=FALLBACK_REQUIRED status=${httpStatus ?? "none"} reason=${errorMessage}`,
      );
      return {
        ...shared,
        outcome: "FALLBACK_REQUIRED",
        errorMessage,
      };
    }
    logger.info?.(
      `[product-discovery-worker] Meta public browser response cycle=${request.cycleId} investigation=${request.investigationId} outcome=OBSERVED status=${httpStatus ?? "none"} ads=${observations.length}`,
    );
    return {
      ...shared,
      outcome: "OBSERVED",
      errorMessage: null,
      observations,
    };
  } catch (error) {
    const finishedAt = currentDate(options).toISOString();
    const errorMessage = truncate(
      `Falha controlada no navegador público da Meta: ${error?.message || String(error)}`,
      1000,
    );
    logger.error?.(
      `[product-discovery-worker] Meta public browser failed cycle=${request.cycleId} investigation=${request.investigationId} url=${searchUrl}`,
      error,
    );
    return {
      ...baseResult,
      finishedAt,
      outcome: "FALLBACK_REQUIRED",
      errorMessage,
    };
  } finally {
    await browser?.close().catch((error) => {
      logger.error?.(
        `[product-discovery-worker] Meta public browser close failed cycle=${request.cycleId} investigation=${request.investigationId}`,
        error,
      );
    });
  }
}

/** Restringe a navegação à Biblioteca oficial e força os filtros comerciais do ciclo. */
export function prepareMetaAdLibraryUrl(
  rawUrl,
  country = "BR",
  publisherPlatform = "INSTAGRAM",
) {
  const url = new URL(rawUrl);
  if (url.protocol !== "https:" || !ALLOWED_HOSTS.has(url.hostname)) {
    throw new Error("A URL da coleta não pertence à Biblioteca pública da Meta");
  }
  if (url.pathname !== LIBRARY_PATH) {
    throw new Error("A URL da coleta não aponta para /ads/library/");
  }
  if (String(publisherPlatform).toUpperCase() !== "INSTAGRAM") {
    throw new Error("O navegador de Argos aceita somente a plataforma Instagram");
  }
  url.searchParams.set("active_status", "active");
  url.searchParams.set("ad_type", "all");
  url.searchParams.set("country", String(country || "BR").toUpperCase());
  url.searchParams.set("is_targeted_country", "false");
  url.searchParams.set("media_type", "all");
  url.searchParams.set("publisher_platforms[0]", "instagram");
  url.searchParams.set("search_type", "keyword_unordered");
  return url.toString();
}

/** Classifica somente conteúdo funcional visível, sem confiar isoladamente no status HTTP. */
export function classifyVisibleMetaLibrary(text) {
  const visible = String(text || "");
  const platform = /(?:Plataforma|Platform):\s*Instagram/i.test(visible);
  const active =
    /Status online:\s*An[uú]ncios ativos/i.test(visible) ||
    /Status:\s*Active ads/i.test(visible);
  const country = /(?:^|\n)(?:Brasil|Brazil)(?:\n|$)/i.test(visible);
  const filtersConfirmed = platform && active && country;
  const cardCount = (
    visible.match(/(?:Identifica[cç][aã]o da biblioteca|Library ID):\s*\d+/gi) ||
    []
  ).length;
  const empty =
    /nenhum an[uú]ncio corresponde|nenhum resultado encontrado|n[aã]o encontramos nenhum resultado|0 resultados?/i.test(
      visible,
    ) ||
    /no ads match|no results found|we couldn't find any results/i.test(
      visible,
    );
  const blocked =
    /captcha|confirme que voc[eê] [eé] humano|security check|temporariamente bloqueado|temporarily blocked|log in to continue|fa[cç]a login para continuar/i.test(
      visible,
    );
  if (filtersConfirmed && cardCount > 0) {
    return { state: "CARDS", filtersConfirmed, cardCount };
  }
  if (filtersConfirmed && empty) {
    return { state: "EMPTY", filtersConfirmed, cardCount: 0 };
  }
  if (blocked) {
    return { state: "BLOCKED", filtersConfirmed, cardCount };
  }
  return { state: "PENDING", filtersConfirmed, cardCount };
}

/** Aguarda cards, vazio explícito ou bloqueio lendo somente o texto já renderizado. */
async function waitForVisibleMetaOutcome(page, options) {
  const deadline = Date.now() + options.timeoutMs;
  let latest = { state: "PENDING", filtersConfirmed: false, cardCount: 0 };
  while (Date.now() < deadline) {
    const text = await page.locator("body").innerText().catch(() => "");
    latest = classifyVisibleMetaLibrary(text);
    if (["CARDS", "EMPTY", "BLOCKED"].includes(latest.state)) {
      return latest;
    }
    await (options.delay || delay)(options.pollIntervalMs);
  }
  return latest;
}

/** Lê apenas os cards carregados na primeira página, sem rolagem ou clique adicional. */
export async function readVisibleMetaCards(page, maxAds) {
  return page.evaluate((limit) => {
    const markerPattern =
      /(?:Identifica[cç][aã]o da biblioteca|Library ID):\s*\d+/gi;
    const cards = [];
    const seen = new Set();
    const walker = document.createTreeWalker(
      document.body,
      NodeFilter.SHOW_TEXT,
    );
    let node;
    while ((node = walker.nextNode()) && cards.length < limit) {
      if (!markerPattern.test(node.nodeValue || "")) {
        markerPattern.lastIndex = 0;
        continue;
      }
      markerPattern.lastIndex = 0;
      let element = node.parentElement;
      let card = null;
      while (element && element !== document.body) {
        const text = element.innerText || "";
        const ids = text.match(markerPattern) || [];
        markerPattern.lastIndex = 0;
        if (ids.length > 1) break;
        if (
          ids.length === 1 &&
          (element.querySelector(
            '[data-testid="ad-library-ad-carousel-container"]',
          ) ||
            /(?:Patrocinado|Sponsored)/i.test(text))
        ) {
          card = element;
          break;
        }
        element = element.parentElement;
      }
      if (!card || seen.has(card)) continue;
      seen.add(card);
      const creative =
        card.querySelector(
          '[data-testid="ad-library-ad-carousel-container"]',
        ) || card;
      cards.push({
        cardText: (card.innerText || "").slice(0, 12_000),
        creativeText: (creative.innerText || "").slice(0, 8_000),
        links: [...creative.querySelectorAll("a")].slice(0, 30).map((link) => ({
          text: (link.innerText || "").trim().slice(0, 500),
          href: link.href,
        })),
        imageCount: creative.querySelectorAll("img").length,
        hasVideo:
          creative.querySelector("video") !== null ||
          /\d{1,2}:\d{2}\s*\/\s*\d{1,2}:\d{2}/.test(
            creative.innerText || "",
          ),
      });
    }
    return { cards };
  }, maxAds);
}

/** Estrutura um card visível e descarta silenciosamente elementos incompletos do layout. */
export function parseMetaAdLibraryCard(card) {
  const cardText = String(card?.cardText || "");
  const id = cardText.match(
    /(?:Identifica[cç][aã]o da biblioteca|Library ID):\s*(\d+)/i,
  )?.[1];
  const active = /(?:^|\n)(?:Ativo|Active)(?:\n|$)/i.test(cardText);
  const creativeLines = lines(card?.creativeText);
  const sponsoredIndex = creativeLines.findIndex((line) =>
    /^(?:Patrocinado|Sponsored)$/i.test(line),
  );
  const advertiserName =
    sponsoredIndex > 0 ? truncate(creativeLines[sponsoredIndex - 1], 255) : "";
  const textLines = [];
  for (const line of creativeLines.slice(sponsoredIndex + 1)) {
    if (isCreativeBoundary(line)) break;
    textLines.push(line);
  }
  const adText = truncate(textLines.join("\n").trim(), 5000);
  if (!id || !active || !advertiserName || !adText) return null;
  const destinationUrl = resolveDestinationUrl(card.links);
  const commercialSignal =
    /(?:R\$|US\$|pre[cç]o|oferta|desconto|cupom|comprar|checkout|assinar|shop now|buy now)/i.test(
      `${adText}\n${card.creativeText || ""}`,
    );
  const formatType = card.hasVideo
    ? "VIDEO"
    : Number(card.imageCount || 0) > 2
      ? "CAROUSEL"
      : Number(card.imageCount || 0) > 0
        ? "IMAGE"
        : "OTHER";
  const snapshotUrl = `https://www.facebook.com/ads/library/?id=${encodeURIComponent(id)}`;
  return {
    metaAdId: id,
    advertiserName,
    active: true,
    publisherPlatforms: ["INSTAGRAM"],
    formatTypes: [formatType],
    texts: [adText],
    destinationUrl,
    snapshotUrl,
    pageActive: true,
    commercialSignal,
    rawPayload: {
      source: "META_AD_LIBRARY_PUBLIC_BROWSER",
      metaAdId: id,
      advertiserName,
      visibleText: truncate(String(card.creativeText || ""), 8000),
      destinationUrl,
      formatType,
      platformFilter: "INSTAGRAM",
    },
  };
}

/** Seleciona o primeiro destino externo e remove o redirecionador público da Meta. */
function resolveDestinationUrl(rawLinks) {
  const candidates = [];
  for (const link of Array.isArray(rawLinks) ? rawLinks : []) {
    try {
      const parsed = new URL(link.href);
      const decoded =
        parsed.hostname === "l.facebook.com"
          ? new URL(parsed.searchParams.get("u"))
          : parsed;
      if (
        decoded.protocol === "https:" ||
        decoded.protocol === "http:"
      ) {
        candidates.push(decoded.toString());
      }
    } catch {
      // Link parcial ou sem destino público não entra na evidência.
    }
  }
  const external = candidates.find((url) => {
    const host = new URL(url).hostname;
    return !host.endsWith("facebook.com") && !host.endsWith("instagram.com");
  });
  return external ||
    candidates.find((url) => new URL(url).hostname.endsWith("instagram.com")) ||
    null;
}

/** Reconhece o início do player, destino ou CTA para não misturá-los ao texto do anúncio. */
function isCreativeBoundary(line) {
  return (
    /^\d{1,2}:\d{2}\s*\/\s*\d{1,2}:\d{2}$/.test(line) ||
    /^(?:https?:\/\/)?(?:www\.)?[a-z0-9][a-z0-9.-]+\.(?:com|com\.br|net|org|io|app)(?:\/.*)?$/i.test(
      line,
    ) ||
    /^(?:Comprar agora|Shop Now|Buy Now|Saiba mais|Learn More|Fale conosco|Visit Instagram Profile|Acessar o perfil do Instagram)$/i.test(
      line,
    )
  );
}

/** Converte o estado incompleto em uma causa objetiva para a sessão humana. */
function fallbackReason(visible) {
  if (visible.state === "BLOCKED") {
    return "A Biblioteca pública exigiu verificação, login ou bloqueou a sessão efêmera.";
  }
  if (visible.cardCount > 0 && !visible.filtersConfirmed) {
    return "A Biblioteca exibiu cards sem confirmar Brasil, Instagram e anúncios ativos.";
  }
  return "A Biblioteca não exibiu cards nem um resultado vazio verificável dentro do timeout.";
}

/** Normaliza linhas visíveis sem preservar marcadores vazios da interface. */
function lines(value) {
  return String(value || "")
    .split(/\r?\n/)
    .map((line) => line.replace(/[\u200B-\u200D\uFEFF]/g, "").trim())
    .filter(Boolean);
}

/** Retorna o instante controlável usado em testes e em auditoria. */
function currentDate(options) {
  return options.now ? options.now() : new Date();
}

/** Limita valores numéricos do ambiente ao contrato operacional seguro. */
function clamp(value, minimum, maximum) {
  return Math.max(minimum, Math.min(maximum, Number.isFinite(value) ? value : minimum));
}

/** Limita texto antes de enviá-lo ao backend. */
function truncate(value, maximum) {
  return String(value || "").slice(0, maximum).trim();
}

/** Aguarda uma nova leitura sem manter qualquer recurso externo aberto. */
function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
