import { mkdir, readFile, writeFile } from "node:fs/promises";
import {
  resolveOpportunityAgentTimeoutMs,
  resolveOpportunityModel,
} from "./agent-configuration.mjs";
import { validateFunctionalResult } from "./contract.mjs";

const providerEndpoint = "https://api.openai.com/v1/responses";

const role = String(process.env.AGENT_ROLE || "").trim().toLowerCase();
if (!new Set(["argos", "hermes", "dedalo", "psique"]).has(role)) {
  throw new Error("AGENT_ROLE deve ser argos, hermes, dedalo ou psique.");
}
if (!process.env.OPENAI_API_KEY) {
  throw new Error("OPENAI_API_KEY não configurada.");
}

const correlationId = sanitizeIdentifier(process.env.RUN_ID || "LOCAL_QA_PDE_OPPORTUNITY");
const auditDirectory = String(process.env.AUDIT_DIR || "").trim();

try {
  const input = await readStandardInput();
  const promptTemplate = await readFile(new URL(`./prompts/${role}.md`, import.meta.url), "utf8");
  const schema = JSON.parse(
    await readFile(new URL(`./schemas/${role}.json`, import.meta.url), "utf8"),
  );
  const model = resolveOpportunityModel();
  const timeoutMs = resolveOpportunityAgentTimeoutMs();
  const effort = process.env.REASONING_EFFORT || (role === "argos" ? "medium" : "high");
  const request = {
    model,
    service_tier: "flex",
    store: false,
    reasoning: { effort },
    input: promptTemplate.replace("{{INPUT_JSON}}", JSON.stringify(input, null, 2)),
    text: {
      format: {
        type: "json_schema",
        name: `pde_opportunity_${role}`,
        strict: true,
        schema,
      },
    },
  };

  await persistAudit("provider", { endpoint: providerEndpoint, timeoutMs });
  await persistAudit("request", request);
  const response = await executeFlex(request, timeoutMs);
  await persistAudit("response", response);
  const outputText = response.output
    ?.flatMap((item) => item.content || [])
    .find((item) => item.type === "output_text")?.text;
  if (!outputText) throw new Error(`Resposta de ${role} sem output_text.`);
  const result = JSON.parse(outputText);
  validateFunctionalResult(role, input, result);

  const envelope = {
    agent: role,
    correlationId,
    model: response.model,
    serviceTier: response.service_tier,
    status: response.status,
    responseId: response.id,
    usage: response.usage,
    result,
  };
  await persistAudit("result", envelope);
  process.stdout.write(JSON.stringify(envelope));
} catch (error) {
  await persistAudit("error", {
    agent: role,
    correlationId,
    message: error instanceof Error ? error.message : String(error),
    stack: error instanceof Error ? error.stack : null,
  });
  throw error;
}

/** Lê o contexto integral sem misturar payload funcional com logs. */
async function readStandardInput() {
  let value = "";
  for await (const chunk of process.stdin) value += chunk;
  if (!value.trim()) throw new Error("Contexto da atividade ausente.");
  return JSON.parse(value);
}

/** Usa Flex por padrão e repete somente indisponibilidade transitória do provedor. */
async function executeFlex(body, timeoutMs) {
  const retriable = new Set([408, 409, 429, 500, 502, 503, 504]);
  const deadline = Date.now() + timeoutMs;
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    const remainingMs = deadline - Date.now();
    if (remainingMs <= 0) {
      throw new Error(`OpenAI excedeu o timeout total de ${timeoutMs} ms.`);
    }
    let response;
    try {
      response = await fetch(providerEndpoint, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(Math.min(150_000, remainingMs)),
      });
    } catch (error) {
      await persistAudit(`provider-error-attempt-${attempt}`, {
        endpoint: providerEndpoint,
        errorName: error instanceof Error ? error.name : "Error",
        message: error instanceof Error ? error.message : String(error),
      });
      if (attempt === 4 || Date.now() >= deadline) {
        throw new Error(
          `OpenAI indisponível ou excedeu o timeout total de ${timeoutMs} ms.`,
          { cause: error },
        );
      }
      await new Promise((resolve) => setTimeout(resolve, attempt * 3000));
      continue;
    }
    const payload = await response.json();
    if (response.ok) return payload;
    await persistAudit(`provider-error-attempt-${attempt}`, {
      endpoint: providerEndpoint,
      httpStatus: response.status,
      payload,
    });
    if (!retriable.has(response.status) || attempt === 4) {
      throw new Error(
        `OpenAI HTTP ${response.status}: ${payload.error?.type || "erro"} — ${payload.error?.message || "sem mensagem"}`,
      );
    }
    await new Promise((resolve) => setTimeout(resolve, attempt * 3000));
  }
  throw new Error("Flex indisponível após quatro tentativas.");
}

/** Persiste request, resposta bruta, resultado ou erro quando há diretório de auditoria. */
async function persistAudit(kind, payload) {
  if (!auditDirectory) return;
  await mkdir(auditDirectory, { recursive: true });
  await writeFile(
    `${auditDirectory}/${role}-${kind}.json`,
    `${JSON.stringify(payload, null, 2)}\n`,
    "utf8",
  );
}

function sanitizeIdentifier(value) {
  return String(value).replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 96);
}
