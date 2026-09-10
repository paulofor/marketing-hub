// Verifica o cliente administrativo local com respostas produzidas pelo teste HTTP do backend.
const { chromium, devices } = require('playwright');
const { readFile, mkdir } = require('node:fs/promises');
const { createServer } = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const fixtureDir = path.resolve(process.env.VEGA377_FIXTURES || 'infra/testing/vega-harness-readiness/fixtures');
  const uiDir = path.resolve(process.env.VEGA377_UI_DIR || 'frontend/dist');
  const screenPath = path.resolve(process.env.VEGA377_SCREEN || 'artifacts/vega377/screen.json');
  const output = path.resolve(process.env.VEGA377_BROWSER_OUTPUT || 'artifacts/vega377/browser');
  const screen = JSON.parse(await readFile(screenPath, 'utf8'));
  const context = JSON.parse(await readFile(path.join(fixtureDir, 'cycle-context.json'), 'utf8'));
  context.nextWork = JSON.parse(await readFile(screenPath + '.next-work.json', 'utf8'));
  const position = JSON.parse(await readFile(path.join(fixtureDir, 'position.json'), 'utf8'));
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, 'http://localhost').pathname;
      const file = name.startsWith('/assets/') ? path.join(uiDir, name) : path.join(uiDir, 'index.html');
      assert(file.startsWith(uiDir + path.sep));
      const body = await readFile(file);
      res.setHeader('Content-Type', file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html');
      res.end(body);
    } catch {
      res.statusCode = 404;
      res.end();
    }
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN || '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  try {
    for (const [name, profile] of [
      ['desktop', { viewport: { width: 1440, height: 900 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const session = await browser.newContext(profile);
      const page = await session.newPage();
      const errors = [];
      const unexpected = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.startsWith('/api/')) {
          assert.equal(request.method(), 'GET', 'A página não pode criar tarefas automaticamente.');
          let data;
          if (url.pathname.endsWith('/activity-executions')) data = screen;
          else if (url.pathname.endsWith('/process-context')) data = context;
          else if (url.pathname === '/api/products/value-chain-positions/4') data = position;
          else if (url.pathname === '/api/creatives/video-review') data = [];
          else if (url.pathname === '/api/ops-monitor/v1/modules/availability') data = [];
          else if (url.pathname === '/api/facebook/configuration-status') data = [];
          else { unexpected.push(url.pathname); return route.abort(); }
          return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
        }
        if (url.origin !== origin) { unexpected.push(url.origin); return route.abort(); }
        await route.continue();
      });
      await page.goto(`${origin}/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-technicalHomologation`, { waitUntil: 'networkidle' });
      const card = page.locator('#activity-technicalHomologation');
      await card.waitFor();
      assert.match(await card.innerText(), /Responsável: Psique/);
      assert.match(await card.innerText(), /testes automáticos com o harness/);
      assert.match(await card.innerText(), /não possui uma URL executável aceita/);
      assert.match(await card.innerText(), /10\/09\/2026/);
      assert.equal(await card.getByRole('button', { name: /Reiniciar tarefa|Executar atividade/ }).count(), 0);
      assert.equal(await page.getByText('Responsável: Harness', { exact: true }).count(), 0);
      assert.match(await page.locator('body').innerText(), /2º ciclo de vendas · Experimento #92/);
      assert.match(await page.locator('body').innerText(), /Aprendizado dos ciclos anteriores/);
      await card.scrollIntoViewIfNeeded();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
      await page.screenshot({ path: path.join(output, name + '.png'), fullPage: true });
      await card.screenshot({ path: path.join(output, name + '-card.png') });
      assert.deepEqual(errors, []);
      assert.deepEqual(unexpected, []);
      console.log(`PASS ${name}: Psique, ferramenta, bloqueio, ciclo, memória, layout e nenhuma escrita`);
      await session.close();
    }
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
