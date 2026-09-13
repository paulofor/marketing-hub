// Confere contratos do backend e a prova no feed; toda escrita fica no double local.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium, devices, expect } = require('@playwright/test');

(async () => {
  const contract = JSON.parse(fs.readFileSync(process.env.MIRA_UI_CONTRACT, 'utf8'));
  const output = process.env.MIRA_UI_OUTPUT;
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox', '--disable-dev-shm-usage'] });
  const results = [];
  try {
    for (const [name, profile] of [['desktop', { viewport: { width: 1440, height: 1000 } }], ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const context = await browser.newContext({ ...profile, permissions: ['clipboard-read', 'clipboard-write'] });
      const page = await context.newPage();
      let phase = 'Correction', decisions = 0;
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', async route => {
        const request = route.request(), url = new URL(request.url());
        if (url.pathname.startsWith('/api/')) {
          let data;
          if (request.method() === 'POST') {
            assert.equal(phase, 'Human');
            assert.ok(url.pathname.endsWith('/activities/human/execution-requests'));
            const decision = request.postDataJSON();
            assert.equal(decision.decision, 'APPROVE');
            assert.equal(decision.operatorName, 'Operador QA local');
            assert.equal(decision.confirmationToken, 'qa-explicit-human-only');
            assert.equal(decision.evidenceReference, 'internal://qa/creative/reviewed');
            decisions++;
            phase = 'Complete';
            data = { processDefinitionId: contract.processId, productId: contract.productId, activityId: 'human', sourceReference: contract.automationComplete.sourceReference, operationalState: 'COMPLETED', objectiveAchieved: true, tasks: [], message: 'Decisão explícita da fixture local registrada.' };
          } else if (url.pathname.includes('/automation/v1')) data = contract['automation' + phase];
          else if (url.pathname.endsWith('/activity-executions')) data = contract['history' + phase];
          else if (url.pathname.endsWith('/process-context')) data = null;
          else if (url.pathname.includes('/value-chain-positions/')) data = { productId: contract.productId, chainDefinitionId: contract.chainId, chainName: 'Cadeia QA', chainVersion: 14, processDefinitionId: contract.processId, sequenceNumber: 4, processCount: 6, processMeasurements: [] };
          else if (url.pathname.endsWith('/configuration-status')) data = { accounts: [] };
          else data = [];
          return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
        }
        if (url.origin === 'http://127.0.0.1:4173') return route.continue();
        return route.abort();
      });
      const url = `http://127.0.0.1:4173/products/${contract.productId}/value-chain-history/processes/${contract.processId}/activities?chainId=${contract.chainId}`;
      await page.goto(url);
      await expect(page.getByRole('button', { name: 'Pausar', exact: true })).toBeVisible();
      const parent = page.locator('a[href*="/processes/94163/activities"]');
      await expect(parent.first()).toHaveAttribute('href', /chainId=94114#activity-creatives$/);
      await page.getByRole('button', { name: 'Copiar contexto do processo', exact: true }).click();
      const copied = await page.evaluate(() => navigator.clipboard.readText());
      assert.ok(copied.includes('product:94110@agent-validation-v1'));
      assert.ok(!copied.includes('experiment:'));
      await page.screenshot({ path: path.join(output, `${name}-correction.png`), fullPage: true });
      phase = 'Human';
      await page.reload();
      const form = page.locator('.product-process-human-decision');
      await expect(form).toBeVisible();
      const submit = form.getByRole('button', { name: 'Registrar decisão', exact: true });
      await expect(submit).toBeDisabled();
      assert.equal(decisions, 0);
      await page.screenshot({ path: path.join(output, `${name}-human.png`), fullPage: true });
      await form.getByLabel('Responsável').fill('Operador QA local');
      await form.getByLabel('Justificativa').fill('Peça local revisada pelos doubles, sem uso comercial.');
      await form.getByLabel('Evidência auditável').fill('internal://qa/creative/reviewed');
      await expect(submit).toBeDisabled();
      await form.getByRole('checkbox').check();
      await expect(submit).toBeEnabled();
      await submit.click();
      await expect(page.getByRole('progressbar', { name: 'Objetivos comprovados' })).toHaveAttribute('aria-valuenow', '100', { timeout: 15000 });
      assert.equal(decisions, 1);
      const width = await page.evaluate(() => ({ doc: document.documentElement.scrollWidth, view: innerWidth }));
      assert.ok(width.doc <= width.view + 1, JSON.stringify(width));
      assert.deepEqual(errors, []);
      await page.screenshot({ path: path.join(output, `${name}-complete.png`), fullPage: true });
      const preview = fs.readFileSync(process.env.MIRA_CREATIVE_PREVIEW).toString('base64');
      await page.setContent(`<html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0;background:#eee}img{display:block;width:100%;max-width:393px;height:auto;margin:auto}</style><img alt="Prévia local, sem publicação" src="data:image/png;base64,${preview}"></html>`);
      const image = page.getByRole('img');
      await expect(image).toBeVisible();
      assert.deepEqual(await image.evaluate(i => [i.naturalWidth, i.naturalHeight]), [1080, 1350]);
      await page.screenshot({ path: path.join(output, `${name}-proof.png`), fullPage: true });
      results.push({ profile: name, correctionVisible: true, parentContext: true, privateReferenceCopied: true, explicitDecisionRequired: true, oneLocalDecision: decisions, completionFromBackend: true, noOverflow: true, noJavascriptErrors: true, proofRendered: true });
      await context.close();
    }
  } finally { await browser.close(); }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
})().catch(error => { console.error(error); process.exit(1); });
