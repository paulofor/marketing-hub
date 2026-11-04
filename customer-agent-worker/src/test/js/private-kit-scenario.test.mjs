import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import { runPrivateKitScenario, waitForPrivateKitResult } from '../../main/resources/browser/private-kit-scenario.mjs';
import { downloadPrivateKit } from '../../main/resources/browser/private-kit-download.mjs';
const require = createRequire(import.meta.url);
let library; try { library = require('playwright'); } catch { library = require('playwright-core'); }
const { chromium, devices } = library;
const root = fileURLToPath(new URL('../../../../backend/ads-service/src/main/resources/private-kit-v1/', import.meta.url));
const image = await readFile(root + 'samples/nails-v2.png');
const payload = Buffer.from('transporte sintético: os contratos de integridade são testados separadamente');

// Usa a página versionada real, com backend local controlado e identidades distintas.
test('prazo único cobre preparação, copiar, baixar e retomar sem repetir composição', async () => {
  const sessions = new Map();
  let stalls = 0;
  const server = createServer(async (req, res) => {
    if (req.url === '/stall') { stalls++; return; }
    if (['/prototype', '/prototype.js', '/prototype.css'].includes(req.url)) {
      const name = req.url === '/prototype' ? 'prototype.html' : req.url.slice(1);
      res.setHeader('Content-Type', name.endsWith('.js') ? 'text/javascript' : name.endsWith('.css') ? 'text/css' : 'text/html');
      res.end(await readFile(root + name)); return;
    }
    const fixture = sessions.get(req.headers['x-kit-session']);
    if (!fixture) { res.writeHead(401); res.end(); return; }
    const response = async (data, type = 'application/json') => {
      await new Promise(resolve => setTimeout(resolve, fixture.delay));
      if (res.destroyed) return;
      res.setHeader('Content-Type', type); res.end(type === 'application/json' ? JSON.stringify(data) : data);
    };
    if (req.url === '/session') return response(fixture.state);
    if (req.url.startsWith('/assets/')) {
      fixture.assetReads++;
      if (fixture.failAssets) { res.writeHead(503); res.end(); return; }
      return response(image, 'image/png');
    }
    if (req.url === '/download') { fixture.downloads++; return response(payload, 'application/zip'); }
    if (req.url === '/input') {
      fixture.compositions++; fixture.state.status = 'READY'; return response(fixture.state);
    }
    if (req.url === '/events') {
      let body = ''; for await (const chunk of req) body += chunk;
      fixture.state.events.push({ code: JSON.parse(body).code, origin: 'AGENT_VALIDATION', mh_internal_test: true });
      return response(fixture.state);
    }
    res.writeHead(404); res.end();
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const executable = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium';
  const browser = await chromium.launch({ ...(existsSync(executable) ? { executablePath: executable } : {}), args: ['--no-sandbox'] });
  const fixtureFor = (id, productId, cycleId, delay) => ({
    delay, compositions: 0, downloads: 0, assetReads: 0, failAssets: false,
    state: { id, productId, cycleId, status: 'INPUT', productName: 'Produto sintético ' + productId,
      presentation: { title: 'Kit de teste', introduction: 'Demonstração local.', statusMessage: '' }, specialties: [], events: [],
      firstApplication: { post: 'post-01.png', story: 'story-01.png', caption: 'Legenda de Cidade Exemplo', message: 'Mensagem de exemplo', calendar: 'Dia 1: post de exemplo' },
      manifest: { zipSha256: String(productId).padStart(64, '0'), files: [{ name: 'posts/post-01.png' }, { name: 'stories/story-01.png' }] } },
  });
  const open = async (token, profile) => {
    const context = await browser.newContext({ ...profile, defaultBrowserType: undefined, acceptDownloads: true });
    context.setDefaultTimeout(200);
    const page = await context.newPage();
    await page.addInitScript(secret => sessionStorage.setItem('private-kit-session-v1', secret), token);
    return { context, page };
  };
  try {
    // Reproduz a espera anterior: o pacote existe, mas a renderização demora mais que o padrão.
    const baselineToken = 'a'.repeat(64), baseline = fixtureFor('anterior', 7, 19, 400);
    baseline.state.status = 'READY'; sessions.set(baselineToken, baseline);
    const previous = await open(baselineToken, { viewport: { width: 1440, height: 900 } });
    try {
      await previous.page.goto(base + '/prototype', { timeout: 5000 });
      await assert.rejects(previous.page.locator('#result').waitFor({ state: 'visible' }), /Timeout/);
    } finally { await previous.context.close(); }

    for (const [code, productId, cycleId, delay, profile] of [
      ['1', 7, 19, 250, { viewport: { width: 1440, height: 900 } }],
      ['2', 901, 9019, 250, devices['Pixel 7']],
      ['3', 902, 9020, 0, devices['iPhone 15 Pro']],
    ]) {
      const token = code.repeat(64), fixture = fixtureFor('session-' + code, productId, cycleId, delay);
      sessions.set(token, fixture);
      const { context, page } = await open(token, profile);
      const startedAt = Date.now();
      try {
        await runPrivateKitScenario(context, startedAt, async signal => {
          await page.goto(base + '/prototype'); await page.locator('#briefing').waitFor({ state: 'visible' });
          await page.locator('[name=consentAccepted]').check(); await page.locator('#prepare').click();
          await waitForPrivateKitResult(page);
          await page.evaluate(() => Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: async text => { window.copied = text; } } }));
          await page.locator('#copy-caption').click(); await page.getByText('Texto copiado.', { exact: false }).waitFor();
          assert.match(await page.evaluate(() => window.copied), /Cidade Exemplo/);
          const download = await downloadPrivateKit(page, startedAt);
          assert.deepEqual(await readFile(await download.path()), payload);
          await page.getByText('Kit recebido.', { exact: false }).waitFor();
          const before = await (await fetch(base + '/session', { headers: { 'X-Kit-Session': token }, signal })).json();
          await page.locator('#exit').click(); await page.locator('#resume').waitFor({ state: 'visible' });
          await page.locator('#return').click(); await waitForPrivateKitResult(page);
          await page.reload(); await waitForPrivateKitResult(page);
          const after = await (await fetch(base + '/session', { headers: { 'X-Kit-Session': token }, signal })).json();
          assert.equal(after.productId, productId); assert.equal(after.cycleId, cycleId);
          assert.equal(after.manifest.zipSha256, before.manifest.zipSha256);
          assert.ok(after.events.some(e => e.code === 'SESSION_RETURNED'));
          assert.equal(fixture.compositions, 1); assert.equal(fixture.downloads, 1);
          assert.equal(fixture.assetReads, 4);

          // Uma falha HTTP verdadeira deve continuar bloqueada e ser descrita imediatamente.
          fixture.failAssets = true;
          await page.reload();
          await assert.rejects(waitForPrivateKitResult(page), /A experiência não abriu o kit/);
          assert.equal(fixture.compositions, 1);
        });
      } finally { await context.close(); }
    }
    const context = await browser.newContext();
    try {
      const start = Date.now(); let aborted = false;
      await assert.rejects(runPrivateKitScenario(context, start - 600000 + 250, async signal => {
        signal.addEventListener('abort', () => { aborted = true; });
        await fetch(base + '/stall', { signal });
      }), /prazo de dez minutos/);
      assert.ok(Date.now() - start < 2000); assert.equal(aborted, true); assert.equal(stalls, 1);
      await assert.rejects(runPrivateKitScenario(context, Date.now() - 600001, () => assert.fail('Não pode iniciar fora do prazo')), /prazo/);
    } finally { await context.close(); }
  } finally {
    await browser.close(); server.closeAllConnections(); await new Promise(resolve => server.close(resolve));
  }
});
