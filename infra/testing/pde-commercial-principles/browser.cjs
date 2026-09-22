const { chromium, devices } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../../..');
const expected = require('./expected.json');
const live = process.env.PRINCIPLES_LIVE === 'true';
const base = process.env.PRINCIPLES_UI_URL || 'http://127.0.0.1:4179';
const output = process.env.PRINCIPLES_OUTPUT || '/tmp/mkt-five-points/browser';

async function main() {
  const processes = live
    ? await (await fetch(new URL('/api/business-processes', base))).json()
    : JSON.parse(fs.readFileSync(path.join(root, 'backend/ads-service/target/principles/processes.json')));
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  const results = [];
  fs.mkdirSync(output, { recursive: true });
  try {
    for (const [name, settings] of [
      ['desktop', { viewport: { width: 1440, height: 1000 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const context = await browser.newContext(settings);
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', (e) => errors.push(e.message));
      if (!live) {
        await page.route('**/api/**', async (route) => {
          const url = new URL(route.request().url());
          if (!url.pathname.startsWith("/api/")) return route.continue();
          let data = [];
          if (url.pathname === '/api/business-processes') data = processes;
          const composition = url.pathname.match(/\/business-processes\/(\d+)\/composition$/);
          if (composition) {
            const process = processes.find((p) => p.id === Number(composition[1]));
            const children = processes.filter((p) => p.parentProcessCode === process.processCode);
            data = { process, subprocesses: children, subprocessCount: children.length };
          }
          await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
        });
      }
      for (const [code, contract] of Object.entries(expected)) {
        const process = processes.find((p) => p.processCode === code && p.versionNumber === contract.targetVersion);
        assert.ok(process, `${code}: versão esperada ausente`);
        await page.goto(`${base}/business-processes?processId=${process.id}`, { waitUntil: 'domcontentloaded' });
        console.log(JSON.stringify({device:name,process:code}));
        const diagram = page.locator('.process-diagram, .learning-cycle-diagram');
        await diagram.waitFor({ timeout: 90000 });
        await page.getByText(Object.values(contract.objectives)[0], { exact: false }).first().waitFor({ timeout: 30000 });
        const text = await diagram.innerText();
        for (const value of Object.values(contract.objectives)) assert.ok(text.includes(value), `${name}: objetivo incompleto em ${code}`);
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `${name}: transbordamento horizontal`);
        if (code === 'pde-communication-sales-journey') await page.screenshot({ path: path.join(output, `${name}.png`), fullPage: true });
        results.push({ device: name, processCode: code, version: contract.targetVersion, checked: Object.keys(contract.objectives).length });
      }
      assert.deepEqual(errors, []);
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(results, null, 2));
  console.log(JSON.stringify({ live, pages: results.length, objectives: results.reduce((n, r) => n + r.checked, 0) }));
}

main().catch((e) => { console.error(e); process.exitCode = 1; });
