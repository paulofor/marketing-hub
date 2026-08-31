import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import http from 'node:http';
import path from 'node:path';
import readline from 'node:readline';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const mcpEntrypoint = fileURLToPath(
  new URL('../../main/resources/mcp/marketing-hub-readonly.mjs', import.meta.url)
);

test('injeta memoria da ferramenta no momento da consulta sem alterar o payload oficial', async () => {
  const requests = [];
  const backend = await startBackend(async (request, response) => {
    requests.push(request.url);
    response.setHeader('Content-Type', 'application/json');
    if (request.url.includes('/api/internal/agent-memory/v1/agents/growth-operator')) {
      response.end(JSON.stringify([{ id: 7, status: 'CONFIRMED', content: 'Nao confundir custo tecnico com gasto Meta.' }]));
      return;
    }
    response.end(JSON.stringify({ visitors: 0, spend: 0 }));
  });
  const client = startMcp(backend.address().port, { MCP_EXPERIMENT_ID: '88' });
  try {
    const listed = await client.request('tools/list', {});
    assert(listed.result.tools.some(tool => tool.name === 'recuperar_memoria_especializada'));
    assert(listed.result.tools.some(tool => tool.name === 'registrar_aprendizado_candidato'));
    const cockpitTool = listed.result.tools.find(tool => tool.name === 'consultar_cockpit');
    assert.deepEqual(cockpitTool.annotations, {
      readOnlyHint: true,
      openWorldHint: true,
      destructiveHint: false
    });
    const memoryWriteTool = listed.result.tools.find(
      tool => tool.name === 'registrar_aprendizado_candidato'
    );
    assert.deepEqual(memoryWriteTool.annotations, {
      readOnlyHint: false,
      openWorldHint: true,
      destructiveHint: false
    });

    const result = await client.request('tools/call', {
      name: 'consultar_cockpit',
      arguments: {}
    });

    assert.equal(result.result.content.length, 2);
    assert.deepEqual(JSON.parse(result.result.content[0].text).data, { visitors: 0, spend: 0 });
    const injected = JSON.parse(result.result.content[1].text).justInTimeMemory;
    assert.equal(injected.scopeType, 'MCP_TOOL');
    assert.equal(injected.scopeId, 'consultar_cockpit');
    assert.equal(injected.items[0].status, 'CONFIRMED');
    assert.match(injected.evidenceBoundary, /nunca prova do resultado atual/);
    assert(requests.some(value => value.includes('scopeType=MCP_TOOL')));
    assert(requests.some(value => value.includes('scopeId=consultar_cockpit')));
  } finally {
    await client.close();
    backend.close();
  }
});

test('mantem a consulta comercial disponivel quando a memoria just-in-time falha', async () => {
  const backend = await startBackend(async (request, response) => {
    response.setHeader('Content-Type', 'application/json');
    if (request.url.includes('/api/internal/agent-memory/v1/agents/growth-operator')) {
      response.statusCode = 503;
      response.end(JSON.stringify({ error: 'indisponivel' }));
      return;
    }
    response.end(JSON.stringify({ impressions: 0 }));
  });
  const client = startMcp(backend.address().port, { MCP_EXPERIMENT_ID: '88' });
  try {
    const result = await client.request('tools/call', {
      name: 'consultar_campanhas',
      arguments: {}
    });
    assert.equal(result.result.content.length, 1);
    assert.deepEqual(JSON.parse(result.result.content[0].text).data, { impressions: 0 });
    assert.match(client.stderr(), /recuperar_memoria_just_in_time/);
    assert.match(client.stderr(), /UNAVAILABLE/);
  } finally {
    await client.close();
    backend.close();
  }
});

test('consulta o preflight do run produtivo mais recente sem misturar tentativa anterior', async () => {
  const requests = [];
  const backend = await startBackend(async (request, response) => {
    requests.push(request.url);
    response.setHeader('Content-Type', 'application/json');
    if (request.url.includes('/api/internal/agent-memory/v1/agents/growth-operator')) {
      response.end('[]');
      return;
    }
    if (request.url === '/api/experiments/89/runs') {
      response.end(JSON.stringify([
        { id: 8, runNumber: 1, mode: 'PRODUCTION', status: 'PREFLIGHT_FAILED' },
        { id: 9, runNumber: 2, mode: 'PRODUCTION', status: 'RUNNING' }
      ]));
      return;
    }
    if (request.url === '/api/experiment-runs/9/preflight') {
      response.end(JSON.stringify({
        runId: 9,
        hasBlockers: false,
        gates: [
          { gateCode: 'DIRECT_CHANNEL_READINESS_CONFIRMED', status: 'PASS' },
          { gateCode: 'CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED', status: 'PASS' },
          { gateCode: 'DATA_FRESHNESS_VALID', status: 'PASS' }
        ]
      }));
      return;
    }
    response.statusCode = 404;
    response.end(JSON.stringify({ error: 'rota inesperada' }));
  });
  const client = startMcp(backend.address().port, { MCP_EXPERIMENT_ID: '89' });
  try {
    const result = await client.request('tools/call', {
      name: 'consultar_preflight',
      arguments: {}
    });
    const payload = JSON.parse(result.result.content[0].text);

    assert.equal(payload.data.run.id, 9);
    assert.equal(payload.data.preflight.hasBlockers, false);
    assert.deepEqual(
      payload.data.preflight.gates.map(gate => gate.gateCode),
      [
        'DIRECT_CHANNEL_READINESS_CONFIRMED',
        'CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED',
        'DATA_FRESHNESS_VALID'
      ]
    );
    assert.deepEqual(payload.audit.sources, [
      '/api/experiments/89/runs',
      '/api/experiment-runs/9/preflight'
    ]);
    assert(requests.includes('/api/experiment-runs/9/preflight'));
    assert(!requests.includes('/api/experiment-runs/8/preflight'));
  } finally {
    await client.close();
    backend.close();
  }
});

