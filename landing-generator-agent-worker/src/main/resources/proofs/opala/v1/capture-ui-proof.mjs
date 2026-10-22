import { createHash } from 'node:crypto';
import { createRequire } from 'node:module';
import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

export const CONTRACT = 'OPALA_COMMERCIAL_UI_PROOF_V1';
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

/** Captura somente o primeiro valor local, sem compra, cadastro ou chamada de IA. */
export async function captureUiProof(browser, input, outputFile) {
  if (input.contractVersion !== CONTRACT || input.adapter !== 'MUSA_LOCAL_FIRST_VALUE_V1'
      || !input.productVersion || input.adapterExperienceVersion !== input.productVersion
      || !/^[a-f0-9]{64}$/.test(input.adapterContractSha256 ?? '') || !Number.isSafeInteger(input.experimentId)
      || input.experimentId < 1 || !Number.isSafeInteger(input.productId) || input.productId < 1
      || !Number.isSafeInteger(input.cycleId) || input.cycleId < 1
      || !input.copySourceTaskId || !input.copySourceArtifactId) {
    throw new Error('Contrato de captura comercial incompleto.');
  }
  const canonical = new URL(input.destinationUrl);
  if (canonical.username || canonical.password || canonical.hash || canonical.search
      || !['http:', 'https:'].includes(canonical.protocol)) throw new Error('Destino de prova inválido.');
  const url = new URL(canonical);
  url.searchParams.set('mh_preview', 'qa');
  url.searchParams.set('pde_analytics', 'off');
  const page = await browser.newPage({ viewport: { width: 480, height: 900 }, deviceScaleFactor: 2.25 });
  try {
    await page.route('**/*', async route => {
      const request = route.request();
      if (request.method() === 'POST') {
        const target = new URL(request.url());
        if (target.origin !== canonical.origin || target.pathname !== '/api/pde/public/presence-diagnostic')
          return route.abort('blockedbyclient');
        const body = request.postDataJSON();
        if (body?.experienceVersion !== input.productVersion) return route.abort('blockedbyclient');
      }
      return route.continue();
    });
    await page.goto(url.toString(), { waitUntil: 'networkidle', timeout: 120000 });
    const observed = new URL(page.url());
    if (observed.origin !== canonical.origin || observed.pathname !== canonical.pathname
        || observed.search !== '?mh_preview=qa&pde_analytics=off' || observed.hash)
      throw new Error('A navegação saiu da candidata autorizada.');
    const form = page.getByRole('region', { name: 'Primeiro ajuste MUSA', exact: true });
    await form.locator('textarea').first().fill('DEMONSTRAÇÃO — trabalho');
    await form.getByRole('button', { name: 'Continuar', exact: true }).click();
    await form.locator('textarea').first().fill('camisa azul e calça preta');
    const responsePromise = page.waitForResponse(r => r.request().method() === 'POST'
      && new URL(r.url()).pathname === '/api/pde/public/presence-diagnostic');
    await form.getByRole('button', { name: 'Descobrir meu primeiro ajuste', exact: true }).click();
    const response = await responsePromise;
    const result = await response.json();
    if (response.status() !== 201 || result.status !== 'COMPLETED'
        || result.model !== 'MUSA_LOCAL_RULES_V1' || result.serviceTier !== 'LOCAL'
        || result.costUsd !== 0 || result.inputTokens !== 0 || result.outputTokens !== 0)
      throw new Error('A prova não demonstrou o primeiro valor local sem custo.');
    const rendered = page.locator('.public-diagnostic-result');
    await rendered.waitFor({ state: 'visible' });
    await page.evaluate(() => document.fonts.ready);
    const text = await rendered.innerText();
    if (!text.includes('camisa azul e calça preta') || !text.includes('DEMONSTRAÇÃO — trabalho'))
      throw new Error('O resultado visível não corresponde à entrada de demonstração.');
    const pageHeightPx = await page.evaluate(() => document.documentElement.scrollHeight);
    await rendered.evaluate(element => window.scrollTo(0, element.getBoundingClientRect().top + window.scrollY - 12));
    let scrollY = Math.round(await page.evaluate(() => window.scrollY));
    if (await rendered.locator('img,video,picture,canvas,image,iframe,object,embed,foreignObject').count())
      throw new Error('O resultado contém mídia adicional sem linhagem de direitos.');
    const externalSvgReference = await rendered.locator('use').evaluateAll(elements => elements.some(element => {
      const href = element.getAttribute('href') ?? element.getAttribute('xlink:href') ?? '';
      return href && !href.startsWith('#');
    }));
    if (externalSvgReference) throw new Error('A interface contém referência vetorial externa sem origem governada.');
    const externalBackground = await rendered.evaluate(root => [root, ...root.querySelectorAll('*')].some(element =>
      [null, '::before', '::after'].some(pseudo => /url\(/i.test(getComputedStyle(element, pseudo).backgroundImage))));
    if (externalBackground) throw new Error('O resultado contém fundo gráfico sem origem governada.');
    const captureBounds = await rendered.evaluate(root => {
      const outer = root.getBoundingClientRect();
      const start = root.querySelector('.public-main-signal')?.getBoundingClientRect();
      const end = root.querySelector('button.secondary-button')?.getBoundingClientRect();
      if (!start || !end) throw new Error('O primeiro valor não possui limites de captura definidos.');
      const y = start.top + window.scrollY - 10;
      return {x: outer.left + window.scrollX, y, width: outer.width, height: end.bottom + window.scrollY + 10 - y};
    });
    await page.evaluate(y => window.scrollTo(0, y), captureBounds.y);
    scrollY = Math.round(await page.evaluate(() => window.scrollY));
    const viewportClip = {...captureBounds, y: Math.max(0, captureBounds.y - scrollY)};
    if (viewportClip.y + viewportClip.height > 900) throw new Error('O resultado completo excede o enquadramento contratado.');
    const png = await page.screenshot({ type: 'png', clip: viewportClip });
    await writeFile(outputFile, png);
    return {
      contractVersion: CONTRACT, adapter: input.adapter,
      adapterContractSha256: input.adapterContractSha256, adapterExperienceVersion: input.adapterExperienceVersion,
      productId: input.productId, cycleId: input.cycleId, experimentId: input.experimentId,
      productVersion: input.productVersion, copySourceTaskId: input.copySourceTaskId,
      copySourceArtifactId: input.copySourceArtifactId, destinationUrl: input.destinationUrl,
      responseSha256: sha(Buffer.from(JSON.stringify(result))),
      sampleInput: {occasion: 'DEMONSTRAÇÃO — trabalho', existingSelection: 'camisa azul e calça preta'},
      functionalResult: result,
      model: result.model, serviceTier: result.serviceTier, inputTokens: 0, outputTokens: 0,
      costUsd: 0, testData: true, commercialEvidenceEligible: false,
      sha256: sha(png), captureSessionId: `opala-ui-${input.taskId}`,
      evidenceKey: 'COMMERCIAL-FIRST-VALUE', evidenceType: 'FOLD', deviceProfile: 'PDE_UI_480',
      pageNumber: 1, foldNumber: 1, viewportWidth: 480, viewportHeight: 900,
      captureMode: 'DOM_REGION', captureRegion: 'FIRST_VALUE_ACTIONS_AND_DOWNLOAD', captureBounds, pixelWidth: png.readUInt32BE(16), pixelHeight: png.readUInt32BE(20),
      pageHeightPx, scrollY, sourceUrl: input.destinationUrl, finalUrl: input.destinationUrl,
      inspectedUrl: page.url(), capturedAt: new Date().toISOString(),
      sourceType: 'OWN_PRODUCT_UI', externalImagesIncluded: false,
      rendererVersion: CONTRACT,
    };
  } finally { await page.close(); }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const [inputFile, outputFile, receiptFile] = process.argv.slice(2);
  if (!inputFile || !outputFile || !receiptFile) throw new Error('Informe entrada, PNG e recibo.');
  const require = createRequire(resolve(process.cwd(), 'package.json'));
  const { chromium } = require('playwright-core');
  const browser = await chromium.launch({ headless: true,
    ...(process.env.CHROMIUM_BIN ? { executablePath: process.env.CHROMIUM_BIN } : {}),
    args: ['--disable-dev-shm-usage'] });
  try {
    const receipt = await captureUiProof(browser, JSON.parse(await readFile(inputFile, 'utf8')), outputFile);
    receipt.rendererSha256 = sha(await readFile(new URL(import.meta.url)));
    await writeFile(receiptFile, JSON.stringify(receipt));
  } catch (error) {
    await writeFile(receiptFile, JSON.stringify({contractVersion: CONTRACT, status: 'FAILED',
      operation: 'CAPTURE_FIRST_VALUE', errorType: error.name, message: String(error.message).slice(0, 1000),
      modelInvoked: false, incrementalModelCostUsd: 0}));
    throw error;
  } finally { await browser.close(); }
}
