// Confere no navegador a fotografia produzida pelo SQL e coletor reais, sem tráfego externo.
const { chromium, devices, expect } = require('@playwright/test');
const { execFileSync } = require('node:child_process');
const { readFile, mkdir, writeFile } = require('node:fs/promises');
const assert = require('node:assert/strict');
const api = 'http://127.0.0.1:18091';
const base = 'http://127.0.0.1:15173';
const prefix = '/api/business-process-chains/learning-cycles/v1';
const output = process.env.LEAD_PORTAL_CYCLE_TEST_EVIDENCE_DIR;

(async () => {
  assert.ok(output, 'Informe a pasta da exportação SQL local');
  const evidence = JSON.parse(await readFile(`${output}/lead-portal-measurement.json`, 'utf8'));
  assert.equal(evidence.experimentId, 91001);
  assert.equal(evidence.sources.leadPortalAnalytics.productVersion, 'fixture-commercial-91001-v12');
  assert.equal(evidence.sources.leadPortalAnalytics.validationDefinitionVersion, 'v1');
  assert.equal(evidence.sources.leadPortalAnalytics.publicationScope, 'PUBLISHED_RUN_ASSET_BUNDLE');
  await mkdir(output, { recursive: true });
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  const results = [];
  try {
    for (const [device, profile] of [['desktop', { viewport: { width: 1440, height: 1000 } }],
      ['iphone', devices['iPhone 15 Pro']], ['pixel', devices['Pixel 7']]]) {
      const cycle = JSON.parse(execFileSync('python3', ['-c', `
import json,sys,os,pathlib
sys.path.insert(0,'infra/testing/cycle-commercial')
from client import prepare,authorize,command,http,read
c=prepare(missing=False)
c=command(c,body=authorize(c))
http('/fixture/experiments/91001/publish',{})
http('/fixture/experiments/91001/stop',{})
e=json.loads((pathlib.Path(os.environ['LEAD_PORTAL_CYCLE_TEST_EVIDENCE_DIR'])/'lead-portal-measurement.json').read_text())
assert e['sources']['leadPortalAnalytics']['productVersion']==c['productVersion']
http('/fixture/experiments/91001/measurement',{'evidence':e})
print(json.dumps(read(c)))
`], { encoding: 'utf8' }));
      const context = await browser.newContext({ ...profile, timezoneId: 'UTC' });
      const page = await context.newPage(), errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await context.route('**/*', async route => {
        const target = new URL(route.request().url());
        if (target.origin !== base) return route.abort();
        if (!target.pathname.startsWith('/api/')) return route.continue();
        if (target.pathname === '/api/products')
          return route.fulfill({ json: [{ id: 91001, name: 'Kit local segregado', internalName: 'Capella · QA', automaticExecutionEnabled: true, automaticExecutionStatus: 'PLAY' }] });
        if (target.pathname === '/api/business-process-chains')
          return route.fulfill({ json: [{ id: 91002, name: 'Cadeia local', versionNumber: 1 }] });
        if (target.pathname.startsWith(prefix))
          return route.fulfill({ response: await route.fetch({ url: api + target.pathname + target.search }) });
        return route.fulfill({ json: [] });
      });
      await page.goto(`${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`);
      const form = page.getByRole('form', { name: 'Decisão do ciclo', exact: true });
      await form.locator('[name="operatorName"]').fill('QA SQL local segregado');
      await form.locator('[name="summary"]').fill('Publicação sintética encerrada com medição SQL');
      await form.locator('[name="evidenceReference"]').fill('internal://fixture/sql-measurement/91001');
      await form.getByRole('button', { name: 'Concluir etapa com evidência', exact: true }).click();
      const readCycle = async () => (await (await fetch(api + prefix + '/products/91001')).json()).find(c => c.id === cycle.id);
      await expect.poll(async () => (await readCycle()).stage).toBe('DECISION');
      const after = await readCycle();
      assert.equal(JSON.stringify(after).includes(evidence.sourceFingerprint), true);
      const state = await (await fetch(api + '/fixture/experiments/91001/state')).json();
      assert.equal(state.status, 'USER_STOPPED');
      assert.equal(state.runCount, 1);
      assert.deepEqual(errors, []);
      await page.screenshot({ path: `${output}/quartzo-${device}.png`, fullPage: true });
      results.push({ device, stage: after.stage, sourceFingerprint: evidence.sourceFingerprint, state, externalCalls: 0 });
      await context.close();
    }
  } finally { await browser.close(); }
  await writeFile(`${output}/quartzo-browser.json`, JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
})().catch(error => { console.error(error); process.exitCode = 1; });
