#!/usr/bin/env node
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { mkdtemp, writeFile, rm, mkdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { poll } from '../ai-worker/codex/connection.mjs';

const { chromium, devices } = createRequire(import.meta.url)('@playwright/test');
const ui = process.env.WORKER_CONNECTION_UI_URL || 'http://127.0.0.1:4173';
const backend = process.env.WORKER_CONNECTION_BACKEND_URL || 'http://127.0.0.1:8098';
assert.match(ui, /^http:\/\/127\.0\.0\.1:/);
assert.match(backend, /^http:\/\/127\.0\.0\.1:/);
const directory = await mkdtemp(join(tmpdir(), 'connection-browser-'));
const output = resolve('artifacts/worker-connection');
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox', '--disable-dev-shm-usage'] });
const summaries = [];
try {
  for (const [name, device] of [['desktop', { viewport: { width: 1365, height: 900 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
    const home = join(directory, name);
    await mkdir(home);
    await writeFile(join(home, 'fixture.json'), JSON.stringify({ loginDelay: 3500 }));
    const context = await browser.newContext(device);
    const page = await context.newPage();
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      if (path.startsWith('/api/ai/worker-connection')) {
        const response = await fetch(`${backend}${path}`, { method: route.request().method(), headers: { 'Content-Type': 'application/json' }, ...(route.request().postData() ? { body: route.request().postData() } : {}) });
        await route.fulfill({ status: response.status, body: await response.text(), contentType: 'application/json' });
      } else await route.fulfill({ status: 200, body: '[]', contentType: 'application/json' });
    });
    await page.goto(`${ui}/ai/pending-requests`, { waitUntil: 'domcontentloaded' });
    const panel = page.getByRole('region', { name: 'Conexão do Worker IA' });
    await panel.getByRole('button', { name: /Conectar Worker IA|Reconectar conta/ }).click();
    const running = poll(backend, { home, workspace: home, cacheDirectory: home, command: resolve('ai-worker/codex/fixtures/app-server.mjs'), noLock: true });
    await panel.getByLabel('Código de conexão').waitFor();
    assert.equal(await panel.getByRole('link', { name: 'Abrir conexão na OpenAI' }).getAttribute('href'), 'https://auth.openai.com/codex/device');
    assert.equal(await panel.getByLabel('Código de conexão').innerText(), 'SYNTHETIC');
    await panel.screenshot({ path: join(output, `${name}-code.png`) });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
    await running;
    await panel.getByText('Worker IA conectado. A sessão foi confirmada pelo executor.').waitFor();
    await page.reload();
    await panel.getByText('Worker IA conectado. A sessão foi confirmada pelo executor.').waitFor();
    await panel.screenshot({ path: join(output, `${name}-connected.png`) });
    summaries.push({ device: name, code: true, connected: true, reload: true, horizontalOverflow: false });
    await context.close();
  }
  const context = await browser.newContext();
  const page = await context.newPage();
  let failure = false;
  await page.route('**/api/**', route => {
    if (route.request().url().includes('/api/ai/worker-connection')) {
      if (failure) return route.abort();
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 99, status: 'EXPIRED', connected: false, workerAvailable: true, userCode: null }) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });
  await page.goto(`${ui}/ai/pending-requests`);
  await page.getByRole('button', { name: 'Gerar novo código' }).waitFor();
  failure = true;
  await page.getByRole('button', { name: 'Atualizar estado' }).click();
  await page.getByText('Não foi possível consultar a conexão. Tente atualizar o estado.').waitFor({ timeout: 30000 });
  summaries.push({ expiredRenewal: true, backendFailure: true });
  await context.close();
  await writeFile(join(output, 'browser-results.json'), JSON.stringify(summaries, null, 2));
  console.log(JSON.stringify(summaries));
} finally {
  await browser.close();
  await rm(directory, { recursive: true, force: true });
}
