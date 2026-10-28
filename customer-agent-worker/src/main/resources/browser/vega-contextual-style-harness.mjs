import { chromium, devices } from 'playwright-core';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';
import { videoBrowserOptions } from './learning-cycle-video-checks.mjs';

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
const input = JSON.parse(await readFile(inputPath, 'utf8'));
const base = '/api/pde/vega/style/v1';
const version = input.prototypeVersion;
const optionalReflection = version === 'musa-pde-entry-v17-continuidade-clara';
assert.ok(['musa-pde-entry-v16-modelagem-internacional', 'musa-pde-entry-v17-continuidade-clara'].includes(version));
const internal = process.env.PDE_INTERNAL_API_TOKEN;
assert.ok(internal && input.cycleId);
assert.equal(input.prototypeVersion, version);
assert.ok(!input.videoIntegration, 'A versão contextual não possui integração audiovisual aprovada.');

// Erros de transporte não incluem corpo, credencial ou objeto de requisição no diagnóstico.
async function api(path, body, token) {
  const response = await fetch(new URL(base + path, input.sourceUrl), {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', 'X-PDE-Internal-Token': internal,
      ...(token ? { 'X-Vega-Style-Session': token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(30000),
  });
  assert.ok(response.ok, `Vega contextual ${path}: HTTP ${response.status}`);
  const text = await response.text(); return text ? JSON.parse(text) : null;
}

const profiles = {
  DESKTOP_1440: { viewport: { width: 1440, height: 900 } },
  IPHONE_15_PRO: { ...devices['iPhone 15 Pro'], defaultBrowserType: 'chromium' },
  PIXEL_7: { ...devices['Pixel 7'] },
};
const allPlans = ['ADHERENT', 'RECOVERY', 'SAFETY'].flatMap(s => Object.keys(profiles).map(d => [s, d]));
const plans = input.mode === 'TECHNICAL' ? allPlans : [[input.scenarioCode,
  input.scenarioCode === 'SAFETY' ? 'PIXEL_7' : input.scenarioCode === 'RECOVERY' ? 'IPHONE_15_PRO' : 'DESKTOP_1440']];
const sideEffects = { paymentEnabled: false, published: false, campaignCreated: false, mediaSpendBrl: 0 };
const startedAt = new Date();
const scenarios = [], artifacts = [];
const browser = await chromium.launch(videoBrowserOptions());
await mkdir(evidenceDirectory, { recursive: true });

// Preenche somente os três campos que já existiam na jornada anterior.
async function fill(page, scenarioCode) {
  await page.getByLabel('Onde você vai usar?').fill('almoço');
  await page.getByLabel('Quais roupas você já tem em mente?').fill(scenarioCode === 'SAFETY'
    ? 'quero comprar uma roupa nova'
    : 'camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça');
  await page.getByLabel('O que você quer sentir').selectOption('Cor e criatividade');
}

try {
  for (const [scenarioCode, deviceProfile] of plans) {
    const session = await api('/internal/sessions', { cycleId: input.cycleId, prototypeVersion: version });
    assert.equal(session.productId, input.productId);
    assert.equal(session.experimentId, Number(input.sourceReference.split(':')[1]));
    assert.equal(session.prototypeVersion, version);
    const context = await browser.newContext(profiles[deviceProfile]);
    const origin = new URL(input.sourceUrl);
    await context.addCookies([{ name: 'vega-style-session', value: session.sessionToken,
      domain: origin.hostname, path: '/api/pde/vega/style/v1', httpOnly: true,
      secure: origin.protocol === 'https:', sameSite: 'Strict' }]);
    const page = await context.newPage();
    const failures = []; page.on('pageerror', e => failures.push(e.message));
    await page.goto(input.sourceUrl, { waitUntil: 'domcontentloaded' });
    const begin = page.getByRole('button', { name: 'Experimentar com minhas roupas', exact: true });
    await begin.waitFor(); await begin.focus();
    const accessibilityBasic = await begin.evaluate(e => document.activeElement === e)
      && await page.locator('html').getAttribute('lang') === 'pt-BR';
    assert.ok(accessibilityBasic);
    assert.equal(Object.keys((await api('/internal/report', undefined, session.sessionToken)).events).length, 0);
    const firstInteractionAt = Date.now();
    await begin.click();
    await fill(page, scenarioCode);
    let recovered = false;
    if (scenarioCode === 'RECOVERY') {
      // O servidor conclui; perde-se só a resposta. A repetição precisa reutilizar a mesma operação.
      await page.route('**/access/missions/estilo-dia-1/ai-guidance', async route => {
        await route.fetch(); await route.abort('failed');
      }, { times: 1 });
      await page.getByRole('button', { name: 'Ver minha orientação', exact: true }).click();
      await page.getByRole('alert').waitFor();
      assert.match(await page.getByLabel('Quais roupas você já tem em mente?').inputValue(), /lenço/);
      assert.equal(await page.getByRole('article', { name: 'Sua orientação pronta' }).count(), 0);
      recovered = true;
    }
    const response = page.waitForResponse(r => r.url().endsWith('/access/missions/estilo-dia-1/ai-guidance') && r.request().method() === 'POST' && r.ok());
    await page.getByRole('button', { name: 'Ver minha orientação', exact: true }).click();
    const result = await (await response).json();
    const resultReadySeconds = Math.ceil((Date.now() - firstInteractionAt) / 1000);
    let resumed = false, journeyComplete = false;
    const screenshot = resolve(evidenceDirectory, `${scenarioCode}-${deviceProfile}.png`);
    if (scenarioCode === 'SAFETY') {
      await page.getByRole('alert').waitFor();
      assert.equal(result.status, 'BLOCKED');
      assert.equal(await page.getByRole('article', { name: 'Sua orientação pronta' }).count(), 0);
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.getByRole('alert').waitFor();
      assert.equal(await page.getByRole('article', { name: 'Sua orientação pronta' }).count(), 0);
    } else {
      const card = page.getByRole('article', { name: 'Sua orientação pronta' });
      await card.waitFor(); assert.match(await card.innerText(), /alça da bolsa/);
      await page.getByRole('button', { name: 'Entendi como aplicar', exact: true }).click();
      await page.getByRole('status').waitFor();
      await page.getByRole('button', { name: 'Apliquei este ajuste', exact: true }).click();
      await page.getByRole('button', { name: 'Conhecer a continuidade', exact: true }).click();
      await page.getByText(/Esta prévia não realiza cobranças/).waitFor();
      await api('/internal/events', { eventType: 'PREFERRED_OVER_FREE', requestId: result.requestId }, session.sessionToken);
      await page.reload({ waitUntil: 'domcontentloaded' });
      await card.waitFor(); assert.match(await card.innerText(), /alça da bolsa/); resumed = true;
      let report = await api('/internal/report', undefined, session.sessionToken);
      assert.equal(Object.keys(report.operations).length, 1);
      assert.equal(report.operations[result.requestId].response.requestId, result.requestId);
      assert.equal(report.operations[result.requestId].decision.ruleId, 'SCARF_MEAL_STRAP');
      assert.equal((await api('/access/workspace', undefined, session.sessionToken)).subscriptionStatus, 'TRIAL');
      await page.screenshot({ path: screenshot, fullPage: true });
      if (scenarioCode === 'ADHERENT') {
        // O direito de teste é concedido pelo runner, separado do simulador de compra.
        await api('/internal/fixtures/full-access', {}, session.sessionToken);
        await page.reload({ waitUntil: 'domcontentloaded' }); await card.waitFor();
        for (let day = 1; day <= 7; day++) {
          if (day > 1) {
            await page.getByRole('button', { name: 'Ver minha orientação', exact: true }).click(); await card.waitFor();
          }
          // A candidata precisa funcionar também sem escrita; preencher sempre ocultava esse gate.
          const reflection = optionalReflection && day % 2 === 1 ? '' : `Registro sintético do dia ${day}; quero repetir esta escolha.`;
          await page.getByLabel(optionalReflection ? 'Quer anotar algo para depois? (opcional)' : 'O que funcionou para você?').fill(reflection);
          const complete = page.waitForResponse(r => r.url().endsWith(`/estilo-dia-${day}/complete`) && r.ok());
          await page.getByRole('button', { name: 'Salvar e concluir este dia', exact: true }).click(); await complete;
          await page.getByRole('status').filter({ hasText: 'ficaram salvos' }).waitFor();
        }
        assert.equal((await api('/access/workspace', undefined, session.sessionToken)).progressPercent, 100);
        for (const name of ['mapa-do-meu-estilo', 'antes-de-sair', 'minhas-formulas']) {
          const material = await fetch(new URL(`${base}/materials/${name}.txt`, input.sourceUrl), { headers: { 'X-Vega-Style-Session': session.sessionToken } });
          assert.equal(material.status, 200);
          const text = await material.text();
          assert.match(text, optionalReflection ? /Registro sintético do dia 6/ : /Registro sintético do dia 7/);
          if (optionalReflection) {
            assert.doesNotMatch(text, /Registro sintético do dia [1357]/);
            assert.match(text, /Minha orientação:/);
            assert.match(text, /alça da bolsa/);
          }
        }
        await page.getByRole('button', { name: /Dia 1$/, exact: false }).first().click();
        await card.waitFor();
        assert.match(await card.innerText(), /alça da bolsa/);
        const journeyReport = await api('/internal/report', undefined, session.sessionToken);
        assert.equal(Object.keys(journeyReport.operations).length, 7);
        if (optionalReflection) {
          assert.equal((await api('/access/workspace', undefined, session.sessionToken)).reflectionRequiredForCompletion, false);
          assert.equal(journeyReport.missions['estilo-dia-1'].answers.reflection, '');
          assert.equal(journeyReport.missions['estilo-dia-7'].answers.reflection, '');
        }
        journeyComplete = true;
        // A primeira orientação já foi capturada; a prova adicional dos sete dias permanece no relatório.
      }
    }
    const report = await api('/internal/report', undefined, session.sessionToken);
    assert.deepEqual(Object.keys(report.events).sort(), (scenarioCode === 'SAFETY' ? ['EXPERIENCE_STARTED']
      : ['EXPERIENCE_STARTED', 'VALUE_MOMENT', 'READY_RESULT_USED', 'PREFERRED_OVER_FREE', 'CHECKOUT_STARTED']).sort());
    assert.deepEqual(report.sideEffects, sideEffects);
    assert.equal(report.trafficClass, 'AGENT_VALIDATION');
    assert.ok(Object.values(report.operations).every(op => op.providerCalls === 0));
    const privacyPreserved = !page.url().includes(session.sessionToken)
      && !(await page.evaluate(() => document.cookie)).includes('vega-style-session');
    assert.ok(privacyPreserved);
    const noHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1);
    assert.ok(noHorizontalOverflow); assert.deepEqual(failures, []);
    const evidenceKey = `vega-style-${session.id}`;
    if (scenarioCode === 'SAFETY') await page.screenshot({ path: screenshot, fullPage: true });
    const viewport = page.viewportSize();
    artifacts.push({ captureSessionId: input.captureSessionId, evidenceKey, evidenceType: 'FULL_PAGE', deviceProfile,
      pageNumber: 1, foldNumber: null, viewportWidth: viewport.width, viewportHeight: viewport.height,
      pageHeightPx: await page.evaluate(() => document.documentElement.scrollHeight), scrollY: 0,
      sourceUrl: input.sourceUrl, finalUrl: page.url(), capturedAt: new Date().toISOString(), localPath: screenshot });
    scenarios.push({ scenarioCode, deviceProfile, status: 'PASS', prototypeVersion: version,
      evidenceId: session.id, screenshotEvidenceKeys: [evidenceKey], input: report.operations[result.requestId].input,
      card: result.status === 'COMPLETED' ? result : null, operations: report.operations, events: report.events,
      trafficClass: 'AGENT_VALIDATION', mhInternalTest: true, providerCalls: Object.values(report.operations).reduce((sum, operation) => sum + operation.providerCalls, 0), resultReadySeconds, resumed, recovered, journeyComplete,
      safetyBlocked: scenarioCode === 'SAFETY', accessibilityBasic, noHorizontalOverflow, privacyPreserved,
      humanEvidenceClaimed: false, commercialEvidenceClaimed: false, sideEffects });
    await context.close();
  }
} finally { await browser.close(); }

const deviceResults = [...new Set(plans.map(p => p[1]))].map(deviceProfile => ({ deviceProfile,
  viewportWidth: profiles[deviceProfile].viewport.width, viewportHeight: profiles[deviceProfile].viewport.height,
  status: 'PASS', screenshotEvidenceKeys: artifacts.filter(a => a.deviceProfile === deviceProfile).map(a => a.evidenceKey) }));
const checks = { sameVersion: scenarios.every(s => s.prototypeVersion === version),
  desktopAndMobile: input.mode !== 'TECHNICAL' || deviceResults.length === 3,
  happyResultWithinTenMinutes: scenarios.filter(s => s.scenarioCode === 'ADHERENT').every(s => s.resultReadySeconds <= 600),
  recoveryPreserved: input.mode !== 'TECHNICAL' || scenarios.some(s => s.recovered && s.resumed),
  safetyBlocked: input.mode !== 'TECHNICAL' || scenarios.some(s => s.safetyBlocked),
  accessibilityBasic: scenarios.every(s => s.accessibilityBasic), responsiveLayout: scenarios.every(s => s.noHorizontalOverflow),
  privacyPreserved: scenarios.every(s => s.privacyPreserved), internalTrafficSegregated: true,
  paymentDisabled: true, publicationDisabled: true, campaignDisabled: true, zeroMediaSpend: true,
  sevenDayJourney: scenarios.filter(s => s.scenarioCode === 'ADHERENT').every(s => s.journeyComplete) };
const finishedAt = new Date();
await writeFile(outputPath, JSON.stringify({ contractVersion: 'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',
  mode: input.mode, decision: Object.values(checks).every(Boolean) ? 'APPROVED' : 'BLOCKED',
  sourceReference: input.sourceReference, productId: input.productId, productSlug: input.productSlug,
  publicUrl: input.sourceUrl, prototypeVersion: version, cycleId: input.cycleId,
  runtimeContract: 'PDE_CONTEXTUAL_STYLE_EXECUTION_V1', generationMode: 'DETERMINISTIC_CONTEXTUAL_RULES', providerCalls: 0, modelFunctionalBehaviorClaimed: false,
  trafficClass: 'AGENT_VALIDATION', internalMarker: 'mh_internal_test', inputComparisonApplicable: false,
  startedAt: startedAt.toISOString(), finishedAt: finishedAt.toISOString(), durationSeconds: Math.ceil((finishedAt - startedAt) / 1000),
  devices: deviceResults, scenarios, checks, artifacts, sideEffects,
  humanEvidenceClaimed: false, commercialEvidenceClaimed: false, evidence: scenarios.map(s => s.evidenceId) }));
