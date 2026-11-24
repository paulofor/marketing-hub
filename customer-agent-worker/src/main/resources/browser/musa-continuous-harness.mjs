import { chromium, devices } from 'playwright-core';
import { readFile, mkdir, writeFile, rename, chmod } from 'node:fs/promises';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { videoBrowserOptions } from './learning-cycle-video-checks.mjs';

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
const input = JSON.parse(await readFile(inputPath, 'utf8'));
const base = '/api/pde/vega/consultor/v1';
const version = 'musa-consultor-pwa-v1';
const checkpointScope = { productId: input.productId, cycleId: input.cycleId,
  sourceReference: input.sourceReference, prototypeVersion: input.prototypeVersion,
  sourceUrl: input.sourceUrl, mode: input.mode, scenarioCode: input.scenarioCode || '',
  captureSessionId: input.captureSessionId };
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
// A chave de recuperação fica apenas em checkpoint privado, gravado antes do HTTP.
const openedSessions = [];
const accessPath = resolve(evidenceDirectory, 'access-private.json');
let accesses = {};
try {
  const saved = JSON.parse(await readFile(accessPath, 'utf8'));
  assert.deepEqual(saved.scope, checkpointScope, 'Checkpoint privado de outro contexto recusado.');
  accesses = saved.slots;
}
catch (error) { if (error.code !== 'ENOENT') throw error; }
await mkdir(evidenceDirectory, { recursive: true, mode: 0o700 });
async function persistAccesses() {
  const temporary = accessPath + '.tmp';
  await writeFile(temporary, JSON.stringify({ scope: checkpointScope, slots: accesses }), { mode: 0o600 });
  await chmod(temporary, 0o600);
  await rename(temporary, accessPath);
}
async function issue(scenarioCode, deviceProfile, role = 'PRIMARY') {
  const slot = `${scenarioCode}:${deviceProfile}:${role}`;
  if (!accesses[slot]) {
    accesses[slot] = { recoveryKey: randomBytes(32).toString('hex') };
    await persistAccesses();
  }
  const grant = await api(`/internal/cycles/${input.cycleId}/harness-access`, {
    productId: input.productId, experimentId: Number(input.sourceReference.split(':')[1]),
    prototypeVersion: version, sourceReference: input.sourceReference,
    attemptId: input.captureSessionId, scenarioCode, deviceProfile, role,
    recoveryKey: accesses[slot].recoveryKey,
  }, null, true);
  assert.equal(grant.productId, input.productId);
  assert.equal(grant.experimentId, Number(input.sourceReference.split(':')[1]));
  assert.equal(grant.prototypeVersion, version);
  const response = await request('/login', { accessKey: accesses[slot].recoveryKey });
  assert.equal(response.status, 200);
  const cookie = response.headers.get('set-cookie')?.split(';')[0];
  assert.ok(cookie?.startsWith('musa-consultor-session='));
  const session = { id: grant.sessionId, cookie, finished: grant.finished };
  openedSessions.push(session);
  return session;
}
async function finish(session) {
  await api('/internal/harness-access/finish', {}, session.cookie, true);
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
if (checkpoint) assert.deepEqual(checkpoint.scope, checkpointScope, 'Checkpoint de outro contexto recusado.');
const scenarios = checkpoint?.scenarios || [], artifacts = checkpoint?.artifacts || [];
const startedAt = new Date(checkpoint?.startedAt || Math.min(Date.now(), ...scenarios.flatMap(s => Object.values(s.operations).map(o => Date.parse(o.startedAt)))));
const browser = await chromium.launch(videoBrowserOptions());
await mkdir(evidenceDirectory, { recursive: true });
if (!checkpoint) {
  await writeFile(checkpointPath + '.tmp', JSON.stringify({ scope: checkpointScope, captureSessionId: input.captureSessionId, startedAt: startedAt.toISOString(), scenarios, artifacts }));
  await rename(checkpointPath + '.tmp', checkpointPath);
}

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
    if (scenarios.some(s => s.scenarioCode === scenarioCode && s.deviceProfile === deviceProfile)) {
      await finish(await issue(scenarioCode, deviceProfile));
      continue;
    }
    const session = await issue(scenarioCode, deviceProfile);
    const context = await browser.newContext({ ...profiles[deviceProfile], reducedMotion: 'reduce' });
    try {
      const origin = new URL(input.sourceUrl);
      await context.addCookies([{ name: 'musa-consultor-session', value: session.cookie.split('=')[1],
        domain: origin.hostname, path: base, httpOnly: true, secure: origin.protocol === 'https:', sameSite: 'Strict' }]);
      const page = await context.newPage();
      const errors = []; page.on('pageerror', e => errors.push(e.message));
      await page.goto(input.sourceUrl, { waitUntil: 'domcontentloaded' });
      const prior = (await report(session.cookie)).execution;
      const priorOps = Object.values(prior.operations);
      if (!prior.consented) await page.getByRole('checkbox').check();
      const begin = page.getByRole('button', { name: 'Começar minha orientação', exact: true });
      if (!prior.consented) await begin.focus();
      const accessibilityBasic = (prior.consented || await begin.evaluate(e => document.activeElement === e))
        && await page.locator('html').getAttribute('lang') === 'pt-BR';
      assert.ok(accessibilityBasic);
      if (!prior.consented) await begin.click();
      let resumed = false, recovered = false, repeatedWithoutDuplicate = false, crossOwnerDenied = false;
      let crossOwnerStatus = null, replayOperationId = null, responseLost = false;
      const cardElement = page.getByRole('article', { name: 'Resultado da orientação' });
      if (scenarioCode === 'ADHERENT') {
        if (priorOps.length === 0) await fill(page);
        if (priorOps.length < 2) {
        if (Object.keys(prior.choices).length === 0) {
          await visible(cardElement); assert.match(await cardElement.innerText(), /alça/);
          await save(page);
        }
        await page.reload();
        await page.getByRole('button', { name: 'Adaptar para outra ocasião', exact: true }).click();
        await page.getByLabel('Para qual ocasião').fill('videochamada de trabalho');
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        assert.equal(await page.getByLabel('Quais peças').inputValue(), pieces);
        await page.getByRole('button', { name: 'Próximo', exact: true }).click();
        assert.equal(await page.getByLabel('O que você prefere').inputValue(), 'Cor e criatividade');
        await page.getByRole('button', { name: 'Receber orientação', exact: true }).click();
        }
        await visible(cardElement); assert.match(await cardElement.innerText(), /lenço no pescoço/);
        if (Object.keys((await workspace(session.cookie)).choices).length < 2) await save(page);
        assert.equal(Object.keys((await workspace(session.cookie)).choices).length, 2);
        resumed = true;
      } else if (scenarioCode === 'RECOVERY') {
        if (priorOps.length === 0) await fill(page, 'passeio', 'camisa e calça');
        if (!priorOps.some(o => o.status === 'COMPLETED')) {
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
            responseLost = true; await route.fetch();
            accesses[`${scenarioCode}:${deviceProfile}:PRIMARY`].responseLost = true; await persistAccesses();
            await route.abort('connectionreset');
          } else await route.continue();
        });
        await page.getByRole('button', { name: 'Receber orientação', exact: true }).click();
        await visible(cardElement);
        } else responseLost = accesses[`${scenarioCode}:${deviceProfile}:PRIMARY`].responseLost === true;
        if (Object.keys((await workspace(session.cookie)).choices).length === 0) await save(page);
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
        if (priorOps.length === 0) await fill(page, 'almoço', 'quero comprar roupa nova');
        await visible(page.getByText('Vamos ajustar a entrada'));
        assert.equal(await page.getByRole('button', { name: 'Salvar minha escolha', exact: true }).count(), 0);
        assert.match(await cardElement.innerText(), /Vamos ajustar a entrada/);
        const state = (await report(session.cookie)).execution;
        const blocked = Object.values(state.operations)[0]; assert.equal(blocked.status, 'BLOCKED');
        const other = await issue(scenarioCode, deviceProfile, 'ISOLATION');
        const denied = await request(`/operations/${blocked.operationId}`, undefined, other.cookie);
        crossOwnerStatus = denied.status; assert.equal(denied.status, 409); crossOwnerDenied = true;
        await finish(other);
        assert.equal((await request('/workspace', undefined, 'musa-consultor-session=invalid')).status, 401);
        assert.equal((await request(`/internal/cycles/${input.cycleId}/access`, {})).status, 401);
      }
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
      const timed = operations.slice().sort((a, b) => Date.parse(a.finishedAt) - Date.parse(b.finishedAt));
      const firstResult = timed.find(o => o.status === 'COMPLETED') || timed[0];
      assert.ok(firstResult && Number.isFinite(Date.parse(firstResult.startedAt)) && Number.isFinite(Date.parse(firstResult.finishedAt)));
      const resultReadySeconds = Math.ceil((Date.parse(firstResult.finishedAt) - Date.parse(firstResult.startedAt)) / 1000);
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
      const evidenceKey = `musa-continuous-${session.id}-${input.captureSessionId}`;
      const screenshot = resolve(evidenceDirectory, `${scenarioCode}-${deviceProfile}.png`);
      await page.screenshot({ path: screenshot, fullPage: true, scale: 'css' });
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
      await writeFile(checkpointPath + '.tmp', JSON.stringify({ scope: checkpointScope, captureSessionId: input.captureSessionId, startedAt: startedAt.toISOString(), scenarios, artifacts }));
      await rename(checkpointPath + '.tmp', checkpointPath);
      await finish(session);
    } finally { await context.close(); }
  }
} catch (error) {
  // Falha terminal registrada conserva estado; queda do processo recupera a tentativa em andamento.
  for (const session of openedSessions) await finish(session).catch(() => {});
  throw error;
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
