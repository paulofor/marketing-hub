import test from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { checkCustomerSurface, validateCustomerSurface } from '../../main/resources/browser/customer-surface-contract.mjs';
import { videoBrowserOptions } from '../../main/resources/browser/learning-cycle-video-checks.mjs';
const require = createRequire(import.meta.url);
let library;
try { library = require('playwright-core'); } catch { library = require('playwright'); }

test('recusa os dados históricos de Capella e outras identidades, mesmo em detalhes fechados', async () => {
  const browser = await library.chromium.launch(videoBrowserOptions());
  try {
    const page = await browser.newPage();
    for (const content of [
      '<details><summary>Dados desta prova</summary><pre>{"productId":7,"cycleId":14,"experimentId":107}</pre></details>',
      '<details><summary>Informações</summary><p>AGENT_VALIDATION</p></details>',
      '<details><summary>Mais</summary><pre>{"sessionId":"other-product","providerCalls":0}</pre></details>',
      '<div hidden id="audit">Dados internos</div>',
      '<details id="test-controls"><summary>Mais</summary><button id="checkout">Conferir</button></details>',
    ]) {
      await page.setContent('<main><h1>Seu kit</h1>' + content + '</main>');
      assert.equal(await page.locator('details[open]').count(), 0);
      await assert.rejects(() => checkCustomerSurface(page), /dados ou controles internos/);
    }
    await page.setContent('<main><p>Demonstração com dados de exemplo · sem cobrança</p><details><summary>Calendário</summary><p>Escolha uma arte para hoje.</p></details></main>');
    assert.equal((await checkCustomerSurface(page)).internalInformationAbsent, true);
  } finally { await browser.close(); }
});

test('a interface real usa apresentação segura, mantém demonstração honesta e não exibe o erro bruto', async () => {
  let status = 'INPUT', fail = false, malformed = false;
  const session = () => ({ id: 'another-session', productId: 9918, cycleId: 2233, experimentId: 4411,
    prototypeVersion: 'another-private-v9', productName: 'Kit de barbearia', status,
    reason: 'SQLException debugInfo fixture sessionId=secret-technical-detail', events: [{ code: 'VALUE_MOMENT' }],
    firstApplication: { post: 'post-01.png', story: 'story-01.png', caption: 'Escolha sua divulgação.', message: 'Consulte os horários.',
      calendar: 'Dia 1 — Apresente seu serviço | posts/post-01.png | stories/story-01.png | legendas/legenda-01.txt' },
    manifest: { files: [{ name: 'posts/post-01.png', bytes: 512 }, { name: 'stories/story-01.png', bytes: 1024 }, { name: 'legendas/legenda-01.txt', bytes: 30 }] },
    presentation: { title: status === 'FAILED' ? 'A preparação foi interrompida' : 'Prepare seu kit',
      introduction: 'Confira sua divulgação.', statusMessage: status === 'FAILED' ? 'Seus dados estão guardados para retomar com segurança.' : '',
      nextActionLabel: '', nextStep: '', nextActionPath: '' } });
  const server = createServer(async (req, res) => {
    if (req.url.includes('/assets/')) {
      res.setHeader('Content-Type', 'image/svg+xml');
      res.end('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="purple"/></svg>'); return;
    }
    if (req.url.endsWith('/session')) {
      res.setHeader('Content-Type', 'application/json');
      if (malformed) { res.end('{"sessionId":"raw-technical-receipt", malformed'); return; }
      res.end(JSON.stringify(session())); return;
    }
    if (req.url.endsWith('/input')) {
      res.writeHead(fail ? 503 : 200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(fail ? { detail: 'SQLState fixture sessionId=raw-error' } : session())); return;
    }
    const name = req.url.split('/').pop();
    if (!['prototype', 'prototype.js', 'prototype.css'].includes(name)) { res.writeHead(404).end(); return; }
    const file = name === 'prototype' ? 'prototype.html' : name;
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html');
    res.end(await readFile(new URL('../../../../backend/ads-service/src/main/resources/private-kit-v1/' + file, import.meta.url)));
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await library.chromium.launch(videoBrowserOptions());
  try {
    const page = await browser.newPage();
    await page.addInitScript(() => sessionStorage.setItem('private-kit-session-v1', 'local-test-only'));
    const url = `http://127.0.0.1:${server.address().port}/api/pde/kit/private/v1/prototype`;
    await page.goto(url);
    await page.locator('#briefing').waitFor({ state: 'visible' });
    assert.equal(await page.locator('#identity').innerText(), 'Kit de barbearia');
    assert.match(await page.locator('.badge').innerText(), /dados de exemplo/);
    await checkCustomerSurface(page);
    await page.locator('[name=consentAccepted]').check();
    fail = true;
    await page.locator('#prepare').click();
    await page.locator('#error').waitFor({ state: 'visible' });
    assert.match(await page.locator('#error').innerText(), /Não foi possível concluir esta ação/);
    await checkCustomerSurface(page);
    status = 'FAILED';
    await page.reload();
    await page.locator('#title').filter({ hasText: 'interrompida' }).waitFor();
    assert.match(await page.locator('#status').innerText(), /retomar com segurança/);
    await checkCustomerSurface(page);
    status = 'READY';
    await page.reload();
    await page.locator('#result').waitFor({ state: 'visible' });
    assert.equal(await page.locator('#calendar').textContent(), 'Dia 1 — Apresente seu serviço | Post 1 | Story 1 | Legenda 1');
    await checkCustomerSurface(page);
    malformed = true;
    await page.reload();
    await page.locator('#error').waitFor({ state: 'visible' });
    assert.equal(await page.locator('#error').innerText(), 'Não foi possível abrir seu kit agora. Tente novamente.');
    await checkCustomerSurface(page);
  } finally { await browser.close(); await new Promise(resolve => server.close(resolve)); }
});

test('o contrato rejeita códigos, tamanhos e identificação interna sem proibir conteúdo útil', () => {
  for (const text of ['produto #19', 'ciclo #81', 'prototypeVersion: v1', 'arquivo · 982 bytes', 'Limites técnicos: 2 por fixture', 'posts/post-01.png'])
    assert.throws(() => validateCustomerSurface({ text }), /dados ou controles internos/);
  assert.doesNotThrow(() => validateCustomerSurface({ text: 'Seu kit tem 10 posts e um calendário de 7 dias.' }));
});
