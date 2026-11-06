import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { mkdtemp, writeFile, readFile, rm, readdir, rename } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { runBounded, header, protocol } from './single-video-bounded-agent.mjs';

async function fixture(t, options = {}) {
  const directory = await mkdtemp(join(tmpdir(), 'mh-bounded-test-'));
  const rows = new Map();
  const counts = { inference: 0, count: 0, callback: 0, promptReads: 0 };
  let callbackFailure = options.callbackFailure;
  let beforeBeginFailure = options.beforeBeginFailure;
  let reserved = 0;
  const server = createServer(async (request, response) => {
    const chunks = []; for await (const chunk of request) chunks.push(chunk);
    const body = chunks.length ? JSON.parse(Buffer.concat(chunks)) : null;
    const reply = (status, value) => { response.writeHead(status, { 'Content-Type': 'application/json' }); response.end(JSON.stringify(value)); };
    if (request.url.startsWith('/api/')) {
      assert.equal(request.headers[header.toLowerCase()], protocol);
      const task = request.url.match(/stage-executions\/(\d+)/)?.[1];
      const key = request.url.match(/stage-executions\/\d+/)?.[0];
      if (request.url.endsWith('/spending-context')) {
        if (options.invalidContext) return reply(200, {});
        return reply(200, { required: options.legacy !== true, receiptId: options.receiptId ?? 128,
          projectId: options.projectId ?? 14, blocker: null, reservation: rows.get(key) ?? null });
      }
      if (request.url.endsWith('/spend-reservations')) {
        if (options.deny || reserved + .6 > (options.limit ?? 15)) return reply(409, { reason: 'Teto integral sem cobertura.' });
        reserved += .6;
        const lease = { id: Number(task), receiptId: options.receiptId ?? 128, state: 'RESERVED', request: body.request,
          maximumUsd: .6, requestSha256: 'backend-hash', rawResponse: null, mayInvoke: false };
        rows.set(key, lease); return reply(200, lease);
      }
      const row = rows.get(key);
      if (request.url.endsWith('/begin')) {
        if (beforeBeginFailure) { beforeBeginFailure = false; return reply(503, { notSubmitted: true }); }
        const mayInvoke = row.state === 'RESERVED'; row.state = 'SUBMITTED';
        return reply(200, { ...row, mayInvoke });
      }
      if (request.url.endsWith('/result')) {
        counts.callback++;
        if (callbackFailure) { callbackFailure = false; return reply(503, { unavailable: true }); }
        row.state = body.rawResponse ? 'RESPONSE_RECORDED' : 'UNCERTAIN';
        row.rawResponse = body.rawResponse; return reply(200, row);
      }
      return reply(404, {});
    }
    if (request.url === '/v1/responses/input_tokens') {
      counts.count++; assert.equal(body.model, 'gpt-6.1-sol');
      return reply(200, { object: 'response.input_tokens', input_tokens: 650 });
    }
    if (request.url === '/v1/responses') {
      counts.inference++;
      assert.equal(body.max_output_tokens, 32768);
      assert.equal(body.service_tier, 'flex');
      assert.equal(body.model, 'gpt-6.1-sol');
      assert.equal(body.tools, undefined);
      assert.equal(body.previous_response_id, undefined);
      assert.equal(body.input[0].content[0].text, 'Evidência sintética; não é vídeo nem aprovação premium.');
      if (options.uncertain) { request.socket.destroy(); return; }
      return reply(200, { id: 'resp-test', status: 'completed', service_tier: 'flex', output: [
        { content: [{ type: 'output_text', text: '{"decision":"ADJUST","testOnly":true}' }] }],
        usage: { input_tokens: 650, output_tokens: 80, input_tokens_details: { cached_tokens: 12 } } });
    }
    return reply(404, {});
  });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  const url = `http://127.0.0.1:${server.address().port}`;
  const reviewKey = join(directory, 'review-api-key');
  await writeFile(reviewKey, 'test-restricted-review-key', { mode: 0o600 });
  const schema = join(directory, 'schema.json');
  await writeFile(schema, '{"type":"object","properties":{"decision":{"type":"string"}},"required":["decision"],"additionalProperties":false}');
  const invoke = async (task = '884', agent = 'communication-director', changes = {}) => {
    const output = join(directory, `result-${task}.json`);
    const result = await runBounded({ args: ['--model', 'gpt-6.1-sol', '--config', 'model_reasoning_effort="max"',
      '--output-schema', schema, '--output-last-message', output],
      env: { MH_AGENT_KEY: agent, MH_AGENT_TASK_ID: task, MH_SPENDING_BACKEND_URL: url,
        MH_BOUNDED_REVIEW_KEY_FILE: reviewKey, OPENAI_API_KEY: 'test-not-a-real-key', MH_BOUNDED_OPENAI_URL: `${url}/v1/`,
        MH_BOUNDED_STATE_DIRECTORY: directory, ...changes },
      input: async () => { counts.promptReads++; return changes.PROMPT_TEXT ?? 'Evidência sintética; não é vídeo nem aprovação premium.'; },
    });
    return { result, output };
  };
  t.after(async () => { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); await rm(directory, { recursive: true, force: true }); });
  return { directory, invoke, counts, rows, liability: () => reserved };
}

