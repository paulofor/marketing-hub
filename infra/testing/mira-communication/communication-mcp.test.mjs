// Exercita o servidor MCP real de Íris contra HTTP local, sem modelo ou credenciais.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { spawn } from 'node:child_process';
import readline from 'node:readline';
import path from 'node:path';

const executable = path.resolve('communication-agent-worker/src/main/resources/mcp/communication-agent.mjs');

for (const [reference, type, id] of [
  ['product:900010@agent-validation-v1', 'PRODUCT', '900010'],
  ['experiment:900092', 'EXPERIMENT', '900092'],
  ['commercial-plan:900003@v7', 'COMMERCIAL_PLAN', '900003'],
]) {
  test(`contexto e memória preservam ${reference}`, { timeout: 10000 }, async (t) => {
    const calls = [];
    const server = http.createServer(async (req, res) => {
      let body = '';
      for await (const data of req) body += data;
      calls.push({ method: req.method, url: new URL(req.url, 'http://localhost'), body: body ? JSON.parse(body) : null });
      res.setHeader('content-type', 'application/json');
      res.end(JSON.stringify({ sourceReference: reference, synthetic: true }));
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const child = spawn(process.execPath, [executable], { env: {
      MCP_MARKETING_HUB_URL: `http://127.0.0.1:${server.address().port}`,
      MCP_TASK_ID: '900001', MCP_SOURCE_REFERENCE: reference,
    }});
    t.after(() => { child.kill(); server.closeAllConnections(); server.close(); });
    let nextId = 0;
    const pending = new Map();
    readline.createInterface({input: child.stdout}).on('line', line => {
      const reply = JSON.parse(line);
      pending.get(reply.id)?.resolve(reply);
      pending.delete(reply.id);
    });
    let error = '';
    child.stderr.on('data', bytes => { error += bytes; });
    child.on('exit', code => {
      for (const request of pending.values()) request.reject(new Error(`MCP encerrou com ${code}: ${error}`));
    });
    const rpc = (method, params) => new Promise((resolve, reject) => {
      const requestId = ++nextId;
      pending.set(requestId, {resolve, reject});
      child.stdin.write(JSON.stringify({jsonrpc:'2.0', id:requestId, method, params}) + '\n');
    });
    assert.ok((await rpc('initialize', {})).result.capabilities.tools);
    assert.equal((await rpc('tools/list', {})).result.tools.length, 3);
    for (const name of ['consultar_contexto_tarefa','recuperar_memoria_especializada','registrar_aprendizado_candidato']) {
      const reply = await rpc('tools/call', {name, arguments:{
        specialty:'Comunicação', content:'Hipótese exclusivamente local', evidence:'Teste sintético segregado', confidence:0.3,
        scopeType:'EXPERIMENT', scopeId:'999999',
      }});
      assert.equal(reply.error, undefined);
    }
    assert.equal(calls.length, 3);
    assert.equal(calls[0].url.pathname, '/api/internal/agent-tasks/communication-director/stage-executions/900001');
    assert.equal(calls[1].url.searchParams.get('scopeType'), type);
    assert.equal(calls[1].url.searchParams.get('scopeId'), id);
    assert.equal(calls[2].body.scopeType, type);
    assert.equal(calls[2].body.scopeId, id);
    assert.equal(calls[2].body.sourceExecutionId, 'agent-task-900001');
    const rejected = await rpc('tools/call', {name:'publicar_campanha'});
    assert.match(rejected.error.message, /não permitida/);
    assert.equal(calls.length, 3);
  });
}

test('referência privada incompleta é recusada antes de acessar HTTP', {timeout:5000}, async () => {
  const child = spawn(process.execPath, [executable], {env:{
    MCP_MARKETING_HUB_URL:'http://127.0.0.1:1', MCP_TASK_ID:'900001', MCP_SOURCE_REFERENCE:'product:900010',
  }});
  child.stderr.resume();
  const code = await new Promise(resolve => child.on('exit', resolve));
  assert.notEqual(code, 0);
});
