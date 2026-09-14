const { chromium, devices, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const output = process.argv[2];
const base = process.env.VIDEO_FINALIZATION_BASE_URL || 'http://127.0.0.1:15173';
assert.match(base, /^http:\/\/127\.0\.0\.1:\d+$/);
const narration = 'Escolha ocasião e combinação. | Veja seu ajuste. Aplique e avalie. | Salve para retomar. | Experimente o primeiro ajuste MUSA. | Sem compra ou cobrança.';
(async () => {
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  const results = [];
  try {
    for (const [name, device] of [['desktop', { viewport: { width: 1440, height: 1000 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const external = [], writes = [], errors = [];
      const profile = { id: 91060, productId: 91001, title: 'Demonstração sintética segregada', videoKind: 'HERO', avatarStrategy: 'PLATFORM_TEST_AVATAR', targetDurationSeconds: 15, status: 'VIDEO_READY', requiresConsent: false };
      const source = { id: 91042, profileId: 91060, assetId: 91010, jobType: 'VIDEO_RENDER', providerFamily: 'EXTERNAL_VIDEO_MODULE', providerName: 'RUNWAY_ROUTER', executionMode: 'TEST', status: 'VIDEO_READY', metadataJson: JSON.stringify({ cta_text: 'CTA #8 plano personalizado', quality_gate: { reject_if: ['flicker', 'haze'] }, syntheticTestOnly: true }) };
      const failed = { ...source, id: 91043, retryOfJobId: 91042, jobType: 'POST_PRODUCTION', status: 'VIDEO_FAILED', assetId: null, failureCode: 'APOLLO_VIDEO_STABILITY_REJECTED', failureDetail: 'Medição sintética acima do limite' };
      const jobs = [source, failed];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', async route => {
        const request = route.request(), url = new URL(request.url());
        if (url.origin !== base) { external.push(url.href); return route.abort(); }
        if (url.pathname === '/fixture.mp4') return route.fulfill({ path: path.join(output, 'final-fixture.mp4'), contentType: 'video/mp4' });
        if (url.pathname.startsWith('/api/')) {
          if (request.method() !== 'GET') {
            writes.push({ method: request.method(), path: url.pathname, body: request.postDataJSON() });
            assert.equal(request.method(), 'POST');
            assert.equal(url.pathname, '/api/sales-videos/jobs/91042/request-post-production');
            assert.equal(request.postDataJSON().captionText, narration);
            assert.equal(request.postDataJSON().voiceOverScript, narration);
            assert.equal(request.postDataJSON().sourceVideoUrl, base + '/fixture.mp4');
            const child = { ...source, id: 91044, retryOfJobId: 91042, assetId: null, jobType: 'POST_PRODUCTION', status: 'VIDEO_REQUESTED' };
            jobs.unshift(child);
            return route.fulfill({ json: child });
          }
          if (url.pathname === '/api/products/91001') return route.fulfill({ json: { id: 91001, name: 'Vega fixture segregada', description: 'Homologação local; sem métricas comerciais' } });
          if (url.pathname === '/api/products/91001/sales-videos/profiles') return route.fulfill({ json: [profile] });
          if (url.pathname === '/api/products/91001/sales-videos/jobs') return route.fulfill({ json: jobs });
          if (url.pathname === '/api/media/91010') return route.fulfill({ json: { id: 91010, type: 'VIDEO', status: 'READY', url: base + '/fixture.mp4' } });
          return route.fulfill({ json: [] });
        }
        return route.continue();
      });
      await page.goto(base + '/products/91001/sales-videos', { waitUntil: 'networkidle' });
      const sourceButton = page.getByRole('button').filter({ hasText: 'Job #91042' });
      await expect(sourceButton).toBeVisible();
      await sourceButton.click();
      await expect(sourceButton).toContainText('Revisão visual pendente');
      await expect(page.getByText('Bloqueado: luz oscilando', { exact: true })).toHaveCount(0);
      await expect(page.getByText('Reprovado na inspeção técnica', { exact: true })).toBeVisible();
      await page.getByLabel('Voz off', { exact: true }).fill(narration);
      await page.getByLabel('Legenda principal', { exact: true }).fill(narration);
      const submit = page.getByRole('button', { name: 'Gerar pós-produção + HLS', exact: true });
      await submit.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, 'finalization-' + name + '.png') });
      const response = page.waitForResponse(r => r.request().method() === 'POST');
      await submit.click();
      assert.equal((await response).status(), 200);
      assert.equal(writes.length, 1); assert.deepEqual(external, []); assert.deepEqual(errors, []);
      results.push({ device: name, syntheticTestOnly: true, sourceJob: 91042, child: 91044, paidCalls: 0, externalRequests: 0, writes });
      await context.close();
    }
    fs.writeFileSync(path.join(output, 'finalization-browser-results.json'), JSON.stringify(results, null, 2));
    console.log(JSON.stringify(results.map(({ device, sourceJob, child, paidCalls, externalRequests }) => ({ device, sourceJob, child, paidCalls, externalRequests }))));
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
