import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const require = createRequire(new URL('../../../frontend/package.json', import.meta.url));
const { chromium, devices } = require('@playwright/test');
const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
const folder = 'artifacts/visual-personalization/browser';
await fs.mkdir(folder, { recursive: true });
const checks = [];
try {
  for (const device of ['Desktop', 'iPhone 15 Pro', 'Pixel 7']) {
    const context = await browser.newContext(device === 'Desktop' ? { viewport: { width: 1280, height: 900 } } : devices[device]);
    const page = await context.newPage(), errors = [];
    page.on('pageerror', e => errors.push(e.message));
    for (const product of [95501, 95502]) {
      const occasion = 'Jantar simulado ' + device + ' produto ' + product;
      const url = 'http://127.0.0.1:15175/ai/image-generator?mode=personalized-preparation&productId=' + product + '&commercialPlanId=' + product + '&experimentId=' + product;
      await page.goto(url);
      await page.getByLabel('Ocasião e condições práticas *').fill(occasion);
      await page.getByLabel('Peças disponíveis, uma por linha *').fill('camisa azul\ncalça bege\ntênis branco');
      await page.getByLabel('Preferências práticas *').fill('Conforto e praticidade');
      await page.getByLabel('Confirmo entrada simulada').check();
      const before = await (await fetch('http://127.0.0.1:15184/fixture/stats')).json();
      await page.getByRole('button', { name: 'Preparar minhas três opções', exact: true }).click();
      await page.getByRole('link', { name: 'Abrir minha entrega privada' }).waitFor();
      const link = await page.getByRole('link', { name: 'Abrir minha entrega privada' }).getAttribute('href');
      const broken = device === 'Desktop' && product === 95501;
      const pump = await fetch('http://127.0.0.1:15184/fixture/worker' + (broken ? '?loseCallback=true' : ''), { method: 'POST' });
      assert.equal(pump.status, broken ? 503 : 200);
      await page.getByRole('button', { name: 'Consultar resultado preservado' }).click();
      await page.getByRole('img', { name: 'Painel gerado para a ocasião e as peças informadas' }).waitFor();
      const firstImage = await page.getByRole('img').getAttribute('src');
      const key = 'visual-preparation:' + product + ':' + product + ':' + product;
      const saved = await page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key);
      if (broken) {
        const recovered = await fetch('http://127.0.0.1:15184/fixture/worker', { method: 'POST' }); assert.equal(recovered.status, 200);
      }
      await page.reload();
      await page.getByRole('img').waitFor();
      assert.equal(await page.getByRole('img').getAttribute('src'), firstImage);
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
      await page.screenshot({ path: folder + '/' + device.replaceAll(' ', '-') + '-' + product + '-hub.png', fullPage: true });
      const privatePage = await context.newPage();
      await privatePage.goto(link);
      await privatePage.getByRole('img', { name: 'Três combinações geradas para esta entrada' }).waitFor();
      assert.ok((await privatePage.getByRole('heading', { level: 2 }).textContent()).includes(occasion));
      assert.equal(await privatePage.getByRole('img').getAttribute('src'), firstImage);
      assert.equal(new URL(privatePage.url()).hash, '');
      await privatePage.reload();
      await privatePage.getByRole('img').waitFor();
      assert.ok(await privatePage.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
      await privatePage.screenshot({ path: folder + '/' + device.replaceAll(' ', '-') + '-' + product + '-delivery.png', fullPage: true });
      const wrong = await fetch('http://127.0.0.1:18096/api/pde/visual-personalization/v1/preparations/' + saved.jobId,
        { headers: { 'X-PDE-Visual-Session': 'wrong-session' } });
      assert.equal(wrong.status, 401);
      const stats = await (await fetch('http://127.0.0.1:15184/fixture/stats')).json();
      assert.equal(stats.providerCalls - before.providerCalls, 1);
      assert.equal(stats.spool.length, 0);
      const request = stats.requests.at(-1);
      assert.equal(JSON.parse(request.input[2].content).occasion, occasion);
      assert.equal(request.metadata.mh_job_id, saved.jobId);
      assert.equal(request.service_tier, 'flex');
      checks.push({ device, product, jobId: saved.jobId, inputBound: true, callbacks: stats.callbacks - before.callbacks, providerCalls: 1, recovered: true, errorFree: errors.length === 0 });
      assert.deepEqual(errors, []);
      await privatePage.close();
      await page.evaluate(key => sessionStorage.removeItem(key), key);
    }
    await context.close();
  }
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  await page.goto('http://127.0.0.1:15175/ai/image-generator?mode=personalized-preparation&productId=95501&commercialPlanId=95501&experimentId=95501');
  await page.getByLabel('Ocasião e condições práticas *').fill('Ocasião de recuperação de custo');
  await page.getByLabel('Peças disponíveis, uma por linha *').fill('camisa verde\ncalça branca');
  await page.getByLabel('Preferências práticas *').fill('Conforto');
  await page.getByLabel('Confirmo entrada simulada').check();
  await page.getByRole('button', { name: 'Preparar minhas três opções', exact: true }).click();
  await page.getByRole('link', { name: 'Abrir minha entrega privada' }).waitFor();
  const link = await page.getByRole('link', { name: 'Abrir minha entrega privada' }).getAttribute('href');
  const count = (await (await fetch('http://127.0.0.1:15184/fixture/stats')).json()).providerCalls;
  assert.equal((await fetch('http://127.0.0.1:18095/fixture/pricing/missing', { method: 'POST' })).status, 200);
  assert.equal((await fetch('http://127.0.0.1:15184/fixture/worker', { method: 'POST' })).status, 200);
  await page.getByRole('button', { name: 'Consultar resultado preservado' }).click();
  await page.getByRole('button', { name: 'Reconciliar a mesma resposta' }).waitFor();
  const saved = await page.evaluate(() => JSON.parse(sessionStorage.getItem('visual-preparation:95501:95501:95501')));
  const state = await (await fetch('http://127.0.0.1:18095/api/pde/visual-personalization/v1/preparations/' + saved.jobId,
    { headers: { 'X-PDE-Visual-Session': saved.accessToken } })).json();
  assert.equal(state.status, 'PDE_COST_PENDING'); assert.ok(state.imageBase64); assert.equal(state.providerCalls, 1);
  const budget = await (await fetch('http://127.0.0.1:18095/api/pde/visual-personalization/v1/products/95501/context?commercialPlanId=95501&experimentId=95501')).json();
  assert.equal(budget.budget.costComplete, false);
  const blocked = await fetch('http://127.0.0.1:18095/api/pde/visual-personalization/v1/products/95501/preparations', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ commercialPlanId: 95501, experimentId: 95501,
      productVersion: budget.budget.productVersion, authorizationHash: budget.budget.authorizationHash, syntheticConsent: true,
      accessToken: 'b'.repeat(64), operationKey: 'b'.repeat(64), input: saved.input }) });
  assert.equal(blocked.status, 409);
  await page.goto(link);
  await page.getByRole('img').waitFor();
  await page.getByText('Dados da homologação', { exact: true }).click();
  assert.equal((await fetch('http://127.0.0.1:18095/fixture/pricing/available', { method: 'POST' })).status, 200);
  await page.getByRole('button', { name: 'Reconciliar a mesma resposta' }).click();
  await page.getByRole('button', { name: 'Reconciliar a mesma resposta' }).waitFor({ state: 'detached' });
  const completed = await (await fetch('http://127.0.0.1:18095/api/pde/visual-personalization/v1/preparations/' + saved.jobId,
    { headers: { 'X-PDE-Visual-Session': saved.accessToken } })).json();
  assert.equal(completed.status, 'PDE_COMPLETED'); assert.equal(completed.imageBase64, state.imageBase64);
  assert.equal((await (await fetch('http://127.0.0.1:15184/fixture/stats')).json()).providerCalls, count + 1);
  checks.push({ device: 'Desktop', product: 95501, jobId: saved.jobId, costReconciliation: true, providerCalls: 1, recovered: true });
  await fs.writeFile(folder + '/recovery-state.json', JSON.stringify({ jobId: saved.jobId, token: saved.accessToken }), { mode: 0o600 });
  await context.close();
  await fs.writeFile(folder + '/summary.json', JSON.stringify({ result: 'PASS', externalProviderCalls: 0, simulations: checks }, null, 2));
  console.log(JSON.stringify({ result: 'PASS', devices: 3, products: 2, simulations: checks.length, externalProviderCalls: 0 }));
} finally { await browser.close(); }
