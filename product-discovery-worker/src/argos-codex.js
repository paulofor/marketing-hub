import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { readStrictOutputSchema } from "./strict-output-schema.js";

/** Cria com Codex um plano de investigação; nenhuma credencial de marketplace entra no prompt. */
export async function planDirectedResearch(job, options = {}) {
  const enabled =
    String(options.enabled ?? process.env.ARGOS_CODEX_ENABLED) === "true";
  if (!enabled) return deterministicPlan(job);
  const prompt = await buildPromptComposition(job);
  const schemaResource = planSchemaResource(job);
  const { contract: schemaContract } = await readStrictOutputSchema(
    schemaResource.url,
    schemaResource.name,
  );
  const directory = await mkdtemp(join(tmpdir(), "argos-plan-"));
  const output = join(directory, "output.json");
  const schema = join(directory, "schema.json");
  const model = options.model || process.env.ARGOS_CODEX_MODEL;
  const reasoningEffort =
    options.reasoningEffort ||
    process.env.ARGOS_CODEX_PLAN_REASONING_EFFORT ||
    process.env.ARGOS_CODEX_REASONING_EFFORT ||
    "medium";
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
        options.timeoutMs ||
          process.env.ARGOS_CODEX_PLAN_TIMEOUT_MS ||
          process.env.ARGOS_CODEX_TIMEOUT_MS ||
          600000,
      ),
      maxBuffer: 10 * 1024 * 1024,
      phaseName: "planejamento",
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
    const plan = normalizePlanForExecution(JSON.parse(rawResponse), job);
    validatePlan(plan, job);
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
  const phaseName = String(options.phaseName || "execução").trim();
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
          new Error(`Saída da ${phaseName} de Argos excedeu o limite seguro`),
        );
      }
      return next;
    };

    try {
      child = spawnProcess(command, args, { stdio: ["pipe", "pipe", "pipe"] });
    } catch (error) {
      rejectOnce(
        new Error(
          `Falha ao iniciar a ${phaseName} de Argos: ${error.message}`,
          {
            cause: error,
          },
        ),
      );
      return;
    }

    timeout = setTimeout(() => {
      child.kill("SIGTERM");
      rejectOnce(
        new Error(
          `${capitalize(phaseName)} de Argos excedeu o timeout de ${timeoutMs} ms`,
        ),
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
        new Error(
          `Falha ao executar a ${phaseName} de Argos: ${error.message}`,
          {
            cause: error,
          },
        ),
      );
    });
    child.on("close", (code, signal) => {
      if (settled) return;
      if (code !== 0) {
        const detail = parseCodexFailure(stdout) || stderr.trim().slice(-2000);
        rejectOnce(
          new Error(
            `Codex encerrou a ${phaseName} de Argos com código ${code ?? "desconhecido"}${signal ? ` e sinal ${signal}` : ""}${detail ? `: ${detail}` : ""}`,
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
            `Falha ao enviar o contexto da ${phaseName} de Argos ao Codex: ${error.message}`,
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

/** Extrai do stream JSON a causa funcional emitida pelo Codex. */
export function parseCodexFailure(stdout) {
  let detail = "";
  for (const line of String(stdout || "").split("\n")) {
    if (!line.trim()) continue;
    try {
      const event = JSON.parse(line);
      if (!["error", "turn.failed"].includes(event.type)) continue;
      const message =
        typeof event.message === "string"
          ? event.message
          : typeof event.error === "string"
            ? event.error
            : event.error?.message;
      if (typeof message === "string" && message.trim()) {
        detail = message
          .replace(/(?:sk-|sess-|eyJ)[A-Za-z0-9._-]+/g, "[SEGREDO_REMOVIDO]")
          .replace(/\s+/g, " ")
          .trim()
          .slice(-2000);
      }
    } catch {
      // O stream também pode conter linhas operacionais não estruturadas.
    }
  }
  return detail;
}

/** Seleciona um contrato estrito sem campos condicionais entre atividades. */
export function planSchemaResource(job = {}) {
  const gapDeepening = job.stageCode === "candidate-gap-deepening";
  return {
    name: gapDeepening
      ? "aprofundamento de lacunas de Argos"
      : "planejamento inicial de Argos",
    url: new URL(
      gapDeepening
        ? "../prompts/productdiscovery.v1/plan/gap-deepening-schema.json"
        : "../prompts/productdiscovery.v1/plan/plan-schema.json",
      import.meta.url,
    ),
  };
}

/** Inicia a descrição da fase com maiúscula sem alterar o texto auditável restante. */
function capitalize(value) {
  const text = String(value || "execução");
  return `${text.charAt(0).toLocaleUpperCase("pt-BR")}${text.slice(1)}`;
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
  if (job.stageCode === "candidate-gap-deepening") {
    return deterministicGapDeepeningPlan(job);
  }
  const theme = compactQuery(
    [job.theme, job.targetAudience].filter(Boolean).join(" "),
  );
  const metaQuery = metaCategoryQuery(job);
  const consumerInstagramFocus = requiresConsumerInstagramFocus(job);
  const discoveryMode = job.researchMode === "DISCOVER_MARKETS";
  const referenceQueries = referenceSourceQueries(job, theme);
  const plan = {
    researchLens: compactQuery(
      discoveryMode
        ? `Escopo inicial de situações pessoais em ${theme}`
        : `Validação factual de ${theme}`,
      160,
    ),
    expansionAxis: "INITIAL_SCOPE",
    expansionRationale:
      "Primeira tentativa preserva o tema, o público e as restrições comerciais recebidas.",
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
      {
        marketplace: "HOTMART",
        query: compactQuery(theme, 80),
        maxProducts: 10,
      },
      {
        marketplace: "CLICKBANK",
        query: compactQuery(theme, 80),
        maxProducts: 10,
      },
    ],
    metaAdRequests: [
      {
        query: metaQuery,
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

/** Produz uma retomada limitada por candidata quando o planejador de modelo não está disponível. */
function deterministicGapDeepeningPlan(job) {
  const candidates = job.previousCandidates || [];
  if (candidates.length < 2 || candidates.length > 3) {
    throw new Error(
      "Aprofundamento exige duas ou três candidatas persistidas na pesquisa inicial",
    );
  }
  const queryCounts = candidates.length === 2 ? [4, 4] : [3, 3, 2];
  const candidateGaps = candidates.map((candidate, index) => {
    const name = compactQuery(candidate.name, 90);
    const queries = [
      `${name} relato comprou desistiu dificuldade`,
      `${name} preço entrega público review`,
      `${name} alternativa gratuita reclamação`,
      `${name} evidência contrária não funciona`,
    ].slice(0, queryCounts[index]);
    return {
      candidateName: candidate.name,
      pendingQuestion: compactQuery(
        `Qual situação, alternativa e obstáculo residual tornam ${name} prioritária ou dispensável?`,
        300,
      ),
      appropriateSource:
        "Relatos públicos de clientes, ofertas atuais aderentes, anúncios observados e contrapontos independentes.",
      evidenceNeeded:
        "Ocasião concreta, preço e entrega atuais, público atendido, compra ou desistência e dificuldade residual.",
      contraryEvidenceToSeek:
        "Relatos de ausência de prioridade, solução gratuita suficiente, reembolso, abandono ou resultado não entregue.",
      publicQueries: queries,
      maxPublicQueries: queries.length,
      maxEstimatedSearchCostUsd: Number((queries.length * 0.005).toFixed(8)),
    };
  });
  const publicQueries = candidateGaps.flatMap((gap) => gap.publicQueries);
  const theme = compactQuery(
    candidates.map((candidate) => candidate.name).join(" e "),
    120,
  );
  const plan = {
    researchLens: compactQuery(`Lacunas específicas de ${theme}`, 160),
    expansionAxis: "INITIAL_SCOPE",
    expansionRationale:
      "A pesquisa retoma as candidatas persistidas, a política de evidências recebida e as perguntas que impedem o handoff.",
    questions: [
      ...candidateGaps.map((gap) => gap.pendingQuestion),
      "Que evidência contrária altera a conclusão anterior sem transformar ausência de fonte em ausência de mercado?",
    ],
    publicQueries,
    marketplaceRequests: [
      { marketplace: "HOTMART", query: theme, maxProducts: 10 },
      { marketplace: "CLICKBANK", query: theme, maxProducts: 10 },
    ],
    metaAdRequests: [
      {
        query: normalizeMetaQuery([theme, job.theme, job.targetAudience]),
        country: "BR",
        publisherPlatform: "INSTAGRAM",
        maxAds: 25,
      },
    ],
    minimumComparableOffers: 10,
    stopConditions: [
      "nenhuma evidência nova para a pergunta candidata-específica",
      "lente ou consulta repetida",
      "limite de doze consultas ou US$ 0,06 estimados nesta tentativa",
      "fonte atual indisponível ou contraditória sem resolução",
    ],
    candidateGaps,
    researchLimits: {
      maxPublicQueries: 12,
      maxEstimatedSearchCostUsd: 0.06,
    },
  };
  validatePlan(plan, job);
  return {
    plan,
    rawResponse: JSON.stringify(plan),
    model: "deterministic-gap-deepening-v1",
    mode: "DETERMINISTIC",
    prompt: JSON.stringify({
      operation: "PRODUCT_DISCOVERY_CANDIDATE_GAP_DEEPENING_V1",
      cycleId: job.cycleId,
      candidateNames: candidates.map((candidate) => candidate.name),
      interviewCount: (job.customerInterviews || []).length,
      limits: job.gapResearchPolicy,
    }),
    reasoningEffort: "NOT_APPLICABLE",
    usage: null,
  };
}

/** Compila a frase livre do modelo no contrato curto aceito pela Biblioteca Meta. */
export function normalizePlanForExecution(plan, job = {}) {
  if (!Array.isArray(plan?.metaAdRequests)) return plan;
  return {
    ...plan,
    metaAdRequests: plan.metaAdRequests.map((request) => ({
      ...request,
      query: normalizeMetaQuery([
        request?.query,
        plan.researchLens,
        job.theme,
        job.targetAudience,
      ]),
    })),
  };
}

/** Valida limites que impedem o agente de ampliar coleta ou inventar fontes. */
export function validatePlan(plan, job = {}) {
  const gapDeepening = job.stageCode === "candidate-gap-deepening";
  const minimumPublicQueries = gapDeepening ? 2 : 8;
  const maximumPublicQueries = gapDeepening ? 12 : 24;
  if (
    typeof plan.researchLens !== "string" ||
    plan.researchLens.trim().length < 8 ||
    Array.from(plan.researchLens).length > 160 ||
    ![
      "INITIAL_SCOPE",
      "ADJACENT_LIFE_MOMENT",
      "ADJACENT_PAIN_LANGUAGE",
      "ADJACENT_PAID_ALTERNATIVE",
      "ADJACENT_AUDIENCE_CONTEXT",
    ].includes(plan.expansionAxis) ||
    typeof plan.expansionRationale !== "string" ||
    plan.expansionRationale.trim().length < 12 ||
    Array.from(plan.expansionRationale).length > 500 ||
    !Array.isArray(plan.questions) ||
    plan.questions.length < 3 ||
    !Array.isArray(plan.publicQueries) ||
    plan.publicQueries.length < minimumPublicQueries ||
    plan.publicQueries.length > maximumPublicQueries ||
    !Array.isArray(plan.marketplaceRequests) ||
    plan.marketplaceRequests.length === 0 ||
    !Array.isArray(plan.metaAdRequests) ||
    plan.metaAdRequests.length !== 1 ||
    plan.minimumComparableOffers < 10
  ) {
    throw new Error("Plano dirigido de Argos fora do contrato v1");
  }
  if (gapDeepening) validateCandidateGapPlan(plan, job);
  if (
    plan.publicQueries.some((query) => !query || Array.from(query).length > 180)
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
    const metaTerms = metaQueryTerms(request.query);
    const backendTerms = backendMetaQueryTerms(request.query);
    if (
      !request.query ||
      Array.from(request.query).length > 60 ||
      metaTerms.length < 2 ||
      metaTerms.length > 5 ||
      backendTerms.length < 2 ||
      backendTerms.length > 5 ||
      !request.country ||
      request.publisherPlatform !== "INSTAGRAM" ||
      request.maxAds < 1 ||
      request.maxAds > 50
    ) {
      throw new Error(
        "Consulta Meta deve representar uma categoria ampla com dois a cinco termos e até 60 caracteres",
      );
    }
  }
}

/** Exige pergunta, fonte, contraponto e orçamento para cada candidata já persistida. */
function validateCandidateGapPlan(plan, job) {
  const expectedNames = new Set(
    (job.previousCandidates || []).map((item) => normalizeIdentity(item.name)),
  );
  const gaps = Array.isArray(plan.candidateGaps) ? plan.candidateGaps : [];
  const receivedNames = new Set();
  let plannedQueries = 0;
  let plannedCost = 0;
  for (const gap of gaps) {
    const name = normalizeIdentity(gap?.candidateName);
    if (
      !name ||
      receivedNames.has(name) ||
      !expectedNames.has(name) ||
      !String(gap.pendingQuestion || "").trim() ||
      !String(gap.appropriateSource || "").trim() ||
      !String(gap.evidenceNeeded || "").trim() ||
      !String(gap.contraryEvidenceToSeek || "").trim() ||
      !Array.isArray(gap.publicQueries) ||
      gap.publicQueries.length < 1 ||
      gap.publicQueries.length > 4 ||
      Number(gap.maxPublicQueries) !== gap.publicQueries.length ||
      Math.abs(
        Number(gap.maxEstimatedSearchCostUsd) -
          gap.publicQueries.length * 0.005,
      ) > 0.000000001
    ) {
      throw new Error(
        "Plano de aprofundamento não cobre pergunta, fonte, contraponto e limite por candidata",
      );
    }
    receivedNames.add(name);
    plannedQueries += gap.publicQueries.length;
    plannedCost += Number(gap.maxEstimatedSearchCostUsd);
  }
  const declaredQueries = new Set(plan.publicQueries.map(normalizeIdentity));
  const candidateQueries = new Set(
    gaps.flatMap((gap) => gap.publicQueries).map(normalizeIdentity),
  );
  const previousQueries = new Set(
    (job.marketExpansionContext?.previousPublicQueries || []).map(
      normalizeIdentity,
    ),
  );
  const previousLenses = new Set(
    (job.marketExpansionContext?.previousResearchLenses || []).map((item) =>
      normalizeIdentity(item.researchLens),
    ),
  );
  const previousMarketplaceQueries = new Set(
    (job.marketExpansionContext?.previousMarketplaceQueries || []).map(
      normalizeIdentity,
    ),
  );
  const previousMetaQueries = new Set(
    (job.marketExpansionContext?.previousMetaQueries || []).map(
      normalizeIdentity,
    ),
  );
  const laterAttempt = previousQueries.size > 0;
  const hasNewMarketplaceQuery = (plan.marketplaceRequests || []).some(
    (request) =>
      !previousMarketplaceQueries.has(
        normalizeIdentity(`${request.marketplace}:${request.query}`),
      ),
  );
  const hasNewMetaQuery = (plan.metaAdRequests || []).some(
    (request) => !previousMetaQueries.has(normalizeIdentity(request.query)),
  );
  if (
    !setsEqual(receivedNames, expectedNames) ||
    !setsEqual(declaredQueries, candidateQueries) ||
    candidateQueries.size !== plannedQueries ||
    [...candidateQueries].some((query) => previousQueries.has(query)) ||
    (laterAttempt &&
      (plan.expansionAxis === "INITIAL_SCOPE" ||
        plannedQueries < 4 ||
        previousLenses.has(normalizeIdentity(plan.researchLens)) ||
        !hasNewMarketplaceQuery ||
        !hasNewMetaQuery)) ||
    plannedQueries > 12 ||
    plannedCost > 0.060000001 ||
    Number(plan.researchLimits?.maxPublicQueries) !== 12 ||
    Number(plan.researchLimits?.maxEstimatedSearchCostUsd) !== 0.06
  ) {
    throw new Error(
      "Plano de aprofundamento não preserva candidatas ou excede o teto da tentativa",
    );
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
    researchIntelligence: JSON.stringify(
      job.researchIntelligence || null,
      null,
      2,
    ),
    researchLibraryContext: JSON.stringify(
      researchLibraryPromptContext(job.researchLibraryContext),
      null,
      2,
    ),
    marketExpansionContext: JSON.stringify(
      job.marketExpansionContext || {
        strategyCode: "BOUNDED_ADJACENT_MARKET_EXPANSION_V1",
        attemptNumber: 1,
        maxAttempts: 3,
        instruction: "Investigue o escopo inicial recebido.",
      },
      null,
      2,
    ),
    stageCode: job.stageCode || "research",
    evidencePolicy: job.evidencePolicy || "CONSENTED_INTERVIEWS_V1",
    previousCandidates: JSON.stringify(job.previousCandidates || [], null, 2),
    customerInterviews: JSON.stringify(job.customerInterviews || [], null, 2),
    gapResearchPolicy: JSON.stringify(job.gapResearchPolicy || null, null, 2),
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

/** Normaliza identidade e consultas somente para validação de igualdade. */
function normalizeIdentity(value) {
  return String(value || "")
    .trim()
    .toLocaleLowerCase("pt-BR");
}

/** Confere conjuntos sem depender da ordem escolhida pelo modelo. */
function setsEqual(left, right) {
  return left.size === right.size && [...left].every((item) => right.has(item));
}

/** Resume a biblioteca viva para planejar consultas sem duplicar manifestos e artigos integrais. */
function researchLibraryPromptContext(context = {}) {
  return {
    evidence: (context.evidence || []).slice(0, 7).map((item) => ({
      evidenceId: item.evidenceId,
      collection: item.collection,
      title: item.title,
      date: item.date,
      path: item.path,
      excerpt: truncateForPrompt(item.excerpt, 900),
    })),
    coverage: (context.coverage || []).map((item) => ({
      collection: item.collection,
      status: item.status,
      documentCount: Number(item.documentCount || 0),
    })),
  };
}

/** Aplica um teto previsível ao texto enviado ao modelo sem modificar o artefato persistido. */
function truncateForPrompt(value, maxChars) {
  const text = String(value || "").trim();
  return Array.from(text).slice(0, maxChars).join("");
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
  const normalized = String(value || "")
    .replace(/\s+/g, " ")
    .trim();
  if (Array.from(normalized).length <= maxLength) return normalized;
  return Array.from(normalized)
    .slice(0, maxLength)
    .join("")
    .replace(/\s+\S*$/, "")
    .trim();
}

/** Extrai uma categoria comercial curta, sem copiar idade, público ou briefing inteiro. */
function metaCategoryQuery(job) {
  return normalizeMetaQuery([job?.theme, job?.targetAudience]);
}

/** Mantém somente termos comerciais úteis para a consulta pública da Biblioteca Meta. */
function metaQueryTerms(value) {
  const ignored = new Set([
    "anos",
    "brasil",
    "brasileira",
    "brasileiras",
    "brasileiro",
    "brasileiros",
    "consumidor",
    "consumidora",
    "consumidores",
    "entre",
    "feminino",
    "feminina",
    "foco",
    "homem",
    "homens",
    "instagram",
    "mulher",
    "mulheres",
    "pessoa",
    "pessoas",
    "produto",
    "produtos",
    "com",
    "das",
    "dos",
    "para",
    "pela",
    "pelo",
  ]);
  return String(value || "")
    .toLocaleLowerCase("pt-BR")
    .split(/[^\p{L}\p{N}]+/u)
    .map((term) => term.trim())
    .filter(
      (term) => term.length >= 3 && !ignored.has(term) && !/^\d+$/.test(term),
    )
    .filter((term, index, terms) => terms.indexOf(term) === index);
}

/** Reproduz a contagem defensiva do backend para impedir divergência entre os contratos. */
function backendMetaQueryTerms(value) {
  const ignored = new Set([
    "como",
    "para",
    "pela",
    "pelo",
    "brasil",
    "consumidor",
    "instagram",
    "produto",
    "produtos",
    "digital",
    "digitais",
    "anuncio",
    "anuncios",
  ]);
  return String(value || "")
    .toLocaleLowerCase("pt-BR")
    .split(/[^\p{L}\p{N}]+/u)
    .map((term) => term.trim())
    .filter(
      (term) => term.length >= 4 && !ignored.has(term) && !/^\d+$/.test(term),
    )
    .filter((term, index, terms) => terms.indexOf(term) === index);
}

/** Reduz uma consulta livre sem perder duas palavras que o backend consiga pesquisar. */
function normalizeMetaQuery(values) {
  const [primary, ...fallbacks] = values || [];
  const primaryTerms = metaQueryTerms(primary);
  const selected = primaryTerms.slice(0, 5);
  const supplementalCandidates = [
    ...new Set([
      ...primaryTerms.slice(5),
      ...fallbacks.flatMap((value) => metaQueryTerms(value)),
    ]),
  ];
  const remainingSpecific = supplementalCandidates.filter(
    (term) =>
      backendMetaQueryTerms(term).length > 0 && !selected.includes(term),
  );

  while (backendMetaQueryTerms(selected.join(" ")).length < 2) {
    const replacement =
      remainingSpecific.shift() ||
      ["serviços", "pessoais", "soluções", "cuidados"].find(
        (term) => !selected.includes(term),
      );
    if (!replacement) break;
    if (selected.length < 5) {
      selected.push(replacement);
      continue;
    }
    let replaceIndex = -1;
    for (let index = selected.length - 1; index >= 0; index -= 1) {
      if (backendMetaQueryTerms(selected[index]).length === 0) {
        replaceIndex = index;
        break;
      }
    }
    if (replaceIndex < 0) break;
    selected[replaceIndex] = replacement;
  }

  return compactQuery(selected.join(" "), 60);
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
          compactQuery(
            `site:${domain} ${theme} problema desejo tendência`,
            180,
          ),
        ];
      } catch {
        return [];
      }
    })
    .slice(0, 6);
}
