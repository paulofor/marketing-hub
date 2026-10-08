import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import { spawn } from 'node:child_process';
import { VisualPersonalizationWorker, buildVisualRequest } from '../src/visual-personalization-v1.js';

const jobId = 'pde-visual-v1-00000000-0000-4000-8000-000000000001';
const entry = { jobId, status: 'PDE_RUNNING', productName: 'Produto simulado', productVersion: 'private-v1',
  contractVersion: 'PDE_VISUAL_PERSONALIZATION_V1', trafficClass: 'AGENT_VALIDATION', model: 'gpt-5.6',
  inputHash: 'a'.repeat(64), input: { occasion: 'Jantar', pieces: ['camisa', 'calça'], preferences: 'Conforto', constraints: '' } };
const prefix = 'http://backend.local/api/pde/visual-personalization/v1/internal/stage-executions';
const response = (value, status = 200) => new Response(typeof value === 'string' ? value : JSON.stringify(value), { status });

async function fixture(t, options = {}) {
  const spoolDir = await fs.mkdtemp(path.join(os.tmpdir(), 'visual-worker-'));
  t.after(() => fs.rm(spoolDir, { recursive: true, force: true }));
  const events = [];
  let failCallback = options.failCallback;
  const fetchImpl = async (url, request = {}) => {
    events.push({ url, request });
    if (url.endsWith('/pending')) return response(options.pending || [entry]);
    if (url.endsWith('/claim')) return response(options.blocked ? { ...entry, status: 'PDE_BLOCKED' } : entry);
    if (url.endsWith('/request')) return response({}, options.rejectRequest ? 409 : 200);
    if (url.endsWith('/result')) {
      if (failCallback) { failCallback = false; return response({}, 503); }
      return response({ status: 'PDE_COMPLETED' });
    }
    if (url.endsWith('/replay')) return response({ status: 'PDE_COMPLETED' });
    assert.equal(url, 'https://api.openai.com/v1/responses');
    if (options.networkFailure) throw new Error('Interrupção simulada depois do envio');
    return response(options.raw || { id: 'native-fixture', metadata: JSON.parse(request.body).metadata, output: [] }, options.httpStatus || 200);
  };
  const make = (apiKey = 'synthetic-key') => new VisualPersonalizationWorker({ backendUrl: 'http://backend.local',
    internalToken: 'synthetic-internal-token', apiKey, fetchImpl, spoolDir });
  return { events, spoolDir, make };
}

test('request versionada preserva entrada, correlação e uma única ferramenta em Flex', async () => {
  const result = await buildVisualRequest(entry);
  assert.equal(result.service_tier, 'flex');
  assert.equal(result.max_tool_calls, 1);
  assert.deepEqual(JSON.parse(result.input[2].content), entry.input);
  assert.equal(result.metadata.mh_job_id, jobId);
  assert.equal(result.tools.length, 1);
  assert.equal(result.tools[0].size, '1024x1024');
  assert.equal(result.tools[0].output_format, 'png');
  await assert.rejects(buildVisualRequest({ ...entry, trafficClass: 'COMMERCIAL' }));
  const second = await buildVisualRequest({ ...entry, inputHash: 'b'.repeat(64), input: { ...entry.input, occasion: 'Outro jantar' } });
  assert.notEqual(second.input[2].content, result.input[2].content);
  assert.notEqual(second.metadata.mh_input_hash, result.metadata.mh_input_hash);
});

test('callback perdido é persistido e recuperado após reinício sem segunda chamada paga', async (t) => {
  const f = await fixture(t, { failCallback: true });
  await assert.rejects(f.make().processNextPending(), /503/);
  const saved = JSON.parse(await fs.readFile(path.join(f.spoolDir, jobId + '.json'), 'utf8'));
  assert.equal(saved.result.providerCalled, true);
  assert.equal(saved.result.rawResponse.id, 'native-fixture');
  await f.make(null).processNextPending();
  assert.equal(f.events.filter(e => e.url === 'https://api.openai.com/v1/responses').length, 1);
  assert.equal(f.events.filter(e => e.url.endsWith('/result')).length, 2);
  assert.deepEqual(await fs.readdir(f.spoolDir), []);
  const raw = f.events.filter(e => e.url.endsWith('/result')).map(e => JSON.parse(e.request.body));
  assert.deepEqual(raw[0], raw[1]);
  assert.ok(f.events.findIndex(e => e.url.endsWith('/request')) < f.events.findIndex(e => e.url.includes('api.openai.com')));
});

test('replay de resposta já persistida nunca chama modelo ou reserva nova tentativa', async (t) => {
  const f = await fixture(t, { pending: [{ ...entry, replayOnly: true }] });
  await f.make().processNextPending();
  assert.deepEqual(f.events.map(e => e.url), [prefix + '/pending', prefix + '/' + jobId + '/replay']);
});

