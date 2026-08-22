import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

/** Cria com Codex um plano de investigação; nenhuma credencial de marketplace entra no prompt. */
export async function planDirectedResearch(job, options = {}) {
  const enabled = String(options.enabled ?? process.env.ARGOS_CODEX_ENABLED) === "true";
  if (!enabled) return deterministicPlan(job);
  const directory = await mkdtemp(join(tmpdir(), "argos-plan-"));
  const output = join(directory, "output.json");
  const schema = join(directory, "schema.json");
  try {
    await writeFile(schema, JSON.stringify(RESEARCH_PLAN_SCHEMA));
    const command = options.command || process.env.ARGOS_CODEX_COMMAND || "codex";
    const args = [
      "exec",
      "-",
      "--skip-git-repo-check",
      "--sandbox",
      "read-only",
      "--output-schema",
      schema,
      "--output-last-message",
      output,
      "--json",
      "--color",
      "never",
    ];
    const model = options.model || process.env.ARGOS_CODEX_MODEL;
    if (model) args.push("--model", model);
    const execute = options.execute || executeCodexWithInput;
    const execution = await execute(command, args, buildPrompt(job), {
      timeoutMs: Number(options.timeoutMs || process.env.ARGOS_CODEX_TIMEOUT_MS || 600000),
      maxBuffer: 10 * 1024 * 1024,
    });
    let rawResponse;
    try {
      rawResponse = await readFile(output, "utf8");
    } catch (error) {
      if (error?.code === "ENOENT") {
        throw new Error("Codex terminou sem produzir o plano estruturado de Argos", {
          cause: error,
        });
      }
      throw error;
    }
    const plan = JSON.parse(rawResponse);
    validatePlan(plan);
    return {
      plan,
      rawResponse,
      model: model || "codex-default",
      mode: "CODEX",
      usage: parseCodexUsage(execution?.stdout),
    };
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

/** Executa o Codex enviando e encerrando explicitamente a entrada padrão. */
export function executeCodexWithInput(command, args, input, options = {}) {
  const spawnProcess = options.spawnProcess || spawn;
  const timeoutMs = Number(options.timeoutMs || 600000);
  const maxBuffer = Number(options.maxBuffer || 10 * 1024 * 1024);
  return new Promise((resolve, reject) => {
    let child;
    let settled = false;
    let timeout;
    let stdout = "";
    let stderr = "";

    const rejectOnce = (error) => {
      if (settled) return;
      settled = true;
      if (timeout) clearTimeout(timeout);
      reject(error);
    };
    const appendOutput = (current, chunk) => {
      const next = current + String(chunk);
      if (next.length > maxBuffer) {
        child?.kill("SIGTERM");
        rejectOnce(new Error("Saída do planejamento de Argos excedeu o limite seguro"));
      }
      return next;
    };

    try {
      child = spawnProcess(command, args, { stdio: ["pipe", "pipe", "pipe"] });
    } catch (error) {
      rejectOnce(new Error(`Falha ao iniciar o Codex para Argos: ${error.message}`, { cause: error }));
      return;
    }

    timeout = setTimeout(() => {
      child.kill("SIGTERM");
      rejectOnce(new Error(`Planejamento de Argos excedeu o timeout de ${timeoutMs} ms`));
    }, timeoutMs);

    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk) => {
      stdout = appendOutput(stdout, chunk);
    });
    child.stderr.on("data", (chunk) => {
      stderr = appendOutput(stderr, chunk);
    });
    child.on("error", (error) => {
      rejectOnce(new Error(`Falha ao executar o Codex para Argos: ${error.message}`, { cause: error }));
    });
    child.on("close", (code, signal) => {
      if (settled) return;
      if (code !== 0) {
        const detail = stderr.trim().slice(-2000);
        rejectOnce(
          new Error(
            `Codex encerrou o planejamento de Argos com código ${code ?? "desconhecido"}${signal ? ` e sinal ${signal}` : ""}${detail ? `: ${detail}` : ""}`,
          ),
        );
        return;
      }
      settled = true;
      clearTimeout(timeout);
      resolve({ stdout, stderr });
    });
    child.stdin.on("error", (error) => {
      if (error?.code !== "EPIPE") {
        rejectOnce(
          new Error(`Falha ao enviar o contexto de Argos ao Codex: ${error.message}`, {
            cause: error,
          }),
        );
      }
    });
    child.stdin.end(input, "utf8");
  });
}

/** Extrai a contabilização final emitida pelo modo JSON do Codex. */
export function parseCodexUsage(stdout) {
  let usage;
  for (const line of String(stdout || "").split("\n")) {
    if (!line.trim()) continue;
    try {
      const event = JSON.parse(line);
      if (event.type === "turn.completed" && event.usage) usage = event.usage;
    } catch {
      // Linhas operacionais não estruturadas não substituem a resposta funcional.
    }
  }
  if (!usage) return null;
  return {
    inputTokens: Number(usage.input_tokens || 0),
    cachedInputTokens: Number(usage.cached_input_tokens || 0),
    outputTokens: Number(usage.output_tokens || 0),
  };
}

