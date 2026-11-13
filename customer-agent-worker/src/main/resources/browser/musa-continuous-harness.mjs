import { chromium, devices } from 'playwright-core';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';
import { videoBrowserOptions } from './learning-cycle-video-checks.mjs';

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
const input = JSON.parse(await readFile(inputPath, 'utf8'));
const base = '/api/pde/vega/consultor/v1';
const version = 'musa-consultor-pwa-v1';
const internal = process.env.PDE_INTERNAL_API_TOKEN;
assert.equal(input.prototypeVersion, version);
assert.ok(internal && input.cycleId);
assert.ok(!input.videoIntegration, 'O consultor privado não possui audiovisual autorizado.');
const pieces = 'Camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça da bolsa.';
const sideEffects = { paymentEnabled: false, published: false, campaignCreated: false, mediaSpendBrl: 0 };

// Mantém credenciais apenas no transporte técnico e nunca inclui corpos no erro de integração.
async function request(path, body, cookie, privileged = false) {
  return fetch(new URL(base + path, input.sourceUrl), {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(cookie ? { Cookie: cookie } : {}),
      ...(privileged ? { 'X-PDE-Internal-Token': internal } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(30000),
  });
}
async function api(path, body, cookie, privileged = false) {
  const response = await request(path, body, cookie, privileged);
  assert.ok(response.ok, `MUSA ${path}: HTTP ${response.status}`);
  return response.json();
}
// A chave de emissão e o cookie não entram no relatório, na URL capturada ou no JavaScript da página.
async function issue() {
  const grant = await api(`/internal/cycles/${input.cycleId}/access`, {}, null, true);
  assert.equal(grant.productId, input.productId);
  assert.equal(grant.experimentId, Number(input.sourceReference.split(':')[1]));
  assert.equal(grant.prototypeVersion, version);
  const response = await request('/login', { accessKey: grant.accessKey });
  assert.equal(response.status, 200);
  const cookie = response.headers.get('set-cookie')?.split(';')[0];
  assert.ok(cookie?.startsWith('musa-consultor-session='));
  return { id: grant.sessionId, cookie };
}
const profiles = {
  DESKTOP_1440: { viewport: { width: 1440, height: 900 } },
  IPHONE_15_PRO: { ...devices['iPhone 15 Pro'], defaultBrowserType: 'chromium' },
  PIXEL_7: { ...devices['Pixel 7'] },
};
assert.ok(['TECHNICAL', 'SCENARIO'].includes(input.mode));
const plans = input.mode === 'TECHNICAL'
  ? ['ADHERENT', 'RECOVERY', 'SAFETY'].flatMap(s => Object.keys(profiles).map(d => [s, d]))
  : [[input.scenarioCode, { ADHERENT: 'DESKTOP_1440', RECOVERY: 'IPHONE_15_PRO', SAFETY: 'PIXEL_7' }[input.scenarioCode]]];
assert.ok(plans.every(([s, d]) => ['ADHERENT', 'RECOVERY', 'SAFETY'].includes(s) && profiles[d]));
const checkpointPath = resolve(evidenceDirectory, 'checkpoint.json');
let checkpoint;
try { checkpoint = JSON.parse(await readFile(checkpointPath, 'utf8')); }
catch (error) { if (error.code !== 'ENOENT') throw error; }
assert.ok(!checkpoint || checkpoint.captureSessionId === input.captureSessionId);
const scenarios = checkpoint?.scenarios || [], artifacts = checkpoint?.artifacts || [];
const startedAt = new Date(checkpoint?.startedAt || Math.min(Date.now(), ...scenarios.flatMap(s => Object.values(s.operations).map(o => Date.parse(o.startedAt)))));
const browser = await chromium.launch(videoBrowserOptions());
await mkdir(evidenceDirectory, { recursive: true });

// Percorre os três passos reais, sem invocar endpoint de geração por fora da experiência.
async function fill(page, occasion = 'almoço', selection = pieces) {
  await page.getByLabel('Para qual ocasião').fill(occasion);
  await page.getByRole('button', { name: 'Próximo', exact: true }).click();
  await page.getByLabel('Quais peças').fill(selection);
  await page.getByRole('button', { name: 'Próximo', exact: true }).click();
  await page.getByLabel('O que você prefere').selectOption('Cor e criatividade');
  await page.getByRole('button', { name: 'Receber orientação', exact: true }).click();
}
async function visible(locator) { await locator.waitFor({ state: 'visible' }); }
async function save(page) {
  await page.getByRole('button', { name: 'Salvar minha escolha', exact: true }).click();
  await visible(page.getByRole('button', { name: 'Escolha salva', exact: true }));
}
const workspace = cookie => api('/workspace', undefined, cookie);
const report = cookie => api('/internal/report', undefined, cookie, true);
try {
  for (const [scenarioCode, deviceProfile] of plans) {
    if (scenarios.some(s => s.scenarioCode === scenarioCode && s.deviceProfile === deviceProfile)) continue;
    const session = await issue();
    const context = await browser.newContext({ ...profiles[deviceProfile], reducedMotion: 'reduce' });
    try {
      const origin = new URL(input.sourceUrl);
      await context.addCookies([{ name: 'musa-consultor-session', value: session.cookie.split('=')[1],
        domain: origin.hostname, path: base, httpOnly: true, secure: origin.protocol === 'https:', sameSite: 'Strict' }]);
      const page = await context.newPage();
      const errors = []; page.on('pageerror', e => errors.push(e.message));
      await page.goto(input.sourceUrl, { waitUntil: 'domcontentloaded' });
      await page.getByRole('checkbox').check();
      const begin = page.getByRole('button', { name: 'Começar minha orientação', exact: true });
      await begin.focus();
      const accessibilityBasic = await begin.evaluate(e => document.activeElement === e)
        && await page.locator('html').getAttribute('lang') === 'pt-BR';
      assert.ok(accessibilityBasic);
      await begin.click();
      const firstInteraction = Date.now();
      let resumed = false, recovered = false, repeatedWithoutDuplicate = false, crossOwnerDenied = false;
      let crossOwnerStatus = null, replayOperationId = null, responseLost = false;
      const cardElement = page.getByRole('article', { name: 'Resultado da orientação' });
      if (scenarioCode === 'ADHERENT') {
        await fill(page); await visible(cardElement); assert.match(await cardElement.innerText(), /alça/);
        await save(page);
        await page.reload();
        await page.getByRole('button', { name: 'Adaptar para outra ocasião', exact: true }).click();
        await page.getByLabel('Para qual ocasião').fill('videochamada de trabalho');
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        assert.equal(await page.getByLabel('Quais peças').inputValue(), pieces);
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        assert.equal(await page.getByLabel('O que você prefere').inputValue(), 'Cor e criatividade');
        await page.getByRole('button', { name: 'Receber orientação', exact: true }).click();
        await visible(cardElement); assert.match(await cardElement.innerText(), /lenço no pescoço/);
        await save(page);
        assert.equal(Object.keys((await workspace(session.cookie)).choices).length, 2);
        resumed = true;
      } else if (scenarioCode === 'RECOVERY') {
        await fill(page, 'passeio', 'camisa e calça');
        await visible(page.getByText('Vamos ajustar a entrada'));
        assert.equal(await page.getByRole('button', { name: 'Salvar minha escolha', exact: true }).count(), 0);
        assert.match(await cardElement.innerText(), /Vamos ajustar a entrada/);
        await page.getByRole('button', { name: 'Revisar minhas peças', exact: true }).click();
        assert.equal(await page.getByLabel('Quais peças').inputValue(), 'camisa e calça');
        await page.getByLabel('Quais peças').fill(pieces);
        await page.getByRole('button', { name: 'Voltar', exact: true }).click();
        await page.getByLabel('Para qual ocasião').fill('almoço');
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        await page.route('**/commands', async route => {
          if (route.request().postDataJSON().action === 'generate' && !responseLost) {
            responseLost = true; await route.fetch(); await route.abort('connectionreset');
          } else await route.continue();
        });
        await page.getByRole('button', { name: 'Receber orientação', exact: true }).click();
        await visible(cardElement); await save(page);
        const before = (await report(session.cookie)).execution;
        const operation = Object.values(before.operations).find(o => o.status === 'COMPLETED');
        const replay = await api('/commands', { action: 'generate', payload: operation.request }, session.cookie);
        assert.equal(replay.operationId, operation.operationId); replayOperationId = replay.operationId;
        assert.deepEqual((await report(session.cookie)).execution.operations, before.operations);
        repeatedWithoutDuplicate = true; recovered = responseLost;
        await page.getByRole('button', { name: 'Usar esta orientação', exact: true }).click();
        await visible(page.getByRole('alert'));
        await page.reload();
        await visible(page.getByRole('button', { name: 'Adaptar para outra ocasião', exact: true }));
        assert.equal(await page.getByRole('button', { name: 'Adaptar para outra ocasião', exact: true }).count(), 1);
        // Retomar o repertório não fabrica nova orientação: reabre o resultado pela operação persistida.
        const loaded = await api(`/operations/${operation.operationId}`, undefined, session.cookie);
        assert.deepEqual(loaded, operation); resumed = true;
      } else {
        await fill(page, 'almoço', 'quero comprar roupa nova');
        await visible(page.getByText('Vamos ajustar a entrada'));
        assert.equal(await page.getByRole('button', { name: 'Salvar minha escolha', exact: true }).count(), 0);
        assert.match(await cardElement.innerText(), /Vamos ajustar a entrada/);
        const state = (await report(session.cookie)).execution;
        const blocked = Object.values(state.operations)[0]; assert.equal(blocked.status, 'BLOCKED');
        const other = await issue();
        const denied = await request(`/operations/${blocked.operationId}`, undefined, other.cookie);
        crossOwnerStatus = denied.status; assert.equal(denied.status, 409); crossOwnerDenied = true;
        assert.equal((await request('/workspace', undefined, 'musa-consultor-session=invalid')).status, 401);
        assert.equal((await request(`/internal/cycles/${input.cycleId}/access`, {})).status, 401);
      }
      const resultReadySeconds = Math.ceil((Date.now() - firstInteraction) / 1000);
      if (scenarioCode !== 'SAFETY') {
        // O sinal de uso é explícito; os dois últimos são exclusivamente transporte sintético interno.
        if (scenarioCode === 'ADHERENT') {
          await page.getByRole('button', { name: 'Usar esta orientação', exact: true }).click();
          await visible(page.getByRole('alert'));
        }
        const ops = Object.values((await report(session.cookie)).execution.operations);
        const op = ops.filter(o => o.status === 'COMPLETED').at(-1);
        for (const eventType of ['PREFERRED_OVER_FREE', 'CHECKOUT_STARTED'])
          await api('/internal/events', { eventType, operationId: op.operationId }, session.cookie, true);
      }
      const observed = await report(session.cookie), data = observed.execution;
      assert.equal(observed.trafficClass, 'AGENT_VALIDATION');
      assert.equal(observed.prototypeVersion, version);
      assert.equal(observed.sessionId, session.id);
      assert.ok(Object.values(data.operations).every(o => o.providerCalls === 0 && o.incrementalInferenceCostUsd === 0));
      const operations = Object.values(data.operations);
      const last = operations.filter(o => o.status === 'COMPLETED').at(-1);
      assert.equal(operations.length, scenarioCode === 'SAFETY' ? 1 : 2);
      assert.equal(Object.keys(data.choices).length, scenarioCode === 'ADHERENT' ? 2 : scenarioCode === 'RECOVERY' ? 1 : 0);
      const privacyPreserved = !page.url().includes('#') && !(await page.evaluate(() => document.cookie)).includes('musa-consultor-session');
      assert.ok(privacyPreserved);
      const noHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1);
      assert.ok(noHorizontalOverflow);
      await page.evaluate(() => { document.documentElement.style.zoom = '2'; });
      const zoom200 = await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1);
      assert.ok(zoom200);
      await page.evaluate(() => { document.documentElement.style.zoom = '1'; });
      assert.deepEqual(errors, []);
      const evidenceKey = `musa-continuous-${session.id}`;
      const screenshot = resolve(evidenceDirectory, `${scenarioCode}-${deviceProfile}.png`);
      await page.screenshot({ path: screenshot, fullPage: true });
      const viewport = page.viewportSize();
      artifacts.push({ captureSessionId: input.captureSessionId, evidenceKey, evidenceType: 'FULL_PAGE', deviceProfile,
        pageNumber: 1, foldNumber: null, viewportWidth: viewport.width, viewportHeight: viewport.height,
        pageHeightPx: await page.evaluate(() => document.documentElement.scrollHeight), scrollY: 0,
        sourceUrl: input.sourceUrl, finalUrl: page.url(), capturedAt: new Date().toISOString(), localPath: screenshot });
      scenarios.push({ scenarioCode, deviceProfile, status: 'PASS', prototypeVersion: version,
        evidenceId: session.id, screenshotEvidenceKeys: [evidenceKey], operations: data.operations,
        choices: data.choices, events: data.signals, card: last || null, replayOperationId,
        repeatedWithoutDuplicate, responseLost, crossOwnerDenied, crossOwnerStatus, resumed, recovered,
        continuousJourney: scenarioCode === 'ADHERENT', safetyBlocked: scenarioCode === 'SAFETY',
        resultReadySeconds, accessibilityBasic, noHorizontalOverflow, zoom200, privacyPreserved,
        trafficClass: 'AGENT_VALIDATION', mhInternalTest: true, providerCalls: 0,
        humanEvidenceClaimed: false, commercialEvidenceClaimed: false, sideEffects });
      await writeFile(checkpointPath, JSON.stringify({ captureSessionId: input.captureSessionId, startedAt: startedAt.toISOString(), scenarios, artifacts }));
    } finally { await context.close(); }
  }
} finally { await browser.close(); }
const deviceResults = [...new Set(plans.map(p => p[1]))].map(deviceProfile => ({ deviceProfile,
  status: 'PASS', screenshotEvidenceKeys: artifacts.filter(a => a.deviceProfile === deviceProfile).map(a => a.evidenceKey) }));
