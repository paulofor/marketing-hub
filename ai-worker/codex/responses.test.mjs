import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, readdir, rm, symlink } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { execute, translate, validateResponse, codexEnvironment, health, login } from './responses.mjs';

const schema = { type: 'object', properties: { ready: { type: 'boolean' } }, required: ['ready'], additionalProperties: false };
const body = (id = 'capella-105-offer') => ({ model: 'gpt-test', input: [{ role: 'user', content: 'Uma oferta útil para a cliente' }], text: { format: { type: 'json_schema', name: 'ready', schema } }, metadata: { idJob: id }, service_tier: 'flex' });

async function setup(t, config = {}) {
  const home = await mkdtemp(join(tmpdir(), 'ai-worker-codex-test-'));
  await writeFile(join(home, 'fixture.json'), JSON.stringify(config));
  t.after(() => rm(home, { recursive: true, force: true }));
  return { home, workspace: home, cacheDirectory: join(home, 'receipts'), command: resolve('fixtures/app-server.mjs'), noLock: true };
}
async function calls(options) {
  return (await readFile(join(options.home, 'calls.jsonl'), 'utf8')).trim().split('\n').map(JSON.parse);
}

test('gera uma saída validada, preservando tier, tokens, request e resposta real', async t => {
  const options = await setup(t);
  const response = validateResponse(body(), await execute(body(), options));
  assert.equal(response.output_text, '{"ready":true}');
  assert.equal(response.service_tier, 'default');
  assert.equal(response.usage.input_tokens, 80);
  assert.equal(response.codex_app_server.requestedServiceTier, 'flex');
  assert.equal(response.codex_app_server.metadata.idJob, 'capella-105-offer');
  assert.equal(response.codex_app_server.request.thread.approvalPolicy, 'never');
  assert.equal(response.codex_app_server.request.thread.sandbox, 'read-only');
  assert.equal(response.codex_app_server.request.turn.outputSchema.properties.ready.type, 'boolean');
  assert.ok(response.codex_app_server.events.some(e => e.method === 'turn/completed'));
});

test('recupera callback do mesmo job sem segunda inferência ou perda de uso', async t => {
  const options = await setup(t);
  const original = await execute(body(), options);
  const before = (await calls(options)).length;
  assert.deepEqual(await execute(body(), options), original);
  assert.equal((await calls(options)).length, before);
});

test('replay continua estável quando a JVM altera a ordem de serialização das chaves', async t => {
  const options = await setup(t);
  await execute({ ...body(), metadata: { idJob: 'stable-job', experimentId: 105 } }, options);
  await execute({ ...body(), metadata: { experimentId: 105, idJob: 'stable-job' } }, options);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 1);
});

test('separa outro produto e outra execução mesmo com texto idêntico', async t => {
  const options = await setup(t);
  await execute(body(), options);
  await execute(body('vega-106-offer'), options);
  await execute(body('capella-105-offer-2'), options);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 3);
  assert.equal((await readdir(options.cacheDirectory)).filter(f => f.endsWith('.json')).length, 3);
});

test('valida resposta inválida e recupera os mesmos bytes sem novo modelo', async t => {
  const options = await setup(t, { text: '{"ready":"talvez"}' });
  const raw = await execute(body(), options);
  assert.throws(() => validateResponse(body(), raw), /viola schema/);
  const before = (await calls(options)).length;
  assert.deepEqual(await execute(body(), options), raw);
  assert.equal((await calls(options)).length, before);
});

for (const text of ['não é JSON', '{}']) {
  test(`bloqueia saída fora do contrato: ${text}`, async t => {
    const options = await setup(t, { text });
    const response = await execute(body(), options);
    assert.throws(() => validateResponse(body(), response));
  });
}

test('bloqueia replay de despacho incerto após queda do processo', async t => {
  const options = await setup(t, { mode: 'disconnect' });
  await assert.rejects(execute(body(), options), /encerrou/);
  await assert.rejects(execute(body(), options), /EEXIST/);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 1);
});

test('turno incompleto preserva entrada, saída bruta, uso e correlação do job', async t => {
  const options = await setup(t, { status: 'failed' });
  await assert.rejects(execute(body(), options), /não concluído/);
  const file = (await readdir(options.cacheDirectory)).find(name => name.endsWith('.failures.jsonl'));
  const receipt = JSON.parse((await readFile(join(options.cacheDirectory, file), 'utf8')).trim());
  assert.equal(receipt.status, 'failed');
  assert.equal(receipt.inferenceStarted, true);
  assert.equal(receipt.usage.inputTokens, 80);
  assert.equal(receipt.codex_app_server.metadata.idJob, 'capella-105-offer');
  assert.ok(receipt.codex_app_server.events.some(e => e.params?.item?.text === '{"ready":true}'));
});

test('erro JSON-RPC conserva código e dados originais para diagnóstico', async t => {
  const options = await setup(t, { mode: 'rpc-error' });
  await assert.rejects(execute(body(), options), /Contrato recusado/);
  const file = (await readdir(options.cacheDirectory)).find(name => name.endsWith('.failures.jsonl'));
  const receipt = JSON.parse((await readFile(join(options.cacheDirectory, file), 'utf8')).trim());
  const event = receipt.codex_app_server.events.find(e => e.method === 'rpc/turn/start');
  assert.equal(event.params.response.error.code, -32602);
  assert.equal(event.params.response.error.data.field, 'outputSchema');
});