/** Produz um plano seguro quando o piloto Codex está desligado ou ainda sem sessão. */
export function deterministicPlan(job) {
  const theme = [job.theme, job.targetAudience].filter(Boolean).join(" ");
  const plan = {
    questions: [
      `Quais produtos pagos resolvem ${theme}?`,
      `Quais promessas, preços e mecanismos se repetem em ${theme}?`,
      `Quais reclamações revelam uma lacuna explorável em ${theme}?`,
    ],
    publicQueries: [
      `${theme} preço review reclamação`,
      `${theme} curso produto digital vale a pena`,
      `${theme} anúncio oferta depoimento`,
    ],
    marketplaceRequests: [
      { marketplace: "HOTMART", query: theme, maxProducts: 10 },
      { marketplace: "CLICKBANK", query: theme, maxProducts: 10 },
    ],
    metaAdRequests: [{ query: theme, country: "BR", maxAds: 25 }],
    minimumComparableOffers: 10,
    stopConditions: [
      "menos de duas fontes independentes",
      "ausência de sinal de compra",
      "credencial ou marketplace indisponível",
    ],
  };
  return {
    plan,
    rawResponse: JSON.stringify(plan),
    model: "deterministic-fallback-v1",
    mode: "DETERMINISTIC",
  };
}

/** Valida limites que impedem o agente de ampliar coleta ou inventar fontes. */
export function validatePlan(plan) {
  if (
    !Array.isArray(plan.questions) ||
    plan.questions.length < 3 ||
    !Array.isArray(plan.publicQueries) ||
    plan.publicQueries.length < 3 ||
    !Array.isArray(plan.marketplaceRequests) ||
    plan.marketplaceRequests.length === 0 ||
    !Array.isArray(plan.metaAdRequests) ||
    plan.metaAdRequests.length === 0 ||
    plan.minimumComparableOffers < 10
  ) {
    throw new Error("Plano dirigido de Argos fora do contrato v1");
  }
  for (const request of plan.marketplaceRequests) {
    if (!["HOTMART", "CLICKBANK"].includes(request.marketplace)) {
      throw new Error("Marketplace não autorizado no plano dirigido");
    }
    if (!request.query || request.maxProducts < 1 || request.maxProducts > 25) {
      throw new Error("Limite inválido de pesquisa dirigida no marketplace");
    }
  }
  for (const request of plan.metaAdRequests) {
    if (!request.query || !request.country || request.maxAds < 1 || request.maxAds > 50) {
      throw new Error("Limite inválido de pesquisa dirigida na Biblioteca Meta");
    }
  }
}

function buildPrompt(job) {
  return `Você é Argos, investigador comercial do Marketing Hub. Crie somente um plano de pesquisa estruturado para o ciclo ${job.cycleId}.
Tema: ${job.theme}. Público: ${job.targetAudience || "não informado"}. Objetivo: ${job.objective || "não informado"}.
Formule perguntas, buscas públicas, pedidos direcionados aos coletores HOTMART/CLICKBANK e consultas à Biblioteca Meta. Anúncio ativo e longevo é apenas sinal de investimento sustentado, nunca prova isolada de vendas. Exija ao menos 10 ofertas comparáveis. Não navegue em áreas autenticadas, não solicite credenciais, não invente vendas e não publique nem compre nada.`;
}

const RESEARCH_PLAN_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: [
    "questions",
    "publicQueries",
    "marketplaceRequests",
    "metaAdRequests",
    "minimumComparableOffers",
    "stopConditions",
  ],
  properties: {
    questions: { type: "array", minItems: 3, items: { type: "string" } },
    publicQueries: { type: "array", minItems: 3, items: { type: "string" } },
    marketplaceRequests: {
      type: "array",
      minItems: 1,
      items: {
        type: "object",
        additionalProperties: false,
        required: ["marketplace", "query", "maxProducts"],
        properties: {
          marketplace: { type: "string", enum: ["HOTMART", "CLICKBANK"] },
          query: { type: "string" },
          maxProducts: { type: "integer", minimum: 1, maximum: 25 },
        },
      },
    },
    metaAdRequests: {
      type: "array",
      minItems: 1,
      items: {
        type: "object",
        additionalProperties: false,
        required: ["query", "country", "maxAds"],
        properties: {
          query: { type: "string" },
          country: { type: "string", minLength: 2, maxLength: 2 },
          maxAds: { type: "integer", minimum: 1, maximum: 50 },
        },
      },
    },
    minimumComparableOffers: { type: "integer", minimum: 10 },
    stopConditions: { type: "array", minItems: 1, items: { type: "string" } },
  },
};