test('replay já recebido continua sem credencial de IA', async (t) => {
  const f = await fixture(t, { pending: [{ ...entry, replayOnly: true }] });
  await f.make(null).processNextPending();
  assert.equal(f.events.at(-1).url, prefix + '/' + jobId + '/replay');
});

test('fila visual indisponível preserva o consumo anterior da fila MUSA', { timeout: 5000 }, async (t) => {
  let release;
  const observed = new Promise(resolve => { release = resolve; });
  const server = http.createServer((req, res) => {
    if (req.url.includes('visual-personalization')) { res.writeHead(404); res.end('{}'); return; }
    assert.equal(req.url, '/api/internal/pde/ai-guidance/stage-executions/pending');
    res.writeHead(200); res.end('[]'); release();
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'visual-loop-'));
  const child = spawn(process.execPath, [new URL('../src/worker.js', import.meta.url).pathname], {
    env: { PATH: process.env.PATH, PDE_BACKEND_URL: 'http://127.0.0.1:' + server.address().port,
      PDE_INTERNAL_API_TOKEN: 'synthetic-internal', OPENAI_API_KEY: 'synthetic-key',
      OPENAI_API_KEY_FILE: path.join(dir, 'missing-key'), PDE_VISUAL_CALLBACK_SPOOL_DIR: dir, POLL_INTERVAL_MS: '1000' },
    stdio: 'ignore',
  });
  t.after(async () => { child.kill('SIGTERM'); server.close(); await fs.rm(dir, { recursive: true, force: true }); });
  await observed;
});

test('fila visual lenta não atrasa a descoberta MUSA nem abre outra geração visual', { timeout: 5000 }, async (t) => {
  let release, visualCalls = 0, musaCalls = 0;
  const observed = new Promise(resolve => { release = resolve; });
  const server = http.createServer((req, res) => {
    if (req.url.includes('visual-personalization')) { visualCalls++; return; }
    musaCalls++; res.writeHead(200); res.end('[]');
    if (musaCalls === 2) release();
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'visual-slow-'));
  const child = spawn(process.execPath, [new URL('../src/worker.js', import.meta.url).pathname], {
    env: { PATH: process.env.PATH, PDE_BACKEND_URL: 'http://127.0.0.1:' + server.address().port,
      PDE_INTERNAL_API_TOKEN: 'synthetic-internal', OPENAI_API_KEY: 'synthetic-key',
      OPENAI_API_KEY_FILE: path.join(dir, 'missing-key'), PDE_VISUAL_CALLBACK_SPOOL_DIR: dir, POLL_INTERVAL_MS: '50' },
    stdio: 'ignore',
  });
  t.after(async () => { child.kill('SIGTERM'); server.close(); await fs.rm(dir, { recursive: true, force: true }); });
  await observed;
  assert.equal(visualCalls, 1);
  assert.equal(musaCalls, 2);
});

test('bloqueio antes da chamada não consome provedor', async (t) => {
  const f = await fixture(t, { blocked: true });
  await f.make().processNextPending();
  assert.equal(f.events.length, 2);
});

test('request recusada conclui falha local sem inventar consumo de provedor', async (t) => {
  const f = await fixture(t, { rejectRequest: true });
  await f.make().processNextPending();
  const result = JSON.parse(f.events.find(e => e.url.endsWith('/result')).request.body);
  assert.equal(result.providerCalled, false);
  assert.equal(f.events.filter(e => e.url.includes('api.openai.com')).length, 0);
});

test('timeout depois de iniciar provedor deixa custo desconhecido e preserva a falha', async (t) => {
  const f = await fixture(t, { networkFailure: true });
  await f.make().processNextPending();
  const result = JSON.parse(f.events.find(e => e.url.endsWith('/result')).request.body);
  assert.equal(result.providerCalled, true);
  assert.match(result.rawResponse.error.message, /Interrupção/);
});

test('erro não JSON do provedor é enviado integralmente para auditoria', async (t) => {
  const f = await fixture(t, { raw: 'falha nativa simulada', httpStatus: 502 });
  await f.make().processNextPending();
  const result = JSON.parse(f.events.find(e => e.url.endsWith('/result')).request.body);
  assert.equal(result.rawResponse.providerRawBody, 'falha nativa simulada');
  assert.equal(result.httpStatus, 502);
  assert.equal(result.providerCalled, true);
});

test('sem credencial consulta estado, mas não reserva trabalho pago nem usa origem externa alternativa', async (t) => {
  const f = await fixture(t);
  await f.make(null).processNextPending();
  assert.deepEqual(f.events.map(e => e.url), [prefix + '/pending']);
  assert.throws(() => new VisualPersonalizationWorker({ backendUrl: 'http://backend.local',
    providerUrl: 'https://example.org/responses', spoolDir: f.spoolDir }), /origem oficial/);
});
