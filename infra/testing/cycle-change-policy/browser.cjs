/** Homologa edição e histórico pela UI com API/MySQL da fixture local. */
const { chromium, devices } = require('playwright');
const fs = require('fs');
const assert = require('assert/strict');
const base = 'http://127.0.0.1:15173';
const out = '.tmp/cycle-change';
(async () => {
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  try {
    for (const [name, device] of [['desktop', { viewport: { width: 1440, height: 1050 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', route => {
        const u = new URL(route.request().url());
        if (u.protocol === 'data:' || u.hostname === '127.0.0.1' || u.hostname === 'localhost') return route.continue();
        return route.abort();
      });
      page.on('dialog', dialog => dialog.accept());
      await page.goto(base + '/business-process-chains?chainId=91002');
      await page.getByRole('button', { name: 'Criar nova versão da cadeia' }).click();
      const purpose = page.getByLabel('Propósito da cadeia');
      await purpose.fill('QA ' + name + ': cada mudança exige novo ciclo e novo experimento; histórico preservado.');
      await page.getByRole('button', { name: 'Salvar rascunho da cadeia' }).click();
      await page.getByText('Rascunho salvo. Revise a cadeia antes de publicar.').waitFor();
      const id = Number(new URL(page.url()).searchParams.get('chainId'));
      await page.getByRole('button', { name: 'Publicar cadeia', exact: true }).click();
      await page.getByText('Cadeia publicada. As execuções anteriores mantêm suas versões.').waitFor();
      const published = await (await context.request.get(base + '/api/business-process-chains/' + id)).json();
      assert.equal(published.status, 'PUBLISHED');
      assert.match(published.purpose, /novo ciclo e novo experimento/);
      await page.screenshot({ path: out + '/local-' + name + '-published.png', fullPage: true });
      await page.goto(base + '/business-process-chains?chainId=91002');
      await page.getByRole('button', { name: 'Criar nova versão da cadeia' }).waitFor();
      assert.equal(new URL(page.url()).searchParams.get('chainId'), '91002');
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 2);
      assert.equal(overflow, false, name + ': overflow horizontal');
      assert.deepEqual(errors, []);
      console.log('PASS', name, 'rascunho, gravação, publicação, histórico exato', id);
      await context.close();
    }
  } finally { await browser.close(); }
})().catch(e => { console.error(e.message); process.exitCode = 1; });
