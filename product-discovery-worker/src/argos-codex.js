import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

/** Cria com Codex um plano de investigação; nenhuma credencial de marketplace entra no prompt. */
export async function planDirectedResearch(job, options = {}) {
  const enabled =
    String(options.enabled ?? process.env.ARGOS_CODEX_ENABLED) === "true";
  if (!enabled) return deterministicPlan(job);
  const prompt = await buildPromptComposition(job);
  const schemaContract = await readFile(
    new URL(
      "../prompts/productdiscovery.v1/plan/plan-schema.json",
      import.meta.url,
    ),
    "utf8",
  );
  JSON.parse(schemaContract);
  const directory = await mkdtemp(join(tmpdir(), "argos-plan-"));
  const output = join(directory, "output.json");
  const schema = join(directory, "schema.json");
  const model = options.model || process.env.ARGOS_CODEX_MODEL;
  const reasoningEffort =
    options.reasoningEffort ||
    process.env.ARGOS_CODEX_REASONING_EFFORT ||
    "high";
  let execution;
  try {
    await writeFile(schema, schemaContract);
    const command =
      options.command || process.env.ARGOS_CODEX_COMMAND || "codex";
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
    args.push("--config", `model_reasoning_effort="${reasoningEffort}"`);
    if (model) args.push("--model", model);
    const execute = options.execute || executeCodexWithInput;
    execution = await execute(command, args, prompt.fullPrompt, {
      timeoutMs: Number(
        options.timeoutMs || process.env.ARGOS_CODEX_TIMEOUT_MS || 600000,
      ),
      maxBuffer: 10 * 1024 * 1024,
    });
    let rawResponse;
    try {
      rawResponse = await readFile(output, "utf8");
    } catch (error) {
      if (error?.code === "ENOENT") {
        throw new Error(
          "Codex terminou sem produzir o plano estruturado de Argos",
          {
            cause: error,
          },
        );
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
      prompt: prompt.fullPrompt,
      agentPromptPart: prompt.agentPromptPart,
      activityPromptPart: prompt.activityPromptPart,
      reasoningEffort,
      usage: parseCodexUsage(execution?.stdout),
    };
  } catch (error) {
    const failure = error instanceof Error ? error : new Error(String(error));
    failure.executionAudit = {
      executionMode: "MODEL",
      modelCode: model || "codex-default",
      reasoningEffort,
      promptSent: prompt.fullPrompt,
      agentPromptPart: prompt.agentPromptPart,
      activityPromptPart: prompt.activityPromptPart,
      accessedUrls: [],
    };
    throw failure;
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
        rejectOnce(
          new Error("Saída do planejamento de Argos excedeu o limite seguro"),
        );
      }
      return next;
    };

    try {
      child = spawnProcess(command, args, { stdio: ["pipe", "pipe", "pipe"] });
    } catch (error) {
      rejectOnce(
        new Error(`Falha ao iniciar o Codex para Argos: ${error.message}`, {
          cause: error,
        }),
      );
      return;
    }

    timeout = setTimeout(() => {
      child.kill("SIGTERM");
      rejectOnce(
        new Error(`Planejamento de Argos excedeu o timeout de ${timeoutMs} ms`),
      );
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
      rejectOnce(
        new Error(`Falha ao executar o Codex para Argos: ${error.message}`, {
          cause: error,
        }),
      );
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
          new Error(
            `Falha ao enviar o contexto de Argos ao Codex: ${error.message}`,
            {
              cause: error,
            },
          ),
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
  const theme = compactQuery([job.theme, job.targetAudience].filter(Boolean).join(" "));
  const consumerInstagramFocus = requiresConsumerInstagramFocus(job);
  const discoveryMode = job.researchMode === "DISCOVER_MARKETS";
  const referenceQueries = referenceSourceQueries(job, theme);
  const plan = {
    questions: consumerInstagramFocus
      ? [
          `Em qual cena pessoal o consumidor reconhece a urgência de ${theme}?`,
          `Quais produtos B2C pagos e alternativas gratuitas resolvem ${theme}?`,
          `Qual microvalor mobile de ${theme} pode ser demonstrado honestamente em um Reel?`,
          `Qual prazo, consequência material e tentativa frustrada tornam ${theme} uma decisão iminente?`,
          `Que assinatura, compra, cancelamento ou comparação de preço já acontece em ${theme}?`,
          `Por que um protótipo de ${theme} venceria Google, ChatGPT, planilha, amigo ou conteúdo gratuito?`,
          `Qual evidência mostra busca por afeto, reconhecimento ou alívio de esforço em ${theme}?`,
          `Que resultado pronto elimina prompting, configuração e montagem manual em ${theme}?`,
        ]
      : discoveryMode
        ? [
            `Que situações pessoais recorrentes aparecem em ${theme}?`,
            `Quais alternativas gratuitas e pagas já são usadas em ${theme}?`,
            `Onde há esforço residual, urgência e linguagem de compra em ${theme}?`,
          ]
        : [
            `Quais produtos pagos resolvem ${theme}?`,
            `Quais preços e alternativas se repetem em ${theme}?`,
            `Quais reclamações revelam uma lacuna explorável em ${theme}?`,
          ],
    publicQueries: consumerInstagramFocus
      ? [
          `${theme} consumidor preço review reclamação`,
          `${theme} aplicativo curso assinatura vale a pena`,
          `${theme} anúncio Instagram Reel demonstração`,
          `${theme} prazo urgente tentativa frustrada quanto pagou`,
          `${theme} assinatura cancelamento alternativa grátis`,
          `${theme} decisão de compra comparação preço`,
          `${theme} sentir valorizado reconhecido pertencimento relato`,
          `${theme} difícil trabalhoso IA prompt configurar montar reclamação`,
          `${theme} solução pronta para usar resultado imediato`,
          ...referenceQueries,
        ]
      : [
          `${theme} relato dificuldade reclamação`,
          `${theme} alternativa grátis preço review`,
          `${theme} produto serviço assinatura vale a pena`,
          `${theme} fórum comunidade dúvida recorrente`,
          `${theme} prazo urgência tentativa frustrada`,
          `${theme} trabalhoso manual confuso`,
          `${theme} estudo científico mecanismo`,
          `${theme} anúncio oferta depoimento`,
          ...referenceQueries,
        ],
    marketplaceRequests: [
      { marketplace: "HOTMART", query: compactQuery(theme, 80), maxProducts: 10 },
      { marketplace: "CLICKBANK", query: compactQuery(theme, 80), maxProducts: 10 },
    ],
    metaAdRequests: [
      {
        query: compactQuery(
          consumerInstagramFocus ? `${theme} consumidor` : theme,
          100,
        ),
        country: "BR",
        publisherPlatform: "INSTAGRAM",
        maxAds: 25,
      },
    ],
    minimumComparableOffers: 10,
    stopConditions: [
      "menos de duas fontes independentes",
      "ausência de sinal de compra",
      "credencial ou marketplace indisponível",
      ...(consumerInstagramFocus
        ? [
            "oportunidade depende de empresa ou não possui cena demonstrável no Instagram",
            "fonte comercial está vazia, vencida ou contém placeholder",
            "não existe vantagem testável sobre a alternativa gratuita",
            "a proposta exige prompting, conhecimento de IA ou montagem manual do resultado",
            "o território humano foi presumido sem duas evidências independentes",
          ]
        : []),
    ],
  };
  plan.publicQueries = [
    ...new Set(plan.publicQueries.map((query) => compactQuery(query, 180))),
  ].slice(0, 24);
  return {
    plan,
    rawResponse: JSON.stringify(plan),
    model: "deterministic-fallback-v1",
    mode: "DETERMINISTIC",
    prompt: JSON.stringify({
      operation: "PRODUCT_DISCOVERY_RESEARCH_PLAN_V1",
      input: job,
    }),
    reasoningEffort: "NOT_APPLICABLE",
    usage: null,
  };
}

/** Valida limites que impedem o agente de ampliar coleta ou inventar fontes. */
export function validatePlan(plan) {
  if (
    !Array.isArray(plan.questions) ||
    plan.questions.length < 3 ||
    !Array.isArray(plan.publicQueries) ||
    plan.publicQueries.length < 8 ||
    plan.publicQueries.length > 24 ||
    !Array.isArray(plan.marketplaceRequests) ||
    plan.marketplaceRequests.length === 0 ||
    !Array.isArray(plan.metaAdRequests) ||
    plan.metaAdRequests.length !== 1 ||
    plan.minimumComparableOffers < 10
  ) {
    throw new Error("Plano dirigido de Argos fora do contrato v1");
  }
  if (
    plan.publicQueries.some(
      (query) => !query || Array.from(query).length > 180,
    )
  ) {
    throw new Error("Consulta pública de Argos deve ser curta e atômica");
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
    if (
      !request.query ||
      Array.from(request.query).length > 100 ||
      !request.country ||
      request.publisherPlatform !== "INSTAGRAM" ||
      request.maxAds < 1 ||
      request.maxAds > 50
    ) {
      throw new Error(
        "Limite inválido de pesquisa dirigida na Biblioteca Meta",
      );
    }
  }
}

async function buildPromptComposition(job) {
  const [systemPrompt, userPrompt] = await Promise.all([
    readFile(
      new URL("../prompts/productdiscovery.v1/plan/system.md", import.meta.url),
      "utf8",
    ),
    readFile(
      new URL("../prompts/productdiscovery.v1/plan/user.md", import.meta.url),
      "utf8",
    ),
  ]);
  const values = {
    cycleId: job.cycleId,
    theme: job.theme,
    targetAudience: job.targetAudience || "não informado",
    acquisitionChannel: job.acquisitionChannel || "não informado",
    commercialConstraints: job.commercialConstraints || "não informadas",
    objective: job.objective || "não informado",
    researchMode: job.researchMode || "VALIDATE_MARKET",
    marketType: job.marketType || "UNSPECIFIED",
    referenceSources: job.referenceSources || "não informadas",
    researchLibraryContext: JSON.stringify(
      job.researchLibraryContext || { evidence: [], coverage: [] },
      null,
      2,
    ),
  };
  const agentPromptPart = systemPrompt.trim();
  const activityPromptPart = resolvePromptPlaceholders(
    userPrompt,
    values,
  ).trim();
  return {
    fullPrompt: `${agentPromptPart}\n\n${activityPromptPart}`,
    agentPromptPart,
    activityPromptPart,
  };
}

/** Resolve somente os placeholders conhecidos e preserva o restante como erro visível. */
function resolvePromptPlaceholders(template, values) {
  let prompt = template;
  for (const [key, value] of Object.entries(values)) {
    prompt = prompt.replaceAll(`{{${key}}}`, String(value));
  }
  if (/{{[^}]+}}/.test(prompt)) {
    throw new Error(
      "Prompt de planejamento de Argos possui placeholder não resolvido",
    );
  }
  return prompt;
}

/** Identifica o contrato comercial explícito sem inferir B2C apenas pelo tema. */
function requiresConsumerInstagramFocus(job) {
  return (
    /instagram/i.test(String(job?.acquisitionChannel || "")) &&
    (job?.marketType === "B2C" ||
      /\bb2c\b|consumidor|pessoa f[ií]sica/i.test(
        `${job?.commercialConstraints || ""} ${job?.targetAudience || ""}`,
      ))
  );
}

/** Reduz briefings longos a uma consulta legível sem cortar a intenção central. */
function compactQuery(value, maxLength = 140) {
  const normalized = String(value || "").replace(/\s+/g, " ").trim();
  if (Array.from(normalized).length <= maxLength) return normalized;
  return Array.from(normalized)
    .slice(0, maxLength)
    .join("")
    .replace(/\s+\S*$/, "")
    .trim();
}

/** Converte fontes editoriais declaradas em buscas públicas por domínio, sem raspar área privada. */
function referenceSourceQueries(job, theme) {
  return String(job?.referenceSources || "")
    .split(/[\n,]+/)
    .map((value) => value.trim())
    .filter(Boolean)
    .flatMap((value) => {
      try {
        const domain = new URL(value).hostname.replace(/^www\./, "");
        return [
          compactQuery(`site:${domain} ${theme}`, 180),
          compactQuery(`site:${domain} ${theme} problema desejo tendência`, 180),
        ];
      } catch {
        return [];
      }
    })
    .slice(0, 6);
}
