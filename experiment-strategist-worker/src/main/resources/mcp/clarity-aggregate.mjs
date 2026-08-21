import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import readline from 'node:readline';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

const ALLOWED_DIMENSIONS = new Set(['PAGE', 'SOURCE', 'DEVICE']);
const FORBIDDEN_GRANULAR_FIELDS = /(?:session[_ -]?id|visitor[_ -]?id|user[_ -]?id|recording(?:url|link|id)?|individual[_ -]?timeline)/i;
const MAX_RESPONSE_BYTES = 500_000;

export function createClarityAggregateServer(environment = process.env) {
  const base = required(environment, 'MCP_BACKEND_URL').replace(/\/$/, '');
  const executionId = required(environment, 'MCP_EXECUTION_ID');
  const clarityToken = readClarityToken(environment);
  const clarityEntrypoint = environment.CLARITY_MCP_ENTRYPOINT
    || '/app/node_modules/@microsoft/clarity-mcp-server/dist/index.js';
  let clarityClient;

  const tool = {
    name: 'consultar_snapshot_comportamental_agregado',
    description: 'Consulta um snapshot agregado do Microsoft Clarity, segregado pelo experimento e sem gravações ou identificadores individuais.',
    inputSchema: {
      type: 'object',
      additionalProperties: false,
      required: ['experimentId', 'dimension', 'windowDays'],
      properties: {
        experimentId: { type: 'integer', minimum: 1 },
        dimension: { type: 'string', enum: ['PAGE', 'SOURCE', 'DEVICE'] },
        windowDays: { type: 'integer', minimum: 1, maximum: 3 }
      }
    }
  };

  async function dispatch(request) {
    if (request.method === 'initialize') {
      return {
        protocolVersion: request.params?.protocolVersion ?? '2025-03-26',
        capabilities: { tools: {} },
        serverInfo: { name: 'marketing-hub-clarity-aggregate', version: '1.0.0' }
      };
    }
    if (request.method === 'tools/list') return { tools: [tool] };
    if (request.method === 'ping' || request.method?.startsWith('notifications/')) return {};
    if (request.method !== 'tools/call'
        || request.params?.name !== 'consultar_snapshot_comportamental_agregado') {
      throw new Error('Ferramenta não permitida');
    }
    const args = validateArguments(request.params.arguments ?? {});
    const reservation = await backendCall(
      base,
      `/api/experiment-strategist/v1/internal/executions/${encodeURIComponent(executionId)}/behavioral-snapshots/reserve`,
      'POST',
      args
    );
    try {
      clarityClient ??= createOfficialClarityClient(clarityEntrypoint, clarityToken);
      audit('clarity_aggregate_request_sent', {
        executionId,
        snapshotId: reservation.id,
        providerUrl: 'mcp://microsoft-clarity/query-analytics-data',
        dimension: args.dimension,
        windowDays: args.windowDays,
        query: reservation.queryText
      });
      const officialResult = await clarityClient.query(reservation.queryText);
      const rawResponse = JSON.stringify(officialResult);
      validateAggregateResponse(rawResponse);
      const aggregateData = normalizeOfficialResult(officialResult);
      await backendCall(
        base,
        `/api/experiment-strategist/v1/internal/executions/${encodeURIComponent(executionId)}/behavioral-snapshots/${encodeURIComponent(reservation.id)}/complete`,
        'POST',
        { rawResponse }
      );
      audit('clarity_aggregate_response_received', {
        executionId,
        snapshotId: reservation.id,
        providerUrl: 'mcp://microsoft-clarity/query-analytics-data',
        dimension: args.dimension,
        windowDays: args.windowDays,
        query: reservation.queryText,
        response: officialResult
      });
      return {
        content: [{
          type: 'text',
          text: JSON.stringify({
            snapshotId: reservation.id,
            provider: 'MICROSOFT_CLARITY_MCP',
            dimension: args.dimension,
            windowDays: args.windowDays,
            aggregateOnly: true,
            providerCostUsd: 0,
            data: aggregateData
          })
        }]
      };
    } catch (error) {
      await reportFailure(base, executionId, reservation.id, error);
      audit('clarity_aggregate_request_failed', {
        executionId,
        snapshotId: reservation.id,
        providerUrl: 'mcp://microsoft-clarity/query-analytics-data',
        query: reservation.queryText,
        error: safeError(error)
      });
      throw error;
    }
  }

  async function close() {
    if (clarityClient) await clarityClient.close();
  }

  return { dispatch, close };
}

export function validateArguments(args) {
  const experimentId = Number(args.experimentId);
  const dimension = String(args.dimension ?? '').toUpperCase();
  const windowDays = Number(args.windowDays);
  if (!Number.isInteger(experimentId) || experimentId < 1) {
    throw new Error('Experimento obrigatório para segregar o snapshot do Clarity');
  }
  if (!ALLOWED_DIMENSIONS.has(dimension)) throw new Error('Dimensão agregada inválida');
  if (!Number.isInteger(windowDays) || windowDays < 1 || windowDays > 3) {
    throw new Error('A janela do Clarity deve ter de 1 a 3 dias');
  }
  return { experimentId, dimension, windowDays };
}