test('registra candidato no experimento ou na ferramenta sem permitir escopo arbitrario', async () => {
  const bodies = [];
  const backend = await startBackend(async (request, response) => {
    let body = '';
    request.on('data', chunk => { body += chunk; });
    await once(request, 'end');
    if (body) bodies.push(JSON.parse(body));
    response.setHeader('Content-Type', 'application/json');
    response.end(JSON.stringify({ id: bodies.length, status: 'CANDIDATE' }));
  });
  const client = startMcp(backend.address().port, { MCP_EXPERIMENT_ID: '88' });
  const candidate = {
    specialty: 'metrica-comercial',
    content: 'Taxa sem denominador deve permanecer nula.',
    evidence: 'Teste deterministico e execucao segregada confirmaram o contrato.',
    confidence: 0.9
  };
  try {
    await client.request('tools/call', {
      name: 'registrar_aprendizado_candidato',
      arguments: { ...candidate, appliesToTool: 'consultar_cockpit' }
    });
    await client.request('tools/call', {
      name: 'registrar_aprendizado_candidato',
      arguments: candidate
    });
    const rejected = await client.request('tools/call', {
      name: 'registrar_aprendizado_candidato',
      arguments: { ...candidate, appliesToTool: 'executar_comando' }
    });

    assert.equal(bodies[0].scopeType, 'MCP_TOOL');
    assert.equal(bodies[0].scopeId, 'consultar_cockpit');
    assert.equal(bodies[0].sourceExecutionId, 'experiment-88');
    assert.equal(bodies[0].appliesToTool, undefined);
    assert.equal(bodies[1].scopeType, 'EXPERIMENT');
    assert.equal(bodies[1].scopeId, '88');
    assert.match(rejected.error.message, /fora do escopo/);
    assert.equal(bodies.length, 2);
  } finally {
    await client.close();
    backend.close();
  }
});

test('preserva o escopo do plano e a correlacao real da execucao existente', async () => {
  const bodies = [];
  const backend = await startBackend(async (request, response) => {
    let body = '';
    request.on('data', chunk => { body += chunk; });
    await once(request, 'end');
    if (body) bodies.push(JSON.parse(body));
    response.setHeader('Content-Type', 'application/json');
    response.end(JSON.stringify({ id: 1, status: 'CANDIDATE' }));
  });
  const client = startMcp(backend.address().port, {
    MCP_COMMERCIAL_PLAN_ID: '2',
    MCP_SOURCE_EXECUTION_ID: 'growth-operator-execution-31'
  });
  try {
    const listed = await client.request('tools/list', {});
    assert(listed.result.tools.some(tool => tool.name === 'consultar_planejamento'));
    await client.request('tools/call', {
      name: 'registrar_aprendizado_candidato',
      arguments: {
        specialty: 'funil',
        content: 'Dado de uma unica campanha nao define regra global.',
        evidence: 'Resultado posterior oficial do planejamento confirmou o limite.',
        confidence: 0.8
      }
    });
    assert.equal(bodies[0].scopeType, 'COMMERCIAL_PLAN');
    assert.equal(bodies[0].scopeId, '2');
    assert.equal(bodies[0].sourceExecutionId, 'growth-operator-execution-31');
  } finally {
    await client.close();
    backend.close();
  }
});

async function startBackend(handler) {
  const server = http.createServer((request, response) => {
    Promise.resolve(handler(request, response)).catch(error => {
      response.statusCode = 500;
      response.end(JSON.stringify({ error: error.message }));
    });
  });
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  return server;
}

function startMcp(port, scope) {
  const child = spawn(process.execPath, [path.resolve(mcpEntrypoint)], {
    env: {
      ...process.env,
      MCP_MARKETING_HUB_URL: `http://127.0.0.1:${port}`,
      ...scope
    },
    stdio: ['pipe', 'pipe', 'pipe']
  });
  const output = readline.createInterface({ input: child.stdout, crlfDelay: Infinity });
  const waiters = new Map();
  let sequence = 0;
  let stderr = '';
  child.stderr.on('data', chunk => { stderr += chunk.toString(); });
  output.on('line', line => {
    const message = JSON.parse(line);
    const waiter = waiters.get(message.id);
    if (!waiter) return;
    waiters.delete(message.id);
    clearTimeout(waiter.timeout);
    waiter.resolve(message);
  });
  return {
    request(method, params) {
      const id = ++sequence;
      child.stdin.write(`${JSON.stringify({ jsonrpc: '2.0', id, method, params })}\n`);
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          waiters.delete(id);
          reject(new Error(`Timeout aguardando ${method}`));
        }, 5000);
        waiters.set(id, { resolve, timeout });
      });
    },
    stderr: () => stderr,
    async close() {
      child.stdin.end();
      child.kill('SIGTERM');
      output.close();
      await Promise.race([once(child, 'exit'), new Promise(resolve => setTimeout(resolve, 1000))]);
    }
  };
}
