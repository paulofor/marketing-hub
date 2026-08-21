import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import readline from 'node:readline';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  createClarityAggregateServer,
  validateAggregateResponse,
  validateArguments
} from '../../main/resources/mcp/clarity-aggregate.mjs';

test('expõe somente consulta agregada e persiste a resposta oficial', async () => {
  const requests = [];
  const backend = http.createServer(async (request, response) => {
    let body = '';
    request.on('data', chunk => { body += chunk; });
    await once(request, 'end');
    requests.push({ url: request.url, body: body ? JSON.parse(body) : null });
    response.setHeader('Content-Type', 'application/json');
    if (request.url.endsWith('/reserve')) {
      response.end(JSON.stringify({ id: 44, queryText: 'Aggregate por dispositivo para /flows/exp-88-' }));
    } else {
      response.end(JSON.stringify({ id: 44, status: 'COMPLETED' }));
    }
  });
  backend.listen(0, '127.0.0.1');
  await once(backend, 'listening');
  const fakeClarity = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fake-clarity.mjs');
  const secretDirectory = mkdtempSync(path.join(os.tmpdir(), 'clarity-token-'));
  const tokenFile = path.join(secretDirectory, 'token');
  writeFileSync(tokenFile, 'token-de-teste', { mode: 0o600 });
  const server = createClarityAggregateServer({
    MCP_BACKEND_URL: `http://127.0.0.1:${backend.address().port}`,
    MCP_EXECUTION_ID: '19',
    CLARITY_API_TOKEN_FILE: tokenFile,
    CLARITY_MCP_ENTRYPOINT: fakeClarity
  });
  try {
    const listed = await server.dispatch({ method: 'tools/list' });
    assert.deepEqual(listed.tools.map(tool => tool.name), ['consultar_snapshot_comportamental_agregado']);
    const result = await server.dispatch({
      method: 'tools/call',
      params: { name: 'consultar_snapshot_comportamental_agregado', arguments: { experimentId: 88, dimension: 'DEVICE', windowDays: 2 } }
    });
    assert.match(result.content[0].text, /"aggregateOnly":true/);
    assert.match(result.content[0].text, /"sessions":12/);
    assert.equal(requests[0].body.dimension, 'DEVICE');
    assert.equal(requests[0].body.experimentId, 88);
    const persistedRaw = JSON.parse(requests[1].body.rawResponse);
    assert.deepEqual(JSON.parse(persistedRaw.content[0].text), {
      sessions: 12,
      rageClicks: 2,
      dimension: 'mobile'
    });
  } finally {
    await server.close();
    backend.close();
    rmSync(secretDirectory, { recursive: true, force: true });
  }
});

test('rejeita dimensões, janelas e respostas granulares', () => {
  assert.throws(() => validateArguments({ experimentId: 88, dimension: 'RECORDING', windowDays: 2 }), /Dimensão/);
  assert.throws(() => validateArguments({ experimentId: 88, dimension: 'PAGE', windowDays: 4 }), /1 a 3 dias/);
  assert.throws(() => validateArguments({ dimension: 'PAGE', windowDays: 2 }), /Experimento obrigatório/);
  assert.throws(() => validateAggregateResponse('{"sessionId":"individual"}'), /individual proibido/);
  assert.doesNotThrow(() => validateAggregateResponse('{"sessions":10,"page":"/oferta"}'));
});

test('negocia o protocolo MCP oficial e publica somente a ferramenta agregada', async () => {
  const adapter = fileURLToPath(new URL('../../main/resources/mcp/clarity-aggregate.mjs', import.meta.url));
  const child = spawn(process.execPath, [adapter], {
    env: {
      ...process.env,
      MCP_BACKEND_URL: 'http://127.0.0.1:1',
      MCP_EXECUTION_ID: '19',
      CLARITY_API_TOKEN: 'token-de-teste'
    },
    stdio: ['pipe', 'pipe', 'pipe']
  });
  const output = readline.createInterface({ input: child.stdout, crlfDelay: Infinity });
  const responses = new Map();
  const waiting = new Map();
  output.on('line', line => {
    const response = JSON.parse(line);
    if (response.id === undefined) return;
    const waiter = waiting.get(response.id);
    if (waiter) {
      waiting.delete(response.id);
      waiter(response);
    } else {
      responses.set(response.id, response);
    }
  });

  function request(id, method, params) {
    child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
    return new Promise((resolve, reject) => {
      const ready = responses.get(id);
      if (ready) {
        responses.delete(id);
        resolve(ready);
        return;
      }
      const timeout = setTimeout(() => {
        waiting.delete(id);
        reject(new Error(`Timeout aguardando ${method}`));
      }, 5000);
      waiting.set(id, response => {
        clearTimeout(timeout);
        resolve(response);
      });
    });
  }

  try {
    const initialized = await request(1, 'initialize', {
      protocolVersion: '2025-03-26',
      capabilities: {},
      clientInfo: { name: 'teste-marketing-hub', version: '1.0.0' }
    });
    assert.equal(initialized.result.serverInfo.name, 'marketing-hub-clarity-aggregate');
    child.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');
    const listed = await request(2, 'tools/list', {});
    assert.deepEqual(listed.result.tools.map(tool => tool.name), ['consultar_snapshot_comportamental_agregado']);
    assert.deepEqual(listed.result.tools[0].inputSchema.required.sort(), ['dimension', 'experimentId', 'windowDays']);
    assert.equal(listed.result.tools[0].inputSchema.additionalProperties, false);
  } finally {
    child.stdin.end();
    child.kill('SIGTERM');
    output.close();
  }
});
