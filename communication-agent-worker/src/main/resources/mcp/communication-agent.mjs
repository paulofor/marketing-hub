import readline from "node:readline";

const baseUrl = requiredEnv("MCP_MARKETING_HUB_URL").replace(/\/$/, "");
const taskId = positiveInteger(requiredEnv("MCP_TASK_ID"), "MCP_TASK_ID");
const sourceReference = requiredEnv("MCP_SOURCE_REFERENCE");
const scope = sourceScope(sourceReference);
const tools = [
  tool("consultar_contexto_tarefa", "Consulta novamente o snapshot imutável da tarefa reservada.", {}, true),
  tool("recuperar_memoria_especializada", "Recupera aprendizados de comunicação confirmados ou candidatos no mesmo escopo.", {}, true),
  tool(
    "registrar_aprendizado_candidato",
    "Registra hipótese de comunicação sem confirmá-la, promovê-la ou tratá-la como resultado.",
    {
      specialty: { type: "string", minLength: 3, maxLength: 120 },
      content: { type: "string", minLength: 10, maxLength: 4000 },
      evidence: { type: "string", minLength: 10, maxLength: 4000 },
      sourceReference: { type: "string", maxLength: 700 },
      confidence: { type: "number", minimum: 0, maximum: 1 }
    },
    false,
    ["specialty", "content", "evidence", "confidence"]
  )
];

const input = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
input.on("line", async (line) => {
  if (!line.trim()) return;
  let request;
  try {
    request = JSON.parse(line);
    const result = await dispatch(request);
    if (request.id !== undefined) send({ jsonrpc: "2.0", id: request.id, result });
  } catch (error) {
    if (request?.id !== undefined) {
      send({ jsonrpc: "2.0", id: request.id, error: { code: -32000, message: safeMessage(error) } });
    }
  }
});

async function dispatch(request) {
  if (request.method === "initialize") {
    return { protocolVersion: request.params?.protocolVersion ?? "2025-03-26", capabilities: { tools: {} }, serverInfo: { name: "iris-communication", version: "1.0.0" } };
  }
  if (request.method === "ping") return {};
  if (request.method === "tools/list") return { tools };
  if (request.method === "tools/call") return callTool(request.params ?? {});
  if (request.method?.startsWith("notifications/")) return {};
  throw new Error(`Método MCP não permitido: ${request.method}`);
}

async function callTool(params) {
  const args = params.arguments ?? {};
  if (params.name === "consultar_contexto_tarefa") {
    return request(
      "GET",
      `/api/internal/agent-tasks/communication-director/stage-executions/${taskId}`,
      undefined,
      params.name
    );
  }
  const memoryRoot = "/api/internal/agent-memory/v1/agents/communication-director";
  if (params.name === "recuperar_memoria_especializada") {
    const query = new URLSearchParams({ scopeType: scope.type, scopeId: scope.id, limit: "8" });
    return request("GET", `${memoryRoot}?${query}`, undefined, params.name);
  }
  if (params.name === "registrar_aprendizado_candidato") {
    return request(
      "POST",
      memoryRoot,
      {
        ...args,
        scopeType: scope.type,
        scopeId: scope.id,
        sourceExecutionId: `agent-task-${taskId}`
      },
      params.name
    );
  }
  throw new Error(`Ferramenta não permitida: ${params.name}`);
}

async function request(method, path, body, toolName) {
  const startedAt = new Date().toISOString();
  process.stderr.write(`${JSON.stringify({ tool: toolName, taskId, sourceReference, path, startedAt })}\n`);
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(30000)
  });
  const text = await response.text();
  process.stderr.write(`${JSON.stringify({ tool: toolName, taskId, path, status: response.status, finishedAt: new Date().toISOString() })}\n`);
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} em ${toolName}`);
  return { content: [{ type: "text", text }] };
}

function sourceScope(reference) {
  const plan =
    /^commercial-plan:([1-9][0-9]*)(?:@v[1-9][0-9]*)?(?::[A-Za-z0-9_-]+)*$/.exec(reference);
  if (plan) return { type: "COMMERCIAL_PLAN", id: plan[1] };
  const experiment = /^experiment:([1-9][0-9]*)$/.exec(reference);
  if (experiment) return { type: "EXPERIMENT", id: experiment[1] };
  throw new Error("Íris exige sourceReference de plano ou experimento.");
}

function tool(name, description, properties, readOnly, required = []) {
  return {
    name,
    description,
    inputSchema: { type: "object", additionalProperties: false, properties, required },
    annotations: { readOnlyHint: readOnly, destructiveHint: false, openWorldHint: false }
  };
}

function positiveInteger(value, name) {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error(`${name} deve ser inteiro positivo`);
  return parsed;
}

function requiredEnv(name) {
  const value = process.env[name];
  if (!value?.trim()) throw new Error(`Variável obrigatória ausente: ${name}`);
  return value.trim();
}

function safeMessage(error) {
  return error instanceof Error ? error.message : "Falha MCP não identificada";
}

function send(value) {
  process.stdout.write(`${JSON.stringify(value)}\n`);
}
