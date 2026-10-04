import readline from "node:readline";
import { pathToFileURL } from "node:url";

// Publica apenas capacidades limitadas; a configuração do executor não vem dos argumentos do modelo.
export function createStrategistServer(
  environment = process.env,
  dependencies = {},
) {
  const base = required(environment, "MCP_BACKEND_URL").replace(/\/$/, "");
  const executionId = required(environment, "MCP_EXECUTION_ID");
  const readPages =
    dependencies.readPages ??
    (async (urls) => {
      const browser = await import("/app/browser/public-research.mjs");
      return browser.readPublicPages(urls);
    });
  const tools = [
    {
      name: "consultar_evidencias_estrategicas",
      description: "Consulta as evidências congeladas da execução estratégica.",
      annotations: {
        readOnlyHint: true,
        destructiveHint: false,
        openWorldHint: false,
      },
      inputSchema: {
        type: "object",
        additionalProperties: false,
        properties: {},
      },
    },
    {
      name: "consultar_paginas_publicas",
      description:
        "Confirma até três URLs HTTP(S) públicas diretas no Chromium mobile, sem seguir redirecionamentos. Retorna URL final, data, status, texto e hash. Não faz login, não preenche formulários e não executa shell. Falhas são lacunas técnicas, não evidência comercial.",
      annotations: {
        readOnlyHint: true,
        destructiveHint: false,
        openWorldHint: true,
      },
      inputSchema: {
        type: "object",
        additionalProperties: false,
        required: ["urls"],
        properties: {
          urls: {
            type: "array",
            minItems: 1,
            maxItems: 3,
            items: { type: "string", maxLength: 2048 },
          },
        },
      },
    },
    memoryReadTool(),
    memoryWriteTool(),
  ];
  async function dispatch(request) {
    if (request.method === "initialize")
      return {
        protocolVersion: request.params?.protocolVersion ?? "2025-03-26",
        capabilities: { tools: {} },
        serverInfo: { name: "experiment-strategist", version: "1.2.0" },
      };
    if (request.method === "tools/list") return { tools };
    if (
      request.method === "ping" ||
      request.method?.startsWith("notifications/")
    )
      return {};
    if (request.method !== "tools/call")
      throw new Error("Ferramenta não permitida");
    const name = request.params?.name;
    const args = request.params.arguments ?? {};
    if (name === "consultar_paginas_publicas") {
      if (
        Object.keys(args).some((key) => key !== "urls") ||
        !Array.isArray(args.urls) ||
        args.urls.length < 1 ||
        args.urls.length > 3 ||
        args.urls.some((url) => typeof url !== "string" || url.length > 2048)
      )
        throw new Error("Informe somente uma a três URLs públicas.");
      for (const value of args.urls) {
        let url;
        try {
          url = new URL(value);
        } catch {
          throw new Error("URL pública inválida.");
        }
        if (
          !["http:", "https:"].includes(url.protocol) ||
          url.username ||
          url.password
        )
          throw new Error("URL HTTP(S) sem credenciais obrigatória.");
      }
      audit("public_research_started", { executionId, urls: args.urls });
      try {
        const result = { executionId, ...(await readPages(args.urls)) };
        audit("public_research_completed", result);
        return content(result);
      } catch (error) {
        audit("public_research_failed", { executionId, error: error.message });
        throw error;
      }
    }
    if (name === "recuperar_memoria_especializada")
      return callMemory("GET", args);
    if (name === "registrar_aprendizado_candidato")
      return callMemory("POST", args);
    if (name !== "consultar_evidencias_estrategicas")
      throw new Error("Ferramenta não permitida");
    return backend(
      `/api/experiment-strategist/v1/internal/executions/${encodeURIComponent(executionId)}`,
    );
  }
  async function backend(path, method = "GET", body) {
    const response = await fetch(base + path, {
      method,
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(30000),
    });
    audit("strategist_backend", { executionId, path, status: response.status });
    if (!response.ok)
      throw new Error(`Backend respondeu HTTP ${response.status}`);
    return { content: [{ type: "text", text: await response.text() }] };
  }
  function callMemory(method, args) {
    const root = "/api/internal/agent-memory/v1/agents/experiment-strategist";
    const path =
      method === "GET"
        ? `${root}?${new URLSearchParams({ ...(args.tenantKey ? { tenantKey: args.tenantKey } : {}), scopeType: args.scopeType, scopeId: args.scopeId, limit: String(args.limit ?? 8) })}`
        : root;
    return backend(
      path,
      method,
      method === "POST"
        ? { ...args, sourceExecutionId: String(executionId) }
        : undefined,
    );
  }
  return { dispatch };
}
function memoryReadTool() {
  return {
    name: "recuperar_memoria_especializada",
    description: "Recupera aprendizados estratégicos deste escopo.",
    annotations: {
      readOnlyHint: true,
      destructiveHint: false,
      openWorldHint: false,
    },
    inputSchema: {
      type: "object",
      additionalProperties: false,
      required: ["scopeType", "scopeId"],
      properties: {
        tenantKey: { type: "string", maxLength: 120 },
        scopeType: { type: "string", maxLength: 60 },
        scopeId: { type: "string", maxLength: 120 },
        limit: { type: "integer", minimum: 1, maximum: 12 },
      },
    },
  };
}
function memoryWriteTool() {
  return {
    name: "registrar_aprendizado_candidato",
    description:
      "Registra hipótese estratégica candidata, sujeita a resultado posterior.",
    annotations: {
      readOnlyHint: false,
      destructiveHint: false,
      openWorldHint: false,
    },
    inputSchema: {
      type: "object",
      additionalProperties: false,
      required: [
        "scopeType",
        "scopeId",
        "specialty",
        "content",
        "evidence",
        "confidence",
      ],
      properties: {
        tenantKey: { type: "string", maxLength: 120 },
        scopeType: { type: "string", maxLength: 60 },
        scopeId: { type: "string", maxLength: 120 },
        specialty: { type: "string", maxLength: 120 },
        content: { type: "string", maxLength: 4000 },
        evidence: { type: "string", maxLength: 4000 },
        sourceReference: { type: "string", maxLength: 700 },
        confidence: { type: "number", minimum: 0, maximum: 1 },
      },
    },
  };
}
function content(value) {
  return { content: [{ type: "text", text: JSON.stringify(value) }] };
}
function required(environment, name) {
  const value = environment[name];
  if (!value) throw new Error(`Variável obrigatória ausente: ${name}`);
  return value;
}
function audit(event, details) {
  process.stderr.write(
    JSON.stringify({ event, ...details, at: new Date().toISOString() }) + "\n",
  );
}
if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const server = createStrategistServer();
  const input = readline.createInterface({
    input: process.stdin,
    crlfDelay: Infinity,
  });
  input.on("line", async (line) => {
    let request;
    try {
      request = JSON.parse(line);
      const result = await server.dispatch(request);
      if (request.id !== undefined)
        process.stdout.write(
          JSON.stringify({ jsonrpc: "2.0", id: request.id, result }) + "\n",
        );
    } catch (error) {
      if (request?.id !== undefined)
        process.stdout.write(
          JSON.stringify({
            jsonrpc: "2.0",
            id: request.id,
            error: { code: -32000, message: error.message },
          }) + "\n",
        );
    }
  });
}