test('um request efetivamente limitado preserva modelo, saída, auditoria e resultado', async t => {
  const f = await fixture(t); const { result, output } = await f.invoke();
  assert.equal(result.legacy, false); assert.equal(result.event.usage.output_tokens, 80);
  assert.equal(result.serviceTier, 'flex'); assert.equal(JSON.parse(await readFile(output)).testOnly, true);
  assert.equal(f.counts.inference, 1); assert.equal(f.counts.callback, 1);
});

test('recupera callback após reinício, sem credencial, contagem ou inferência nova', async t => {
  const f = await fixture(t, { callbackFailure: true });
  await assert.rejects(f.invoke(), /Autoridade de gasto indisponível/);
  for (const name of await readdir(f.directory)) {
    if (/^128-[0-9]+\.json$/.test(name)) await rename(join(f.directory,name), join(f.directory,name+'.pending'));
  }
  const replay = await f.invoke('884', 'communication-director', { OPENAI_API_KEY: '' });
  assert.equal(replay.result.legacy, false);
  assert.equal(f.counts.inference, 1); assert.equal(f.counts.count, 1); assert.equal(f.counts.callback, 2);
});

test('retoma reserva não enviada com a entrada original, sem criar outra reserva', async t => {
  const f=await fixture(t,{beforeBeginFailure:true});
  await assert.rejects(f.invoke(), /Autoridade de gasto indisponível/);
  assert.equal(f.counts.inference,0);
  await f.invoke('884','communication-director',{PROMPT_TEXT:'Contexto recomposto depois do reinício.'});
  assert.equal(f.counts.inference,1); assert.equal(f.counts.count,1); assert.equal(f.liability(),.6);
});

test('resultado incerto retém reserva e recusa nova chamada após reinício', async t => {
  const f = await fixture(t, { uncertain: true });
  await assert.rejects(f.invoke());
  await assert.rejects(f.invoke(), /incerto/);
  assert.equal(f.counts.inference, 1); assert.equal(f.counts.count, 1); assert.equal(f.liability(), .6);
});

test('nega antes de inferir quando o teto compartilhado não cobre a chamada', async t => {
  const f = await fixture(t, { deny: true }); await assert.rejects(f.invoke(), /HTTP 409/);
  assert.equal(f.counts.inference, 0);
});

test('três agentes concorrentes disputam o mesmo teto e só duas chamadas são enviadas', async t => {
  const f = await fixture(t, { limit: 1.2 });
  const results = await Promise.allSettled([
    f.invoke('101', 'financial-agent'), f.invoke('102', 'customer-agent'), f.invoke('103', 'meta-ad-approver'),
  ]);
  assert.equal(results.filter(x => x.status === 'fulfilled').length, 2);
  assert.equal(f.counts.inference, 2); assert.equal(f.liability(), 1.2);
});

test('outro produto e recibo recebem estado independente sem herdar reserva ou resposta', async t => {
  const f = await fixture(t, { receiptId: 900128, projectId: 9014 });
  const { result } = await f.invoke('900884'); assert.equal(result.receiptId, 900128);
  assert.equal(f.counts.inference, 1);
});

test('ausência de recibo mantém o caminho anterior sem ler prompt ou consultar provedor', async t => {
  const f = await fixture(t, { legacy: true }); const { result } = await f.invoke();
  assert.equal(result.legacy, true); assert.equal(f.counts.promptReads, 0); assert.equal(f.counts.count, 0); assert.equal(f.counts.inference, 0);
});

test('falha da autoridade não libera fallback de custo desconhecido', async t => {
  const f = await fixture(t, { invalidContext: true }); await assert.rejects(f.invoke(), /sem resposta válida/);
  assert.equal(f.counts.count, 0); assert.equal(f.counts.inference, 0);
});


test('Têmis não usa a credencial do produtor como fallback de sua revisão', async t => {
  const f = await fixture(t);
  await assert.rejects(f.invoke('900', 'meta-ad-approver', { MH_BOUNDED_REVIEW_KEY_FILE: '' }), /Credencial de API/);
  assert.equal(f.counts.count, 0); assert.equal(f.counts.inference, 0);
});

test('marcação protegida contraditória não cai no transporte legado', async t => {
  const f = await fixture(t, { legacy: true });
  await assert.rejects(f.invoke('900', 'customer-agent', { MH_EXPECT_SHARED_SPENDING: 'true' }), /fallback recusado/);
  assert.equal(f.counts.promptReads, 0); assert.equal(f.counts.inference, 0);
});