test('entrypoint real entrega recibo de falha ao Java e encerra sem nova inferência', async t => {
  const options = await setup(t, { status: 'failed' });
  const bin = join(options.home, 'bin');
  await mkdir(bin);
  await symlink(resolve('fixtures/app-server.mjs'), join(bin, 'codex'));
  const child = spawn(process.execPath, [resolve('responses.mjs')], { env: { ...process.env, CODEX_HOME: options.home, AI_WORKER_CODEX_WORKSPACE: options.workspace, AI_WORKER_CODEX_RECEIPTS: options.cacheDirectory, PATH: `${bin}:${process.env.PATH}` } });
  let output = '', error = '';
  child.stdout.on('data', chunk => output += chunk);
  child.stderr.on('data', chunk => error += chunk);
  child.stdin.end(JSON.stringify(body()));
  const code = await new Promise(resolve => child.on('exit', resolve));
  assert.equal(code, 1);
  assert.equal(JSON.parse(output).status, 'failed');
  assert.equal(JSON.parse(output).codex_app_server.metadata.idJob, 'capella-105-offer');
  assert.match(error, /não concluído/);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 1);
});

test('autenticação ausente não inicia turno nem bloqueia conexão futura', async t => {
  const options = await setup(t, { auth: false });
  await assert.rejects(execute(body(), options), /CODEX_AUTH_REQUIRED/);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 0);
  await writeFile(join(options.home, 'fixture.json'), '{}');
  assert.equal((await execute(body(), options)).status, 'completed');
});

test('não aceita autenticação por chave de API no lugar da sessão ChatGPT', async t => {
  const options = await setup(t, { authType: 'apiKey' });
  await assert.rejects(health(options), /CODEX_AUTH_REQUIRED/);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 0);
});

test('prova de saúde autentica sem executar inferência', async t => {
  const options = await setup(t);
  assert.equal((await health(options)).authenticated, true);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 0);
});

test('conecta sessão própria por device code sem inferência ou exposição de tokens', async t => {
  const options = await setup(t, { auth: false });
  const codes = [];
  assert.equal((await login(options, code => codes.push(code))).authenticated, true);
  assert.deepEqual(codes, [{ verificationUrl: 'https://auth.openai.com/codex/device', userCode: 'SYNTHETIC' }]);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 0);
});

test('preserva screenshot e ordem do contexto multimodal', () => {
  const input = translate({ model: 'gpt-test', input: [{ role: 'user', content: [{ type: 'input_text', text: 'Avalie a tela' }, { type: 'input_image', image_url: 'data:image/png;base64,cHJvdmE=' }] }] }).input;
  assert.deepEqual(input.at(-1), { type: 'image', url: 'data:image/png;base64,cHJvdmE=' });
});

test('mantém instruções de sistema como instruções do desenvolvedor, separadas do contexto da cliente', () => {
  const result = translate({ model: 'gpt-test', input: [{ role: 'system', content: 'Não invente provas' }, { role: 'user', content: 'Quero ver a oferta' }] });
  assert.equal(result.developerInstructions, 'Não invente provas');
  assert.deepEqual(result.input, [{ type: 'text', text: '[user]' }, { type: 'text', text: 'Quero ver a oferta' }]);
});

test('não descarta ferramentas ou entradas não suportadas', () => {
  assert.throws(() => translate({ ...body(), tools: [{ type: 'function' }] }), /Ferramentas/);
  assert.throws(() => translate({ ...body(), input: [{ role: 'user', content: [{ type: 'input_audio' }] }] }), /Entrada não suportada/);
  assert.throws(() => translate({ ...body(), background: true }), /isolado/);
});

test('schema inválido é bloqueado antes de iniciar processo ou inferência', async t => {
  const options = await setup(t);
  const invalid = { ...body(), text: { format: { type: 'json_schema', schema: { type: 'tipo-inexistente' } } } };
  await assert.rejects(execute(invalid, options), /schema is invalid/);
  await assert.rejects(readFile(join(options.home, 'calls.jsonl')), { code: 'ENOENT' });
});

test('remove chaves e tokens das variáveis herdadas pelo App Server', () => {
  assert.deepEqual(codexEnvironment({ PATH: '/usr/bin', CODEX_HOME: '/isolated', OPENAI_API_KEY: 'synthetic', GH_TOKEN: 'synthetic', AWS_SECRET_ACCESS_KEY: 'synthetic', CODEX_API_KEY: 'synthetic', CODEX_ACCESS_TOKEN: 'synthetic' }), { PATH: '/usr/bin', CODEX_HOME: '/isolated' });
});

test('recusa pedido de ferramenta e não interpreta a execução como concluída', async t => {
  const options = await setup(t, { mode: 'tool' });
  await assert.rejects(execute(body(), options), /não autorizada/);
});

test('não inventa custo ou tokens quando não foram retornados', async t => {
  const options = await setup(t, { missingUsage: true });
  assert.equal((await execute(body(), options)).usage, null);
});

test('não substitui automaticamente modelo indisponível', async t => {
  const options = await setup(t);
  await assert.rejects(execute({ ...body(), model: 'modelo-ausente' }, options), /indisponível/);
  assert.equal((await calls(options)).filter(c => c.method === 'turn/start').length, 0);
});
