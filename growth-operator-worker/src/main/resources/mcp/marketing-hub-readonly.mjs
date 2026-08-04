import readline from 'node:readline';

const baseUrl = requiredEnv('MCP_MARKETING_HUB_URL').replace(/\/$/, '');
const planId = positiveInteger(requiredEnv('MCP_COMMERCIAL_PLAN_ID'), 'MCP_COMMERCIAL_PLAN_ID');

const tools = [
  tool('consultar_planejamento', 'Consulta o planejamento comercial e suas metas atuais.', {}),
  tool('consultar_funil', 'Consulta o funil consolidado do experimento vinculado ao planejamento.', {}),
  tool('consultar_sessoes', 'Consulta jornadas e eventos anonimizados do planejamento.', {
    eventLimit: { type: 'integer', minimum: 1, maximum: 2000, default: 2000 }
  }),
  tool('consultar_campanhas', 'Consulta as campanhas Meta do experimento vinculado ao planejamento.', {}),
  tool('consultar_memoria', 'Consulta o historico auditavel dos ciclos do Operador.', {}),
  tool('consultar_estrategia_videos', 'Consulta estrategia, custos, progressao e aprendizados dos videos.', {})
];

const routes = {
  consultar_planejamento: () => `/api/planning/commercial-plans/${planId}`,
  consultar_funil: () => experimentRoute('funnel'),
  consultar_sessoes: args => `/api/growth-operator/v1/internal/commercial-plans/${planId}/session-intelligence?eventLimit=${boundedLimit(args.eventLimit)}`,
  consultar_campanhas: () => experimentRoute('facebook-campaigns'),
  consultar_memoria: () => `/api/growth-operator/v1/commercial-plans/${planId}/executions`,
  consultar_estrategia_videos: () => `/api/growth-operator/v1/internal/commercial-plans/${planId}/video-strategy-intelligence`
};

const input = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
input.on('line', async line => {
  if (!line.trim()) return;
  let request;
  try {
    request = JSON.parse(line);
    const result = await dispatch(request);
    if (request.id !== undefined) send({ jsonrpc: '2.0', id: request.id, result });
  } catch (error) {
    if (request?.id !== undefined) {
      send({ jsonrpc: '2.0', id: request.id, error: { code: -32000, message: safeMessage(error) } });
    }
  }
});

async function dispatch(request) {
  if (request.method === 'initialize') {
    return { protocolVersion: request.params?.protocolVersion ?? '2025-03-26', capabilities: { tools: {} }, serverInfo: { name: 'marketing-hub-readonly', version: '1.0.0' } };
  }
  if (request.method === 'ping') return {};
  if (request.method === 'tools/list') return { tools };
  if (request.method === 'tools/call') return callTool(request.params ?? {});
  if (request.method?.startsWith('notifications/')) return {};
  throw new Error(`Metodo MCP nao permitido: ${request.method}`);
}

async function callTool(params) {
  const route = routes[params.name];
  if (!route) throw new Error(`Ferramenta nao permitida: ${params.name}`);
  const path = await route(params.arguments ?? {});
  const startedAt = new Date().toISOString();
  const response = await fetch(`${baseUrl}${path}`, { method: 'GET', headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(30000) });
  const body = await response.text();
  process.stderr.write(`${JSON.stringify({ tool: params.name, planId, path, startedAt, status: response.status })}\n`);
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} em ${params.name}`);
  const payload = JSON.parse(body);
  return { content: [{ type: 'text', text: JSON.stringify({ audit: { tool: params.name, planId, source: path, consultedAt: startedAt, readOnly: true }, data: payload }) }] };
}

async function experimentRoute(suffix) {
  const response = await fetch(`${baseUrl}/api/planning/commercial-plans/${planId}`, {
    method: 'GET', headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) throw new Error(`Nao foi possivel resolver o experimento do planejamento: HTTP ${response.status}`);
  const plan = await response.json();
  const experimentId = positiveInteger(plan.experimentId, 'experimentId do planejamento');
  return `/api/experiments/${experimentId}/${suffix}`;
}

function tool(name, description, properties, required = []) {
  return { name, description, inputSchema: { type: 'object', additionalProperties: false, properties, required } };
}

function boundedLimit(value) {
  if (value === undefined) return 2000;
  return Math.min(2000, positiveInteger(value, 'eventLimit'));
}

function positiveInteger(value, name) {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error(`${name} deve ser inteiro positivo`);
  return parsed;
}

function requiredEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`Variavel obrigatoria ausente: ${name}`);
  return value;
}

function safeMessage(error) {
  return error instanceof Error ? error.message : 'Falha MCP nao identificada';
}

function send(value) {
  process.stdout.write(`${JSON.stringify(value)}\n`);
}
