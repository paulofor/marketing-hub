// Executa o build real em navegadores com contratos produzidos pelo controller e gates locais.
const { chromium, devices, expect } = require('@playwright/test');
const { readFile, mkdir, writeFile } = require('node:fs/promises');
const { createServer } = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const output = path.resolve(process.env.VEGA_ONE_ACTION_OUTPUT || 'artifacts/vega-one-action/browser');
  const contract = path.resolve(process.env.VEGA_ONE_ACTION_SCREEN || 'artifacts/vega-one-action/prep/screen.json.recovery.json');
  const uiDir = path.resolve('frontend/dist');
  const load = async file => JSON.parse(await readFile(file, 'utf8'));
  const snapshots = {
    initial: await load(contract),
    PENDING: await load(contract + '.after.json'),
    IN_PROGRESS: await load(contract + '.IN_PROGRESS.json'),
    BLOCKED: await load(contract + '.BLOCKED.json'),
    COMPLETED: await load(contract + '.COMPLETED.json'),
  };
  const command = await load(contract + '.command.json');
  const cycle = await load('infra/testing/vega-harness-readiness/fixtures/cycle-context.json');
  const position = await load('infra/testing/vega-harness-readiness/fixtures/position.json');
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, 'http://localhost').pathname;
      const file = name.startsWith('/assets/') ? path.join(uiDir, name) : path.join(uiDir, 'index.html');
      assert(file.startsWith(uiDir + path.sep));
      res.setHeader('Content-Type', file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html');
      res.end(await readFile(file));
    } catch { res.statusCode = 404; res.end(); }
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN || '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  const results = [];
  try {
    for (const [name, profile] of [['desktop', { viewport: { width: 1440, height: 1080 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const session = await browser.newContext(profile);
      const page = await session.newPage();
      let state = 'initial', posts = 0, historyReads = 0, progressReads = 0, progressFailure = false, cycleFailure = false, historyFailure = false;
      let release, historyFailureResponses = 0, cycleFailureResponses = 0;
      const errors = [], unexpected = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.route('**/*', async route => {
        const request = route.request(), url = new URL(request.url());
        if (url.pathname.startsWith('/api/')) {
          if (request.method() === 'POST') {
            assert.equal(url.pathname, '/api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests');
            assert.equal(url.searchParams.get('learningCycleId'), '2');
            assert.equal(request.postData(), null);
            posts++;
            if (posts === 1) return route.fulfill({ status: 503, json: { message: 'Falha simulada no envio da tarefa. Tente novamente.' } });
            assert.equal(posts, 2, 'Somente uma falha controlada e uma criação são permitidas.');
            await new Promise(resolve => { release = resolve; });
            state = 'PENDING';
            return route.fulfill({ status: 200, json: command });
          }
          assert.equal(request.method(), 'GET');
          if (url.pathname.endsWith('/execution-progress')) {
            progressReads++;
            assert.equal(url.searchParams.get('sourceReference'), 'experiment:92');
            return progressFailure ? route.fulfill({ status: 503, json: { message: 'Falha temporária no acompanhamento' } })
              : route.fulfill({ json: [{ taskId: state === 'initial' ? 377 : 900378, status: state, updatedAt: state }] });
          }
          if (url.pathname.endsWith('/activity-executions')) {
            historyReads++;
            if (historyFailure) {
              historyFailureResponses++;
              return route.fulfill({ status: 503, json: { message: 'Falha temporária na leitura do resultado' } });
            }
            assert.equal(url.searchParams.get('learningCycleId'), '2');
            assert.equal(url.searchParams.get('chainId'), '14');
            return route.fulfill({ json: snapshots[state] });
          }
          if (url.pathname.endsWith('/process-context')) {
            if (cycleFailure) {
              cycleFailureResponses++;
              return route.fulfill({ status: 503, json: { message: 'Contexto temporariamente indisponível' } });
            }
            return route.fulfill({ json: cycle });
          }
          if (url.pathname === '/api/products/value-chain-positions/4') return route.fulfill({ json: position });
          if (['/api/creatives/video-review', '/api/ops-monitor/v1/modules/availability', '/api/facebook/configuration-status'].includes(url.pathname)) return route.fulfill({ json: [] });
          unexpected.push(url.pathname); return route.abort();
        }
        if (url.origin !== origin) { unexpected.push(url.origin); return route.abort(); }
        return route.continue();
      });
      await page.goto(`${origin}/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-technicalHomologation`, { waitUntil: 'networkidle' });
      const card = page.locator('#activity-technicalHomologation');
      await expect(card.locator('.product-process-activity-control').getByRole('button')).toHaveCount(1);
      await expect(card.getByRole('button', { name: 'Criar tarefa de correção' })).toBeEnabled();
      await expect(page.locator('body')).toContainText('2º ciclo de vendas · Experimento #92');
      await expect(page.locator('body')).toContainText('Aprendizado dos ciclos anteriores');
      const startingPolls = progressReads;
      await expect.poll(() => progressReads, { timeout: 12000 }).toBeGreaterThan(startingPolls + 1);
      const stableReads = historyReads;
      await expect.poll(() => progressReads, { timeout: 12000 }).toBeGreaterThan(startingPolls + 3);
      assert.equal(historyReads, stableReads, 'Estado inalterado não relê a auditoria.');
      await card.getByRole('button', { name: 'Criar tarefa de correção' }).click();
      const sendError = card.getByRole('alert').filter({ hasText: 'Falha simulada no envio' });
      await expect(sendError).toBeInViewport();
      await page.screenshot({ path: path.join(output, `${name}-send-error.png`) });
      await card.getByRole('button', { name: 'Criar tarefa de correção' }).click();
      const pendingButton = card.getByRole('button', { name: 'Criando tarefa...' });
      await expect(pendingButton).toBeDisabled();
      await expect(pendingButton.locator('.spinner-border')).toBeVisible();
      await expect.poll(() => typeof release).toBe('function'); release();
      const tracking = card.getByLabel('Acompanhamento da tarefa');
      await expect(tracking).toContainText('Tarefa #900378');
      await expect(tracking.getByRole('status')).toBeInViewport();
      await expect(card.getByRole('button', { name: 'Tarefa na fila' })).toBeDisabled();
      await page.screenshot({ path: path.join(output, `${name}-pending.png`) });
      await page.evaluate(() => scrollTo(0, 0));
      state = 'IN_PROGRESS';
      await expect(tracking.getByRole('status')).toContainText('Em execução', { timeout: 12000 });
      assert.equal(await page.evaluate(() => scrollY), 0, 'Atualizar o estado não reposiciona a leitura do usuário.');
      await expect(card.getByRole('button', { name: 'Tarefa em execução' })).toBeDisabled();
      await page.reload({ waitUntil: 'networkidle' });
      await expect(tracking).toContainText('Tarefa #900378 · Em execução');
      assert.equal(posts, 2, 'Reabrir a tela não dispara nova tarefa.');
      progressFailure = true;
      await expect(tracking.getByRole('alert')).toContainText('Não foi possível atualizar', { timeout: 20000 });
      await expect(tracking).toContainText('Tarefa #900378 · Em execução');
      progressFailure = false;
      await expect(tracking.getByRole('alert')).toHaveCount(0, { timeout: 12000 });
      historyFailure = true; cycleFailure = true;
      state = 'BLOCKED';
      await expect.poll(() => historyFailureResponses, { timeout: 12000 }).toBeGreaterThan(0);
      await expect.poll(() => cycleFailureResponses, { timeout: 12000 }).toBeGreaterThan(0);
      await expect(tracking.getByRole('alert')).toContainText('Não foi possível atualizar', { timeout: 20000 });
      await expect(tracking).toContainText('Tarefa #900378 · Em execução');
      await expect(page.locator('body')).toContainText('Aprendizado dos ciclos anteriores');
      historyFailure = false; cycleFailure = false;
      await expect(tracking).toContainText('Tarefa #900378 · Bloqueada', { timeout: 20000 });
      await expect(tracking.getByRole('alert')).toHaveCount(0, { timeout: 12000 });
      await expect(tracking).toContainText('Disponibilizar a versão corrigida antes de homologar.');
      await expect(card.getByRole('button', { name: 'Criar tarefa de correção' })).toBeEnabled();
      await tracking.getByText('Ver motivo registrado pelo agente').click();
      await expect(tracking).toContainText('A versão executável ainda não foi disponibilizada.');
      await tracking.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, `${name}-blocked.png`) });
      // O cenário de conclusão usa a saída do gate real com a correção sintética válida.
      state = 'COMPLETED';
      await expect(tracking).toContainText('Tarefa #900378 · Concluída', { timeout: 12000 });
      await expect(card.getByRole('button', { name: 'Correção concluída' })).toBeDisabled();
      await expect(card.locator('.product-process-activity-control').getByRole('button')).toHaveCount(1);
      await expect(tracking.getByText('A atividade ainda não foi concluída.')).toHaveCount(0);
      await card.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, `${name}-completed.png`) });
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
      assert.equal(posts, 2);
      assert.deepEqual(errors, []); assert.deepEqual(unexpected, []);
      results.push({ device: name, passed: true, posts, historyReads, progressReads, historyFailureResponses, cycleFailureResponses });
      console.log(`PASS ${name}: ação única, retorno na viewport, fila, execução, reload, erro de conexão, bloqueio, conclusão e isolamento`);
      await session.close();
    }
    await writeFile(path.join(output, 'results.json'), JSON.stringify(results, null, 2));
  } finally { await browser.close(); await new Promise(resolve => server.close(resolve)); }
})().catch(error => { console.error(error); process.exitCode = 1; });