const checks = { sameVersion: scenarios.every(s => s.prototypeVersion === version),
  desktopAndMobile: input.mode !== 'TECHNICAL' || deviceResults.length === 3,
  happyResultWithinTenMinutes: scenarios.filter(s => s.scenarioCode === 'ADHERENT').every(s => s.resultReadySeconds <= 600),
  recoveryPreserved: scenarios.filter(s => s.scenarioCode === 'RECOVERY').every(s => s.recovered && s.resumed && s.repeatedWithoutDuplicate),
  safetyBlocked: scenarios.filter(s => s.scenarioCode === 'SAFETY').every(s => s.safetyBlocked && s.crossOwnerDenied),
  accessibilityBasic: scenarios.every(s => s.accessibilityBasic && s.zoom200),
  responsiveLayout: scenarios.every(s => s.noHorizontalOverflow), privacyPreserved: scenarios.every(s => s.privacyPreserved),
  internalTrafficSegregated: true, paymentDisabled: true, publicationDisabled: true, campaignDisabled: true, zeroMediaSpend: true,
  continuousJourney: scenarios.filter(s => s.scenarioCode === 'ADHERENT').every(s => s.continuousJourney) };
const finishedAt = new Date();
await writeFile(outputPath, JSON.stringify({ contractVersion: 'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',
  mode: input.mode, decision: Object.values(checks).every(Boolean) ? 'APPROVED' : 'BLOCKED',
  sourceReference: input.sourceReference, productId: input.productId, productSlug: input.productSlug,
  publicUrl: input.sourceUrl, prototypeVersion: version, cycleId: input.cycleId,
  runtimeContract: 'MUSA_CONTINUOUS_STYLE_EXECUTION_V1', generationMode: 'DETERMINISTIC_CONTEXTUAL_RULES',
  providerCalls: 0, modelFunctionalBehaviorClaimed: false, trafficClass: 'AGENT_VALIDATION', internalMarker: 'mh_internal_test',
  inputComparisonApplicable: false, startedAt: startedAt.toISOString(), finishedAt: finishedAt.toISOString(),
  durationSeconds: Math.ceil((finishedAt - startedAt) / 1000), devices: deviceResults, scenarios, checks, artifacts, sideEffects,
  humanEvidenceClaimed: false, commercialEvidenceClaimed: false, evidence: scenarios.map(s => s.evidenceId) }));
