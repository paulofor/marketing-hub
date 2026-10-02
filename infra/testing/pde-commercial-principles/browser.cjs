const { chromium, devices } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../../..');
const expected = require(process.env.PRINCIPLES_EXPECTED || './expected.json');
const live = process.env.PRINCIPLES_LIVE === 'true';
const base = process.env.PRINCIPLES_UI_URL || 'http://127.0.0.1:4179';
const output = process.env.PRINCIPLES_OUTPUT || '/tmp/mkt-five-points/browser';
const expectedChain = process.env.PRINCIPLES_CHAIN
  ? JSON.parse(fs.readFileSync(process.env.PRINCIPLES_CHAIN)) : undefined;

async function main() {
  const processes = live
    ? await (await fetch(new URL('/api/business-processes', base))).json()
    : JSON.parse(fs.readFileSync(process.env.PRINCIPLES_PROCESSES || path.join(root, 'backend/ads-service/target/principles/processes.json')));
  let chain = expectedChain;
  if (live && expectedChain) {
    const chains = await (await fetch(new URL('/api/business-process-chains', base))).json();
    const selected = chains.find((item) => item.chainCode === expectedChain.chainCode && item.versionNumber === expectedChain.versionNumber);
    assert.ok(selected, 'cadeia esperada ausente');
    chain = await (await fetch(new URL(`/api/business-process-chains/${selected.id}`, base))).json();
  }
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
          if (chain && url.pathname === '/api/business-process-chains') data = [chain];
          if (chain && url.pathname === `/api/business-process-chains/${chain.id}`) data = chain;
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
      if (chain) {
        await page.goto(`${base}/business-process-chains?chainId=${chain.id}`, { waitUntil: 'domcontentloaded' });
        const items = page.locator('.business-process-chain-process');
        await items.first().waitFor({ timeout: 30000 });
        assert.equal(await items.count(), 6);
        for (const [index, item] of expectedChain.processes.entries()) {
          assert.ok((await items.nth(index).innerText()).includes(item.valueContribution), `${name}: contribuição ausente na cadeia`);
          const actual = chain.processes[index];
          assert.equal(actual.processCode, item.processCode);
          assert.equal(actual.versionNumber, item.versionNumber);
          assert.ok((await items.nth(index).locator('a').first().getAttribute('href')).includes(`processId=${actual.processDefinitionId}`));
        }
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `${name}: cadeia com transbordamento`);
        await page.screenshot({ path: path.join(output, `chain-${name}.png`), fullPage: true });
      }
      assert.deepEqual(errors, []);
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(results, null, 2));
  console.log(JSON.stringify({ live, pages: results.length, chainPages: chain ? 3 : 0, objectives: results.reduce((n, r) => n + r.checked, 0) }));
}

main().catch((e) => { console.error(e); process.exitCode = 1; });
