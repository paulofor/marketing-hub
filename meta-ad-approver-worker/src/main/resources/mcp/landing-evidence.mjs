const MAX_JSON_BYTES = 2 * 1024 * 1024;
const LANDING_TIMEOUT_MS = 120000;

/** Captura landing e checkout em modo somente leitura, sem clicar ou enviar formulários. */
export async function captureCommercialLanding(browser, destinationUrl) {
  const landingUrl = inspectionUrl(destinationUrl);
  const screenshots = [];
  const offers = [];
  for (const viewport of [{ width: 390, height: 844 }, { width: 1440, height: 1000 }]) {
    const page = await browser.newPage({ viewport });
    const responseTasks = observeCommercialOffers(page, landingUrl);
    try {
      await page.goto(landingUrl, { waitUntil: 'domcontentloaded', timeout: LANDING_TIMEOUT_MS });
      await waitForCommercialLanding(page);
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
      await page.evaluate(() => document.fonts?.ready);
      screenshots.push(await page.screenshot({ fullPage: true, type: 'jpeg', quality: 82 }));
      offers.push(...(await resolveOffers(responseTasks)));
    } finally {
      await page.close();
    }
  }
  const offer = uniqueOffers(offers)[0] ?? null;
  const checkout = offer ? await captureCheckout(browser, offer) : null;
  return { landingUrl, screenshots, checkout };
}

/** Acrescenta marcadores neutros de QA para que a inspeção não contamine métricas comerciais. */
function inspectionUrl(destinationUrl) {
  const url = new URL(destinationUrl);
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('URL de destino não usa HTTP ou HTTPS.');
  }
  url.searchParams.set('mh_preview', 'qa');
  url.searchParams.set('pde_analytics', 'off');
  return url.toString();
}

/** Observa contratos JSON carregados pela própria landing e localiza o checkout comercial. */
function observeCommercialOffers(page, landingUrl) {
  const tasks = [];
  page.on('response', response => {
    const contentType = response.headers()['content-type'] ?? '';
    if (!contentType.toLowerCase().includes('application/json')) return;
    tasks.push(readCommercialOffers(response, landingUrl));
  });
  return tasks;
}

/** Lê somente respostas JSON pequenas e devolve os contratos comerciais normalizados. */
async function readCommercialOffers(response, landingUrl) {
  const declaredLength = Number(response.headers()['content-length']);
  if (Number.isFinite(declaredLength) && declaredLength > MAX_JSON_BYTES) return [];
  try {
    const body = await response.body();
    if (body.length > MAX_JSON_BYTES) return [];
    return findCommercialOffers(JSON.parse(body.toString('utf8')), response.url(), landingUrl);
  } catch {
    return [];
  }
}

/** Percorre o contrato sem expor o payload completo ao modelo revisor. */
function findCommercialOffers(value, sourceUrl, landingUrl, depth = 0) {
  if (depth > 8 || value === null || typeof value !== 'object') return [];
  if (Array.isArray(value)) {
    return value.flatMap(item => findCommercialOffers(item, sourceUrl, landingUrl, depth + 1));
  }
  const offers = [];
  if (value.commercialCheckout && typeof value.commercialCheckout === 'object') {
    const normalized = normalizeOffer(value.commercialCheckout, sourceUrl, landingUrl);
    if (normalized) offers.push(normalized);
  }
  for (const [key, child] of Object.entries(value)) {
    if (key !== 'commercialCheckout') {
      offers.push(...findCommercialOffers(child, sourceUrl, landingUrl, depth + 1));
    }
  }
  return offers;
}

/** Mantém somente os campos necessários para conferir oferta, preço e cobrança. */
function normalizeOffer(value, sourceUrl, landingUrl) {
  const rawUrl = value.checkoutUrl ?? value.url;
  if (typeof rawUrl !== 'string' || rawUrl.trim() === '') return null;
  let checkoutUrl;
  try {
    checkoutUrl = new URL(rawUrl, landingUrl).toString();
  } catch {
    return null;
  }
  if (!/^https?:\/\//i.test(checkoutUrl)) return null;
  return {
    sourceUrl,
    provider: stringValue(value.provider),
    checkoutUrl,
    offerReference: stringValue(value.offerReference),
    priceBrl: numberValue(value.priceBrl),
    currency: stringValue(value.currency),
    billingModel: stringValue(value.billingModel),
  };
}

/** Aguarda as leituras assíncronas disparadas pelos eventos de rede. */
async function resolveOffers(tasks) {
  return (await Promise.all(tasks)).flat();
}

/** Elimina repetições causadas pelas capturas mobile e desktop. */
function uniqueOffers(offers) {
  const byUrl = new Map();
  for (const offer of offers) byUrl.set(offer.checkoutUrl, offer);
  return [...byUrl.values()];
}

/** Abre a oferta sem interação e captura exatamente o conteúdo visível do checkout. */
async function captureCheckout(browser, offer) {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  try {
    const response = await page.goto(offer.checkoutUrl, {
      waitUntil: 'domcontentloaded',
      timeout: LANDING_TIMEOUT_MS,
    });
    if (!response || !response.ok()) {
      throw new Error(`Checkout respondeu HTTP ${response?.status() ?? 'desconhecido'}.`);
    }
    await page.waitForFunction(
      () => (document.body?.innerText?.trim().length ?? 0) >= 80,
      null,
      { timeout: LANDING_TIMEOUT_MS },
    );
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    const visibleText = await page.locator('body').innerText();
    return {
      ...offer,
      finalUrl: page.url(),
      title: await page.title(),
      visibleTextExcerpt: visibleText.replace(/\s+/g, ' ').trim().slice(0, 1500),
      screenshot: await page.screenshot({ fullPage: true, type: 'jpeg', quality: 82 }),
      interactionPerformed: false,
    };
  } finally {
    await page.close();
  }
}

/** Aguarda conteúdo comercial real, ignorando a tela transitória do PDE. */
async function waitForCommercialLanding(page) {
  await page.waitForFunction(() => {
    const text = document.body?.innerText?.trim() ?? '';
    const transient = text === 'Preparando uma oferta especial para você...';
    return !transient && text.length >= 200;
  }, null, { timeout: LANDING_TIMEOUT_MS });
}

/** Normaliza texto opcional sem inventar evidência. */
function stringValue(value) {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null;
}

/** Normaliza preço numérico opcional sem aceitar valores inválidos. */
function numberValue(value) {
  if (value === null || value === undefined || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}
