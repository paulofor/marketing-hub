#!/usr/bin/env node
// Homologa o cadastro existente e a biblioteca com contratos gerados pelo backend local.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium, devices } = require('playwright');
const base = process.env.HARNESS_UI_BASE || 'http://127.0.0.1:5511';
const artifact = process.env.HARNESS_BACKEND_FIXTURE;
assert(artifact, 'Defina HARNESS_BACKEND_FIXTURE com o contrato exportado pelo teste Java.');
const backend = JSON.parse(fs.readFileSync(artifact, 'utf8'));
const fixture = JSON.parse(fs.readFileSync(path.join(__dirname, '../backend/ads-service/src/test/resources/fixtures/harness-curadoria-agentes-v1.json'), 'utf8'));
const output = process.env.HARNESS_UI_OUTPUT || '/tmp/harness-curation-ui';
fs.mkdirSync(output, { recursive: true });

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN || '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  try {
    for (const [deviceName, settings] of [['desktop', { viewport: { width: 1440, height: 1000 } }], ['iPhone 15 Pro', devices['iPhone 15 Pro']], ['Pixel 7', devices['Pixel 7']]]) {
      const context = await browser.newContext(settings);
      const page = await context.newPage();
      const agents = fixture.map(a => ({ id: a.agentId, name: `Agente ${a.agentName}`, nickname: a.agentName, agentKey: a.agentKey, status: 'TEST', currentVersion: 1, executionMode: 'ON_DEMAND', themeId: 3, inputs: [{ name: 'Entrada anterior', type: 'CONTEXT', description: 'Preservar esta orientação.', orderIndex: 0 }], outputs: [], internalFunctions: [], description: 'Cadastro isolado de homologação.' }));
      let writes = 0;
      let failCatalog = false;
      await context.route('**/api/**', async route => {
        const req = route.request();
        const url = new URL(req.url());
        if (!url.pathname.startsWith('/api/')) return route.continue();
        const match = url.pathname.match(/^\/api\/agents\/(\d+)$/);
        let result = [];
        if (match) {
          const index = agents.findIndex(a => a.id === Number(match[1]));
          if (req.method() === 'PUT') {
            const payload = req.postDataJSON();
            assert.equal(payload.inputs[0].name, 'Entrada anterior');
            assert.equal(payload.description, agents[index].description);
            const expected = fixture.find(a => a.agentId === agents[index].id);
            assert.deepEqual(payload.inputs.slice(1).map(({ name, type, description }) => ({ name, type, description })), expected.inputs);
            agents[index] = { ...agents[index], ...payload, currentVersion: agents[index].currentVersion + 1 };
            writes++;
          }
          result = agents[index];
        } else if (url.pathname === '/api/agents') result = agents;
        else if (url.pathname === '/api/agent-themes') result = [{ id: 3, name: 'Homologação' }];
        else if (url.pathname === '/api/research-intelligence/v1/catalog') {
          if (failCatalog) return route.fulfill({ status: 503, contentType: 'application/json', body: '{"message":"Fonte indisponível em teste"}' });
          result = backend.catalog;
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(result) });
      });
      for (const agent of fixture) {
        await page.goto(`${base}/agents/${agent.agentId}/edit`);
        await page.locator('#agent-nickname').waitFor();
        const section = page.locator('.card').filter({ hasText: 'Informações que o agente deve receber' });
        for (const item of agent.inputs) {
          await section.getByRole('button', { name: 'Adicionar', exact: true }).click();
          const row = section.locator('.border.rounded.p-3').last();
          await row.locator('input').nth(0).fill(item.name);
          await row.locator('input').nth(1).fill(item.type);
          await row.locator('textarea').fill(item.description);
        }
        const saved = page.waitForResponse(r => r.url().endsWith(`/api/agents/${agent.agentId}`) && r.request().method() === 'PUT');
        await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click();
        assert.equal((await saved).status(), 200);
        await page.waitForURL('**/agents');
      }
      assert.equal(writes, 9);
      await page.goto(`${base}/audio-video-studio/research-library`);
      await page.getByLabel(/^Agente/).selectOption('landing-generator');
      await page.getByText('3 cartões encontrados · exibindo 3', { exact: true }).waitFor();
      assert.equal(await page.getByText('Direcionado a Dédalo', { exact: true }).count(), 3);
      const width = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, viewport: innerWidth }));
      assert(width.scroll <= width.viewport + 1, `${deviceName}: overflow ${JSON.stringify(width)}`);
      await page.screenshot({ path: path.join(output, `${deviceName.replaceAll(' ', '-')}.png`), fullPage: true });
      await page.getByRole('link', { name: 'Editar referências de Dédalo', exact: true }).click();
      await page.locator('#agent-nickname').waitFor();
      assert.equal(await page.locator('#agent-nickname').inputValue(), 'Dédalo');
      assert.equal(await page.locator('input').filter({ visible: true }).count() > 0, true);
      failCatalog = true;
      await page.reload();
      await page.goto(`${base}/audio-video-studio/research-library`);
      await page.getByText(/Não foi possível carregar a biblioteca agora/).waitFor({ timeout: 20000 });
      assert.equal(await page.getByText('Direcionado a Dédalo', { exact: true }).count(), 0);
      fs.writeFileSync(path.join(output, `${deviceName.replaceAll(' ', '-')}-cadastros.json`), JSON.stringify(agents, null, 2));
      console.log(`${deviceName}: 9 cadastros, 25 vínculos, fonte indisponível e navegação aprovados.`);
      await context.close();
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
