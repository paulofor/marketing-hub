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
  tool('consultar_estrategia_videos', 'Consulta estrategia, custos, progressao e aprendizados dos videos.', {}),
  tool('consultar_pendencias', 'Consulta acoes abertas e resultados comprovados do planejamento.', {}),
  tool('recuperar_memoria_especializada', 'Recupera aprendizados comerciais confirmados e candidatos deste planejamento.', {}, []),
  tool('registrar_aprendizado_candidato', 'Registra uma hipótese comercial sem tratá-la como resultado confirmado.', {
    specialty: { type: 'string', minLength: 3, maxLength: 120 }, content: { type: 'string', minLength: 10, maxLength: 4000 },
    evidence: { type: 'string', minLength: 10, maxLength: 4000 }, sourceReference: { type: 'string', maxLength: 700 },
    confidence: { type: 'number', minimum: 0, maximum: 1 }
  }, ['specialty', 'content', 'evidence', 'confidence']),
  tool('solicitar_pausa_experimento', 'Solicita pausa preventiva governada pelo backend.', actionSchema(), ['reason', 'evidence']),
  tool('solicitar_retomada_experimento', 'Solicita retomada sujeita a aprovacao humana.', actionSchema(), ['reason', 'evidence'])
];

const routes = {
  consultar_planejamento: () => `/api/planning/commercial-plans/${planId}`,
  consultar_funil: () => experimentRoute('funnel'),
  consultar_sessoes: args => `/api/growth-operator/v1/internal/commercial-plans/${planId}/session-intelligence?eventLimit=${boundedLimit(args.eventLimit)}`,
  consultar_campanhas: () => experimentRoute('facebook-campaigns'),
  consultar_memoria: () => `/api/growth-operator/v1/commercial-plans/${planId}/executions`,
  consultar_estrategia_videos: () => `/api/growth-operator/v1/internal/commercial-plans/${planId}/video-strategy-intelligence`,
  consultar_pendencias: () => `/api/growth-operator/v1/commercial-plans/${planId}/tasks`,
  solicitar_pausa_experimento: () => `/api/growth-operator/v1/internal/commercial-plans/${planId}/experiment/pause`,
  solicitar_retomada_experimento: () => `/api/growth-operator/v1/internal/commercial-plans/${planId}/experiment/resume-request`
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
  if (params.name === 'recuperar_memoria_especializada') return callMemory('GET', params.arguments ?? {});
  if (params.name === 'registrar_aprendizado_candidato') return callMemory('POST', params.arguments ?? {});
  const route = routes[params.name];
  if (!route) throw new Error(`Ferramenta nao permitida: ${params.name}`);
  const args = params.arguments ?? {};
  const path = await route(args);
  const mutable = params.name.startsWith('solicitar_');
  const startedAt = new Date().toISOString();
  const response = await fetch(`${baseUrl}${path}`, { method: mutable ? 'POST' : 'GET', headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body: mutable ? JSON.stringify(args) : undefined, signal: AbortSignal.timeout(30000) });
  const body = await response.text();
  process.stderr.write(`${JSON.stringify({ tool: params.name, planId, path, startedAt, status: response.status })}\n`);
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} em ${params.name}`);
  const payload = JSON.parse(body);
  return { content: [{ type: 'text', text: JSON.stringify({ audit: { tool: params.name, planId, source: path, consultedAt: startedAt, readOnly: !mutable, governedMutation: mutable }, data: payload }) }] };
}

async function callMemory(method, args) {
  const root = '/api/internal/agent-memory/v1/agents/growth-operator';
  const path = method === 'GET' ? `${root}?${new URLSearchParams({ scopeType: 'COMMERCIAL_PLAN', scopeId: String(planId), limit: '8' })}` : root;
  const body = method === 'POST' ? JSON.stringify({ ...args, scopeType: 'COMMERCIAL_PLAN', scopeId: String(planId), sourceExecutionId: `plan-${planId}` }) : undefined;
  const response = await fetch(`${baseUrl}${path}`, { method, headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body, signal: AbortSignal.timeout(30000) });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} na memória do Operador`);
  return { content: [{ type: 'text', text: await response.text() }] };
}

function actionSchema() {
  return { reason: { type: 'string', minLength: 10, maxLength: 500 }, evidence: { type: 'array', minItems: 1, maxItems: 10, items: { type: 'string', minLength: 3, maxLength: 300 } } };
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
