// Confere a orientação real do backend na interface local sem chamar produção ou modelos.
const fs = require('node:fs');
const path = require('node:path');
const { chromium, devices, expect } = require('@playwright/test');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'artifacts/flow-recovery');
const history = JSON.parse(fs.readFileSync(path.join(output, 'local-history.json'), 'utf8'));
const base = process.env.BROWSER_BASE_URL || 'http://127.0.0.1:5177';
if (!['localhost', '127.0.0.1'].includes(new URL(base).hostname)) throw new Error('Use somente frontend local.');
const control = {
  id: 99995, productId: history.productId, processDefinitionId: history.selectedProcessDefinitionId,
  chainId: 99027, sourceReference: history.currentExecutionReference, status: 'BLOCKED',
  reason: 'Correção pendente na origem: Atena precisa preservar a entrada existente.',
  currentActivityId: history.currentActivityId, currentActivityName: 'Reconciliar estratégia', currentOwnerName: 'Atena',
  totalActivities: 3, completedActivities: 0, remainingActivities: 3, omittedActivities: 0,
  completionPercentage: 0, knownCostUsd: null, costCoverage: 'NOT_REPORTED',
  canStart: false, canPause: false, canResume: false, automaticExecution: false,
  financialReviewUrl: '/financial/execution?productId=7&chainId=99027&cycleId=99119',
  parentProcesses: [], subprocesses: [], revision: 1,
};
(async () => {
  const browser = await chromium.launch({executablePath: '/usr/bin/chromium', args: ['--no-sandbox', '--disable-dev-shm-usage']});
  try {
    for (const [name, options] of [['desktop', { viewport: { width: 1440, height: 1000 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      page.on('pageerror', e => console.error('Página: ' + e.message));
      const writes = [];
      await page.route('**/api/**', async route => {
        const request = route.request();
        if (!new URL(request.url()).pathname.startsWith('/api/')) return route.continue();
        if (request.method() !== 'GET') writes.push(request.method());
        const url = new URL(request.url());
        let body = [];
        if (url.pathname.endsWith('/activity-executions')) body = history;
        else if (url.pathname.endsWith('/automation/v1')) body = control;
        else if (url.pathname.includes('value-chain-position')) body = {};
        await route.fulfill({status: 200, contentType: 'application/json', body: JSON.stringify(body)});
      });
      await page.goto(`${base}/products/${history.productId}/value-chain-history/processes/${history.selectedProcessDefinitionId}/activities?chainId=99027&sourceReference=${encodeURIComponent(history.currentExecutionReference)}`, {waitUntil: 'networkidle'});
      fs.writeFileSync(path.join(output, `debug-${name}.txt`), await page.locator('body').innerText());
      await expect(page.getByText('Correção pendente na origem: Atena precisa preservar a entrada existente.')).toBeVisible();
      await expect(page.getByText('Atena precisa alinhar a estratégia à base disponível e ao protocolo da candidata antes desta passagem.').first()).toBeVisible();
      await expect(page.getByRole('link', {name: 'Ver demonstração financeira'})).toBeVisible();
      await expect(page.getByRole('button', {name: 'Retomar processo'})).toHaveCount(0);
      expect(writes).toEqual([]);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
      await page.screenshot({path: path.join(output, `local-${name}.png`), fullPage: true});
      fs.writeFileSync(path.join(output, `local-${name}.txt`), await page.locator('body').innerText());
      console.log(`${name}: origem correta, dependentes bloqueados, financeiro disponível, zero chamadas de execução`);
      await context.close();
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
