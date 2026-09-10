// Executa o frontend real com contratos de leitura locais; nenhuma API produtiva é chamada.
const { chromium, devices, expect } = require('@playwright/test');
const { readFile, mkdir, writeFile } = require('node:fs/promises');
const { createServer } = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const fixtureDir = path.resolve('infra/testing/vega-cycle-card/fixtures');
  const output = path.resolve(process.env.VEGA_CARD_OUTPUT || 'artifacts/vega-cycle-card/browser');
  const uiDir = path.resolve('frontend/dist');
  const readJson = async (name) => JSON.parse(await readFile(path.join(fixtureDir, name + '.json'), 'utf8'));
  const position = await readJson('position');
  const context = await readJson('context');
  const activities = await readJson('activities');
  const products = [
    { id: 4, name: 'Vega · QA local', internalName: 'Vega', productTypeInternalName: 'Opala', slug: 'metodo-musa-7-dias', commercialStatus: 'ATIVO', operationStatus: 'PLAY', currentPriceBrl: 67, targetAudience: 'Mulheres que querem aplicar um primeiro ajuste com o que já possuem.', primaryHypothesis: 'Melhorar o primeiro resultado útil e medir a continuidade até compra.' },
    { id: 10, name: 'Mira · QA local', internalName: 'Mira', productTypeInternalName: 'Safira', slug: 'pde-planejado-36', commercialStatus: 'PLANNED', operationStatus: 'PLAY', currentPriceBrl: 49 },
    { id: 9, name: 'Rigel · QA local', internalName: 'Rigel', productTypeInternalName: 'Opala', slug: 'kit-whatsapp-pronto', commercialStatus: 'ATIVO', operationStatus: 'PLAY', currentPriceBrl: 349 },
  ];
  const mira = { ...position, productId: 10, commercialStatus: 'PLANNED', processDefinitionId: 70, processCode: 'pde-construction-approval', processName: 'Protótipo, validação multiagente e aprovação do PDE', sequenceNumber: 3, processMeasurements: [], subprocessPosition: null };
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, 'http://localhost').pathname;
      const file = name.startsWith('/assets/') ? path.join(uiDir, name) : path.join(uiDir, 'index.html');
      assert(file.startsWith(uiDir + path.sep));
      const body = await readFile(file);
      res.setHeader('Content-Type', file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html');
      res.end(body);
    } catch { res.writeHead(404); res.end(); }
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN || '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  const results = [];
  try {
    for (const [device, profile] of [
      ['desktop', { viewport: { width: 1440, height: 1000 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const session = await browser.newContext(profile);
      const page = await session.newPage();
      const errors = [];
      const unexpected = [];
      const requests = [];
      let responseMode = 'success';
      page.on('pageerror', error => errors.push(error.message));
      await page.route('**/*', async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.startsWith('/ws')) return route.abort();
        if (url.pathname.startsWith('/api/')) {
          requests.push({ method: request.method(), path: url.pathname, query: url.search });
          assert.equal(request.method(), 'GET', 'A navegação não pode criar tarefas nem métricas.');
          let data;
          if (url.pathname === '/api/products') data = products;
          else if (url.pathname === '/api/products/value-chain-positions') data = [position, mira, { ...position, productId: 9, processMeasurements: [], subprocessPosition: null }];
          else if (url.pathname === '/api/products/value-chain-positions/4') data = position;
          else if (url.pathname.endsWith('/process-context')) {
            assert.equal(url.pathname, '/api/business-process-chains/learning-cycles/v1/products/4/process-context');
            assert.equal(url.searchParams.get('cycleId'), '2');
            assert.equal(url.searchParams.get('chainId'), '14');
            assert(['75', '70'].includes(url.searchParams.get('processDefinitionId')));
            if (responseMode === 'error') return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Indisponibilidade simulada na sandbox' }) });
            data = responseMode === 'empty' ? null : context;
          } else if (url.pathname === '/api/business-processes/70/products/4/activity-executions') {
            assert.equal(url.searchParams.get('learningCycleId'), '2');
            assert.equal(url.searchParams.get('chainId'), '14');
            data = activities;
          } else if (['/api/creatives/video-review', '/api/ops-monitor/v1/modules/availability', '/api/facebook/configuration-status'].includes(url.pathname)) data = [];
          else { unexpected.push(url.pathname); return route.abort(); }
          return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
        }
        if (url.origin !== origin) { unexpected.push(url.href); return route.abort(); }
        await route.continue();
      });

      for (const [surface, pathname] of [['home', '/'], ['catalog', '/products']]) {
        await page.goto(origin + pathname, { waitUntil: 'domcontentloaded' });
        const card = page.getByRole('region', { name: 'Posição de Vega · QA local na cadeia de valor' });
        await expect(card.getByRole('heading', { name: '2º ciclo · Experimento #92' })).toBeVisible();
        await expect(card.getByText('Pendência do ciclo', { exact: true })).toBeVisible();
        await expect(card.getByText('Responsável: Psique')).toBeVisible();
        await expect(card.getByText('Etapa 6 de 6')).toHaveCount(0);
        await expect(card.getByText(/Não há outro subprocesso previsto/)).toHaveCount(0);
        await expect(card.getByText(/Processo 3 — Protótipo/)).toBeVisible();
        await expect(card.getByRole('link', { name: 'Ver atividade e pendência' })).toHaveAttribute('href', context.nextWork.url);
        await expect(card.getByRole('link', { name: 'Ver ciclo e decisões' })).toHaveAttribute('href', context.cycleUrl);
        const other = page.getByRole('region', { name: 'Posição de Mira · QA local na cadeia de valor' });
        await expect(other.getByText('Etapa 3 de 6')).toBeVisible();
        await expect(other.getByText(/Experimento #92/)).toHaveCount(0);
        await card.scrollIntoViewIfNeeded();
        await page.screenshot({ path: path.join(output, `${device}-${surface}.png`) });
        await card.screenshot({ path: path.join(output, `${device}-${surface}-card.png`) });
        const learning = card.locator('summary').filter({ hasText: 'Aprendizado dos ciclos anteriores' });
        // Summary nativo permanece utilizável pelo teclado em todos os perfis.
        await learning.focus();
        await page.keyboard.press('Enter');
        await expect(card.getByText(context.previousLearning[2].learning, { exact: true })).toBeVisible();
        await expect(card.getByText(context.previousLearning[2].limitation, { exact: false })).toBeVisible();
        await card.locator('summary').filter({ hasText: 'Evidência registrada' }).nth(2).click();
        await expect(card.getByText(context.previousLearning[2].evidenceReference, { exact: true }).last()).toBeVisible();
        await card.locator('summary').filter({ hasText: 'Melhoria e hipótese desta passagem' }).click();
        await expect(card.getByText(context.mainChange, { exact: true })).toBeVisible();
        await card.locator('summary').filter({ hasText: 'Histórico da cadeia · tempo e custo acumulados' }).click();
        await expect(card.getByText('Valores acumulados do produto na cadeia; não são o total deste ciclo.')).toBeVisible();
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, 'A memória e as fontes não podem criar overflow horizontal.');
        await card.getByRole('link', { name: 'Ver atividade e pendência' }).click();
        await expect(page).toHaveURL(origin + context.nextWork.url);
        const activity = page.locator('#activity-technicalHomologation');
        await expect(activity.getByText('Responsável: Psique')).toBeVisible();
        await expect(activity.getByRole('button', { name: /Executar atividade|Reiniciar tarefa/ })).toHaveCount(0);
        await expect(page.getByRole('heading', { name: '2º ciclo de vendas · Experimento #92' })).toBeVisible();
        await expect(page.getByText(context.previousLearning[2].learning, { exact: true })).toBeVisible();
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
        await activity.scrollIntoViewIfNeeded();
        await page.screenshot({ path: path.join(output, `${device}-${surface}-destination.png`) });
        results.push({ device, surface, outcome: 'PASS', cycle: 2, experiment: 92, activity: '3.5', writes: 0 });
      }

      responseMode = 'error';
      await page.goto(origin, { waitUntil: 'domcontentloaded' });
      const card = page.getByRole('region', { name: 'Posição de Vega · QA local na cadeia de valor' });
      await expect(card.getByRole('alert')).toBeVisible({ timeout: 20000 });
      await expect(card.getByText('Etapa 6 de 6')).toHaveCount(0);
      responseMode = 'success';
      await card.getByRole('button', { name: 'Tentar novamente' }).click();
      await expect(card.getByText('Pendência do ciclo')).toBeVisible();
      responseMode = 'empty';
      await page.goto(origin, { waitUntil: 'domcontentloaded' });
      await expect(card.getByRole('alert')).toBeVisible();
      await expect(card.getByText('Etapa 6 de 6')).toHaveCount(0);
      assert.deepEqual(errors, []);
      assert.deepEqual(unexpected, []);
      assert(requests.every(request => request.method === 'GET'));
      await writeFile(path.join(output, device + '-requests.json'), JSON.stringify(requests, null, 2));
      results.push({ device, surface: 'failure-recovery', outcome: 'PASS', writes: 0, errors: 0 });
      console.log(`PASS ${device}: início, catálogo, atividade, memória, teclado, layout, erro/recuperação e nenhuma escrita`);
      await session.close();
    }
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
  await writeFile(path.join(output, 'results.json'), JSON.stringify(results, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
