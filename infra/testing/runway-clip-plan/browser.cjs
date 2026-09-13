// Confere a projeção real do backend em desktop e celulares, com todas as APIs simuladas.
const { chromium, devices, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

async function main() {
  const contract = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const output = process.argv[3];
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  try {
    for (const [name, options] of [
      ['desktop', { viewport: { width: 1440, height: 1000 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
      await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        if (url.pathname.startsWith('/api/')) {
          assert.equal(route.request().method(), 'GET', 'A conferência visual não deve escrever dados.');
          let json = [];
          if (url.pathname === '/api/sales-videos/projects/7') json = contract.project;
          else if (url.pathname.endsWith('/projects/7/autonomy/v1/cycles')) json = [contract.cycle];
          else if (url.pathname.endsWith('/studio/catalog')) json = { characters: [], captionPresets: [] };
          else if (url.pathname.endsWith('/projects/7/storyboard')) json = { scenes: [] };
          else if (url.pathname.endsWith('/sales-videos/profiles')) json = [{ id: 13, name: 'QA local', status: 'SCRIPT_READY' }];
          await route.fulfill({ json });
        } else if (url.origin === 'http://127.0.0.1:4173') await route.continue();
        else await route.abort();
      });
      await page.goto('http://127.0.0.1:4173/audio-video-studio/projects/7', { waitUntil: 'networkidle' });
      fs.writeFileSync(path.join(output, `${name}-page.txt`), await page.locator('body').innerText());
      fs.writeFileSync(path.join(output, `${name}-errors.json`), JSON.stringify(errors));
      const clips = page.getByText(/Plano de cortes: 2 clipes solicitados ao provider, com até 10s cada/);
      await expect(clips).toBeVisible();
      await expect(page.getByText(/Ciclo #11:/)).toBeVisible();
      await clips.scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(output, `${name}.png`) });
      assert.deepEqual(errors, [], `${name}: erros de página`);
      console.log(`PASS: ${name}, dois clipes de até 10s e nenhuma escrita ou API externa.`);
      await context.close();
    }
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
