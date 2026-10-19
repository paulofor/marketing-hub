import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { mkdtemp, writeFile, readFile, rm, readdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { poll, replay, reportSession } from './connection.mjs';

// Isola provedor, backend, sessão e recibos para cada identidade sintética.
async function fixture(t, config = {}, id = 42) {
  const directory = await mkdtemp(join(tmpdir(), 'worker-connection-'));
  await writeFile(join(directory, 'fixture.json'), JSON.stringify(config));
  const options = { home: directory, workspace: directory, cacheDirectory: directory,
    command: resolve('fixtures/app-server.mjs'), noLock: true };
  const calls = [];
  let terminalFailures = 0;
  let codeFailures = 0;
  let pending = true;
  const server = createServer(async (req, res) => {
    let body = '';
    for await (const chunk of req) body += chunk;
    calls.push({ path: req.url, body: body ? JSON.parse(body) : null });
    res.setHeader('Content-Type', 'application/json');
    if (req.url.endsWith('/pending')) {
      if (!pending) { res.writeHead(204); res.end(); return; }
      pending = false;
      res.end(JSON.stringify({ id })); return;
    }
    if (req.url.endsWith('/completion') && terminalFailures-- > 0
        || req.url.endsWith('/device-code') && codeFailures-- > 0) res.statusCode = 503;
    res.end('{}');
  });
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  t.after(async () => { server.closeAllConnections(); server.close(); await rm(directory, { recursive: true, force: true }); });
  return { options, calls, base: `http://127.0.0.1:${server.address().port}`,
    failCompletion: n => { terminalFailures = n; }, failCode: n => { codeFailures = n; } };
}

test('conecta pelo backend sem iniciar turno ou transportar credencial; recupera callbacks transitórios', async t => {
  const f = await fixture(t, { auth: false });
  f.failCode(1);
  f.failCompletion(1);
  await poll(f.base, f.options);
  const codes = f.calls.filter(c => c.path.endsWith('/device-code'));
  assert.equal(codes.length, 2);
  assert.deepEqual(codes[0].body, { verificationUrl: 'https://auth.openai.com/codex/device', userCode: 'SYNTHETIC' });
  assert.deepEqual(f.calls.at(-1).body, { authenticated: true });
  const requests = (await readFile(join(f.options.home, 'calls.jsonl'), 'utf8')).split('\n').filter(Boolean).map(JSON.parse);
  assert.equal(requests.filter(r => r.method === 'account/login/start').length, 1);
  assert.equal(requests.some(r => r.method === 'turn/start'), false);
  assert.equal(/token|email|authType/i.test(JSON.stringify(f.calls)), false);
});

test('recibo de sucesso sobrevive à falha total do callback e é recuperado sem novo login', async t => {
  const f = await fixture(t, {}, 87);
  f.failCompletion(3);
  await assert.rejects(poll(f.base, f.options), /CALLBACK_UNAVAILABLE/);
  assert.deepEqual(JSON.parse(await readFile(join(f.options.cacheDirectory, 'connection/87.pending.json'))), { id: 87, body: { authenticated: true } });
  await replay(f.base, f.options);
  assert.deepEqual(await readdir(join(f.options.cacheDirectory, 'connection')), []);
  const requests = await readFile(join(f.options.home, 'calls.jsonl'), 'utf8');
  assert.equal(requests.match(/account\/login\/start/g).length, 1);
});

test('recusa sessão API key e URL que não pertence à autenticação oficial', async t => {
  for (const config of [{ authType: 'apiKey' }, { verificationUrl: 'https://invalid.example/login' }]) {
    const f = await fixture(t, config);
    await poll(f.base, f.options);
    assert.deepEqual(f.calls.at(-1).body, { authenticated: false });
    if (config.verificationUrl) assert.equal(f.calls.some(c => c.path.endsWith('/device-code')), false);
  }
});

test('prova independente diferencia sessão válida e ausência de autenticação, sem login', async t => {
  for (const authenticated of [true, false]) {
    const f = await fixture(t, { auth: authenticated });
    await reportSession(f.base, f.options);
    assert.deepEqual(f.calls.at(-1).body, { authenticated });
    const requests = await readFile(join(f.options.home, 'calls.jsonl'), 'utf8');
    assert.equal(requests.includes('account/login/start'), false);
    assert.equal(requests.includes('turn/start'), false);
  }
});
