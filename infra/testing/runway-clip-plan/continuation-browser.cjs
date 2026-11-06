// Homologa a página real, o controller e o coordenador locais; providers e persistência são doubles.
const { chromium, devices, expect } = require('@playwright/test');
const fs = require('node:fs');
const frontend = 'http://127.0.0.1:4173';
const backend = 'http://127.0.0.1:18081';
(async () => {
  const out = process.argv[2] || '.codex/vega-resume/browser';
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  try {
    for (const [name, options, offset] of [
      ['desktop', { viewport: { width: 1440, height: 1000 } }, 0],
      ['iphone', devices['iPhone 15 Pro'], 100], ['pixel', devices['Pixel 7'], 200],
    ]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const errors = [], requests = [];
      page.on('pageerror', error => errors.push(error.message));

      await page.route('**/*', async route => {
        const request = route.request(), url = new URL(request.url());
        if (url.pathname.startsWith('/api/')) {
          requests.push({ method: request.method(), path: url.pathname, body: request.headers()['content-type']?.includes('multipart/form-data') ? 'PRIVATE_RECORDING_MULTIPART' : request.postDataJSON() });
          if (url.pathname.endsWith('/product-recording')) return route.continue();
          if (url.pathname.includes('/autonomy/v1/') || ['/api/sales-videos/provider-models', '/api/sales-videos/studio/clip-plan'].includes(url.pathname)) {
            const response = await route.fetch({ url: backend + url.pathname, headers: { ...request.headers(), 'X-Tenant-ID': 'studio-test' } });
            return route.fulfill({ response });
          }
          if (request.method() !== 'GET') throw new Error('Mutação fora do escopo local: ' + url.pathname);
          const match = url.pathname.match(/^\/api\/sales-videos\/projects\/(\d+)$/);
          if (match) {
            const id = Number(match[1]), base = id - offset;
            return route.fulfill({ json: {
              id, productId: base === 14 ? 4 : 44, salesVideoProfileId: id + 55,
              experimentId: base === 14 ? 114 : 215, campaignKey: `fixture-local-${id}`,
              videoCategory: 'COMMERCIAL_SHORT', contextType: 'PDE', productionMode: 'CINEMATIC_SCENE_BLUEPRINT',
              targetChannel: 'SOCIAL_REELS_STORIES', format: 'VERTICAL_9_16', title: `Projeto sintético ${id}`,
              objective: 'Demonstrar mecanismo real sem geração externa', targetDurationSeconds: 32,
              scenePlan: 'Gancho\nResultado\nMecanismo\nProva\nCTA', providerPlan: 'Provider escolhido no Estudio: Runway Gen-4.5 (RUNWAY).', status: 'DRAFT',
            } });
          }
          return route.fulfill({ json: url.pathname.endsWith('/studio/catalog') ? { characters: [], captionPresets: [] } : [] });
        }
        if (url.origin === frontend) return route.continue();
        return route.abort();
      });
      const projectId = 14 + offset;
      await page.goto(`${frontend}/e2e/studio-continuation.html?projectId=${projectId}`, { waitUntil: 'networkidle' });
      await page.getByRole('button', { name: 'Continuar storyboard no ciclo' }).click();
      await expect(page.getByText(new RegExp(`Ciclo #${projectId + 1000}: QUEUED_FOR_APOLLO.*job #${projectId + 23000}`))).toBeVisible();
      await page.getByRole('button', { name: 'Continuar storyboard no ciclo' }).click();
      await expect(page.getByText(new RegExp(`job #${projectId + 23000}`))).toBeVisible();
      const denied = await context.request.post(`${backend}/api/sales-videos/projects/${projectId}/autonomy/v1/render`, { headers: { 'X-Tenant-ID': 'other-tenant' } });
      expect(denied.status()).toBe(404);
      await page.screenshot({ path: `${out}/${name}-canonical.png`, fullPage: true });
      const preparationId = 81 + offset;
      await page.goto(`${frontend}/e2e/studio-continuation.html?projectId=${preparationId}`, { waitUntil: 'networkidle' });
      const form = page.getByRole('form', { name: 'Preparação integral de custos' });
      await expect(form).toBeVisible();
      for (const input of await form.getByLabel('Máximo restante em US$ *', { exact: true }).all()) await input.fill('0.10');
      for (const input of await form.getByLabel('Fonte ou evidência *', { exact: true }).all()) await input.fill('Fonte sintética local, sem cobrança externa');
      await form.getByLabel('Validade da previsão (UTC) *', { exact: true }).fill('2099-01-01T12:00');
      await form.getByRole('button', { name: 'Registrar evidências e reavaliar o ciclo' }).click();
      await expect(page.getByText(`Ciclo #${preparationId + 1000}: PENDING_FINANCIAL_REVIEW`, { exact: false })).toBeVisible();
      await page.screenshot({ path: `${out}/${name}-costs.png`, fullPage: true });
      if (process.env.STUDIO_FUNCTIONAL_RECORDING_DIR) {
        const source=process.env.STUDIO_FUNCTIONAL_RECORDING_DIR;
        await page.goto(`${frontend}/e2e/studio-continuation.html?projectId=414`, { waitUntil: 'networkidle' });
        await page.getByLabel('MP4 da gravação (até 8 MiB)',{exact:true}).setInputFiles(`${source}/functional-recording.mp4`);
        await page.getByLabel('Manifesto da captura (JSON)',{exact:true}).setInputFiles(`${source}/manifest.json`);
        await expect(page.getByLabel('Prévia da gravação funcional')).toBeVisible();
        await page.getByRole('button',{name:'Vincular gravação privada',exact:true}).click();
        await expect(page.getByText('Gravação privada vinculada ao projeto. Nenhuma produção paga foi iniciada.')).toBeVisible().catch(async error => { await page.screenshot({path:`${out}/${name}-recording-failure.png`,fullPage:true}); fs.writeFileSync(`${out}/${name}-recording-failure.txt`,await page.locator('body').innerText()); throw error; });
        const retrieved=await context.request.get(`${backend}/api/sales-videos/projects/414/product-recording`,{headers:{'X-Tenant-ID':'studio-test'}});
        expect(retrieved.status()).toBe(200);
        expect(require('node:crypto').createHash('sha256').update(await retrieved.body()).digest('hex')).toBe(JSON.parse(fs.readFileSync(`${source}/manifest.json`)).sha256);
        const other=await context.request.get(`${backend}/api/sales-videos/projects/414/product-recording`,{headers:{'X-Tenant-ID':'other-tenant'}});
        expect(other.status()).toBe(404);
        await page.screenshot({path:`${out}/${name}-functional-recording.png`,fullPage:true});
      }
      if (requests.some(r => r.path.endsWith('/request-render'))) throw new Error('Atalho administrativo de render foi chamado');
      if (errors.length) throw new Error(errors.join('\n'));
      fs.writeFileSync(`${out}/${name}-requests.json`, JSON.stringify(requests, null, 2));
      console.log(`PASS ${name}: UI/backend canônicos, reuso do job, tenant recusado e previsão reavaliada no mesmo ciclo sem gerar`);
      await context.close();
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
