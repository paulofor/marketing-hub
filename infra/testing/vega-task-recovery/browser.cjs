// Homologa o comando de correção com contratos produzidos pelo controller real em teste local.
const { chromium, devices, expect } = require('@playwright/test');
const { readFile, mkdir } = require('node:fs/promises');
const { createServer } = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const output = path.resolve(process.env.VEGA_RECOVERY_OUTPUT || 'artifacts/vega-task-recovery/browser');
  const contract = path.resolve(process.env.VEGA_RECOVERY_SCREEN || 'artifacts/vega-task-recovery/screen.json.recovery.json');
  const uiDir = path.resolve('frontend/dist');
  const load = async file => JSON.parse(await readFile(file, 'utf8'));
  const screen = await load(contract);
  const after = await load(contract + '.after.json');
  const command = await load(contract + '.command.json');
  const next = await load(contract + '.next-work.json');
  const nextAfter = await load(contract + '.after-next-work.json');
  const context = await load('infra/testing/vega-harness-readiness/fixtures/cycle-context.json');
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
  try {
    for (const [name, profile] of [
      ['desktop', { viewport: { width: 1440, height: 1050 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const session = await browser.newContext(profile);
      const page = await session.newPage();
      const errors = [];
      const unexpected = [];
      let submitted = 0;
      let created = false;
      let releaseCommand;
      page.on('pageerror', error => errors.push(error.message));
      await page.route('**/*', async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.startsWith('/api/')) {
          if (request.method() === 'POST') {
            assert.equal(url.pathname, '/api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests');
            assert.equal(url.searchParams.get('learningCycleId'), '2');
            assert.equal(request.postData(), null);
            submitted++;
            if (submitted === 1) return route.fulfill({ status: 503, json: { message: 'Falha simulada ao criar a tarefa. Tente novamente.' } });
            await new Promise(resolve => { releaseCommand = resolve; });
            created = true;
            return route.fulfill({ status: 200, json: command });
          }
          assert.equal(request.method(), 'GET');
          let data;
          if (url.pathname.endsWith('/activity-executions')) {
            assert.equal(url.searchParams.get('learningCycleId'), '2');
            assert.equal(url.searchParams.get('chainId'), '14');
            data = created ? after : screen;
          } else if (url.pathname.endsWith('/process-context')) {
            data = { ...context, nextWork: created ? nextAfter : next };
          } else if (url.pathname === '/api/products/value-chain-positions/4') data = position;
          else if (['/api/creatives/video-review', '/api/ops-monitor/v1/modules/availability', '/api/facebook/configuration-status'].includes(url.pathname)) data = [];
          else { unexpected.push(url.pathname); return route.abort(); }
          return route.fulfill({ status: 200, json: data });
        }
        if (url.origin !== origin) { unexpected.push(url.origin); return route.abort(); }
        await route.continue();
      });
      await page.goto(`${origin}/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-technicalHomologation`, { waitUntil: 'networkidle' });
      const card = page.locator('#activity-technicalHomologation');
      await expect(card.getByRole('button', { name: 'Reiniciar tarefa' })).toBeDisabled();
      const recovery = card.getByRole('button', { name: 'Criar tarefa de correção' });
      await expect(recovery).toBeEnabled();
      await expect(card).toContainText('Responsável: Dédalo');
      await expect(page.locator('body')).toContainText('2º ciclo de vendas · Experimento #92');
      await expect(page.locator('body')).toContainText('Aprendizado dos ciclos anteriores');
      await expect(page.locator('#activity-prototypeCorrection')).not.toContainText('versão histórica');
      await recovery.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, name + '-before.png') });
      await recovery.click();
      await expect(page.getByRole('alert').filter({ hasText: 'Falha simulada ao criar a tarefa' })).toBeVisible();
      await expect(recovery).toBeEnabled();
      await recovery.click();
      const pending = card.getByRole('button', { name: 'Criando tarefa...' });
      await expect(pending).toBeDisabled();
      await expect(pending.locator('.spinner-border')).toBeVisible();
      await expect(page.locator('#activity-prototypeCorrection').getByRole('button', { name: 'Executando...' })).toBeDisabled();
      await expect.poll(() => typeof releaseCommand).toBe('function');
      releaseCommand();
      await expect(page.locator('#activity-prototypeCorrection')).toContainText('Tarefa #900378');
      await expect(card).toContainText('Tarefa #377');
      await expect(card.getByRole('button', { name: 'Criar tarefa de correção' })).toBeDisabled();
      assert.equal(submitted, 2, 'Uma falha simulada e uma criação, sem duplicação automática.');
      await page.reload({ waitUntil: 'networkidle' });
      await expect(page.locator('#activity-prototypeCorrection')).toContainText('Tarefa #900378');
      await expect(card.getByRole('button', { name: 'Criar tarefa de correção' })).toBeDisabled();
      assert.equal(submitted, 2);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
      await card.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, name + '-after.png') });
      assert.deepEqual(errors, []);
      assert.deepEqual(unexpected, []);
      console.log(`PASS ${name}: recuperação, erro HTTP, carregamento, criação, releitura, ciclo, aprendizado e isolamento`);
      await session.close();
    }
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