export function validateAggregateResponse(rawResponse) {
  if (Buffer.byteLength(rawResponse, 'utf8') > MAX_RESPONSE_BYTES) {
    throw new Error('Resposta agregada do Clarity excedeu o limite seguro');
  }
  if (FORBIDDEN_GRANULAR_FIELDS.test(rawResponse)) {
    throw new Error('Clarity devolveu campo individual proibido; snapshot bloqueado');
  }
}

export function normalizeOfficialResult(result) {
  const textBlocks = Array.isArray(result?.content)
    ? result.content.filter(item => item?.type === 'text' && typeof item.text === 'string')
    : [];
  if (textBlocks.length !== 1) return result;
  try {
    return JSON.parse(textBlocks[0].text);
  } catch (error) {
    return { text: textBlocks[0].text };
  }
}

export function createOfficialClarityClient(entrypoint, token) {
  const child = spawn(process.execPath, [entrypoint], {
    env: { ...process.env, CLARITY_API_TOKEN: token },
    stdio: ['pipe', 'pipe', 'pipe']
  });
  const pending = new Map();
  let sequence = 0;
  const output = readline.createInterface({ input: child.stdout, crlfDelay: Infinity });
  output.on('line', line => {
    let response;
    try {
      response = JSON.parse(line);
    } catch (error) {
      rejectAll(pending, new Error('MCP oficial do Clarity devolveu JSON inválido'));
      return;
    }
    const waiter = pending.get(response.id);
    if (!waiter) return;
    pending.delete(response.id);
    if (response.error) waiter.reject(new Error(response.error.message || 'Falha no MCP oficial do Clarity'));
    else waiter.resolve(response.result);
  });
  child.stderr.on('data', data => {
    const line = data.toString('utf8').trim();
    if (line) audit('clarity_official_mcp_log', { message: line.slice(0, 1000) });
  });
  child.on('exit', code => rejectAll(pending, new Error(`MCP oficial do Clarity encerrou com código ${code}`)));

  function call(method, params) {
    const id = ++sequence;
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        pending.delete(id);
        reject(new Error('Timeout do MCP oficial do Clarity'));
      }, 30_000);
      pending.set(id, {
        resolve: value => { clearTimeout(timeout); resolve(value); },
        reject: error => { clearTimeout(timeout); reject(error); }
      });
      child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
    });
  }

  const initialized = call('initialize', {
    protocolVersion: '2025-03-26',
    capabilities: {},
    clientInfo: { name: 'marketing-hub-clarity-aggregate', version: '1.0.0' }
  }).then(() => {
    child.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');
  });

  return {
    async query(query) {
      await initialized;
      return call('tools/call', { name: 'query-analytics-data', arguments: { query } });
    },
    async close() {
      child.stdin.end();
      if (!child.killed) child.kill('SIGTERM');
    }
  };
}

async function backendCall(base, path, method, body) {
  const response = await fetch(base + path, {
    method,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(30_000)
  });
  if (!response.ok) throw new Error(`Backend respondeu HTTP ${response.status}`);
  return response.json();
}

async function reportFailure(base, executionId, snapshotId, error) {
  try {
    await backendCall(
      base,
      `/api/experiment-strategist/v1/internal/executions/${encodeURIComponent(executionId)}/behavioral-snapshots/${encodeURIComponent(snapshotId)}/fail`,
      'POST',
      { errorMessage: safeError(error) }
    );
  } catch (reportError) {
    audit('clarity_aggregate_failure_report_failed', {
      executionId,
      snapshotId,
      error: safeError(reportError)
    });
  }
}

function rejectAll(pending, error) {
  for (const waiter of pending.values()) waiter.reject(error);
  pending.clear();
}

function required(environment, name) {
  const value = environment[name];
  if (!value || !String(value).trim()) throw new Error(`Variável obrigatória ausente: ${name}`);
  return String(value).trim();
}

function readClarityToken(environment) {
  if (environment.CLARITY_API_TOKEN_FILE) {
    const value = readFileSync(String(environment.CLARITY_API_TOKEN_FILE), 'utf8').trim();
    if (value) return value;
  }
  return required(environment, 'CLARITY_API_TOKEN');
}

function safeError(error) {
  return String(error?.message ?? error ?? 'Falha desconhecida').slice(0, 1000);
}

function audit(event, details) {
  process.stderr.write(JSON.stringify({ event, at: new Date().toISOString(), ...details }) + '\n');
}

async function runStdio() {
  const implementation = createClarityAggregateServer();
  const server = new McpServer({
    name: 'marketing-hub-clarity-aggregate',
    version: '1.0.0'
  });
  server.registerTool(
    'consultar_snapshot_comportamental_agregado',
    {
      description: 'Consulta um snapshot agregado do Microsoft Clarity, segregado pelo experimento e sem gravações ou identificadores individuais.',
      inputSchema: {
        experimentId: z.number().int().positive(),
        dimension: z.enum(['PAGE', 'SOURCE', 'DEVICE']),
        windowDays: z.number().int().min(1).max(3)
      }
    },
    async args => implementation.dispatch({
      method: 'tools/call',
      params: {
        name: 'consultar_snapshot_comportamental_agregado',
        arguments: args
      }
    })
  );
  process.stdin.once('end', () => implementation.close());
  await server.connect(new StdioServerTransport());
}

if (process.argv[1] && new URL(import.meta.url).pathname === process.argv[1]) {
  runStdio().catch(error => {
    audit('clarity_aggregate_fatal', { error: safeError(error) });
    process.exitCode = 1;
  });
}
