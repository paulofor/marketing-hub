import { execFile } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

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
      "--color",
      "never",
    ];
    const model = options.model || process.env.ARGOS_CODEX_MODEL;
    if (model) args.push("--model", model);
    await execFileAsync(command, args, {
      input: buildPrompt(job),
      timeout: Number(options.timeoutMs || process.env.ARGOS_CODEX_TIMEOUT_MS || 600000),
      maxBuffer: 10 * 1024 * 1024,
    });
    const rawResponse = await readFile(output, "utf8");
    const plan = JSON.parse(rawResponse);
    validatePlan(plan);
    return { plan, rawResponse, model: model || "codex-default", mode: "CODEX" };
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
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
}

function buildPrompt(job) {
  return `Você é Argos, investigador comercial do Marketing Hub. Crie somente um plano de pesquisa estruturado para o ciclo ${job.cycleId}.
Tema: ${job.theme}. Público: ${job.targetAudience || "não informado"}. Objetivo: ${job.objective || "não informado"}.
Formule perguntas, buscas públicas e pedidos direcionados aos coletores HOTMART/CLICKBANK. Exija ao menos 10 ofertas comparáveis. Não navegue em áreas autenticadas, não solicite credenciais, não invente vendas e não publique nem compre nada.`;
}

const RESEARCH_PLAN_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: [
    "questions",
    "publicQueries",
    "marketplaceRequests",
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
    minimumComparableOffers: { type: "integer", minimum: 10 },
    stopConditions: { type: "array", minItems: 1, items: { type: "string" } },
  },
};
