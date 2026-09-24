import type { IndependentBusinessProcessExecution } from "../../api/businessProcess/types";

const value = (text?: string | number | null) =>
  typeof text === "string"
    ? text.trim() || "Não informado"
    : (text ?? "Não informado");
const cost = (amount?: number | null) =>
  amount == null
    ? "Não informado"
    : `USD ${amount.toLocaleString("pt-BR", { maximumFractionDigits: 8 })}`;

/** Formata apenas campos de diagnóstico do contrato oficial, sem exportar prompts ou payloads brutos. */
export function independentExecutionAihubContext(
  detail: IndependentBusinessProcessExecution,
  origin: string,
  consultedAt: string,
) {
  const { execution: e, activities, processReport: report } = detail;
  const lines = [
    "CONTEXTO DA EXECUÇÃO INDEPENDENTE — MARKETING HUB",
    `Consulta recebida em: ${consultedAt}`,
    "Escopo: execução independente; não exige produto de entrada.",
    "Produto, tipo e ficha de entrada: não informados neste contrato. Produtos derivados, quando vinculados, aparecem nas candidatas abaixo.",
    "Cadeia e processo pai: não informados neste contrato.",
    "Experimento: não informado neste contrato.",
    `Execução: #${e.id} · ${value(e.displayName)}`,
    `Processo: ${e.processName} · v${e.processVersionNumber} · definição ID ${e.processDefinitionId}`,
    `Identificador do processo: ${e.processCode}`,
    `Referência oficial: ${value(e.sourceReference)}`,
    `Ciclo de descoberta: ${value(report?.privateValidationHandoff?.cycleId)}`,
    `Link da execução: ${new URL(`/business-process-executions/${e.id}`, origin).href}`,
    `Endpoint oficial: /api/independent-business-process-executions/${e.id}`,
    `Situação da execução no backend: ${e.status}`,
    `Progresso registrado: ${e.completedActivityCount}/${e.activityCount} atividades concluídas`,
    `Causa registrada: ${value(e.latestError)}`,
    `Criada: ${e.createdAt} · início: ${value(e.startedAt)} · fim: ${value(e.finishedAt)}`,
    `Custo estimado da execução: ${cost(e.estimatedCostUsd)} · cobertura: ${e.costCoverage}`,
    `Tokens: entrada ${value(e.inputTokens)} · cache ${value(e.cachedInputTokens)} · saída ${value(e.outputTokens)}`,
    "",
    "ENTRADA DA EXECUÇÃO",
    `Tema: ${value(e.input?.theme)}`,
    `País: ${value(e.input?.country)} · idioma: ${value(e.input?.language)}`,
    "Outros campos de entrada e payloads brutos devem ser consultados na tela oficial quando necessários; não são exportados neste resumo.",
    "",
    "ATIVIDADES E TAREFAS — ESTADOS PERSISTIDOS",
    "Objetivos, dependências e critérios de aceite: consultar a definição oficial do processo; este contrato de detalhe não os informa.",
  ];
  if (!activities.length)
    lines.push("Nenhuma atividade informada pelo backend.");
  for (const activity of activities) {
    lines.push(
      "",
      `Atividade: ${activity.activityName} (${activity.activityId})`,
      `Situação: ${activity.status}`,
      `Tarefas registradas: ${activity.tasks.length}`,
    );
    for (const task of activity.tasks) {
      lines.push(
        `- Tarefa #${task.taskId}: ${task.title} · ${task.status}`,
        `  Responsável: ${value(task.assignedAgentNickname)} (${value(task.assignedAgentKey)})`,
        `  Processo da tarefa: definição ID ${value(task.processDefinitionId)} · versão ${value(task.processVersionNumber)} · referência ${value(task.sourceReference)}`,
        `  Criada: ${task.createdAt} · início: ${value(task.startedAt)} · fim: ${value(task.finishedAt)}`,
        `  Modelo: ${value(task.modelCode)} · modo: ${value(task.executionMode)} · esforço: ${value(task.reasoningEffort)}`,
        `  Custo estimado: ${cost(task.estimatedCostUsd)} · cobertura: ${value(task.costEstimationStatus)}`,
        `  Tokens: entrada ${value(task.inputTokens)} · cache ${value(task.cachedInputTokens)} · saída ${value(task.outputTokens)}`,
        `  Erro registrado: ${value(task.executionError)}`,
      );
    }
  }
  lines.push("", "RELATÓRIO DE NEGÓCIO");
  if (!report) {
    lines.push("Relatório não informado pelo backend.");
    return lines.join("\n");
  }
  lines.push(
    `Tipo: ${report.reportType} · situação: ${report.status}`,
    `Resumo: ${value(report.headline)}`,
    `Canal: ${value(report.acquisitionChannel)}`,
    `Candidatas: ${report.candidateCount} · dossiês prontos: ${report.dossierReadyCount} · produtos planejados: ${report.plannedProductCount}`,
  );
  if (report.privateValidationHandoff) {
    const h = report.privateValidationHandoff;
    lines.push(
      `Handoff: ${h.status} · disponível: ${h.available ? "Sim" : "Não"}`,
      `Ação informada: ${value(h.actionLabel)} · motivo: ${value(h.reason)}`,
    );
  }
  for (const source of report.sourceCoverage) {
    lines.push(
      `Fonte: ${source.label} (${source.sourceCode}) · ${source.status} · ${source.itemCount} itens · ${value(source.summary)}`,
    );
  }
  if (report.marketExpansion) {
    const expansion = report.marketExpansion;
    lines.push(
      `Pesquisa: ${value(expansion.strategyCode)} · tentativas ${expansion.attemptsCompleted}/${expansion.maxAttempts}`,
      `Parada: ${value(expansion.stopReason)} · ${value(expansion.stopSummary)}`,
      `Última lente: ${value(expansion.finalResearchLens)}`,
    );
    for (const attempt of expansion.attempts) {
      lines.push(
        `Tentativa ${attempt.attemptNumber}: ${value(attempt.researchLens)} · resultado ${value(attempt.outcome)} · novas evidências ${attempt.newPublicEvidenceCount} · ofertas ${attempt.newComparableOfferCount} · anúncios ${attempt.newMetaAdCount}`,
      );
    }
  }
  for (const candidate of report.candidates) {
    lines.push(
      "",
      `Candidata #${candidate.opportunityId}: ${candidate.name}`,
      `Maturidade: ${candidate.maturity} · decisão: ${candidate.decision} · score: ${value(candidate.score)}`,
      `Público: ${value(candidate.primaryAudience)} · situação de compra: ${value(candidate.purchaseSituation)}`,
      `Dor: ${value(candidate.rootPain)} · esforço residual: ${value(candidate.residualEffort)}`,
      `Linguagem observada: ${value(candidate.observedLanguage.join("; "))}`,
      `Alternativas atuais: ${value(candidate.currentAlternatives.join("; "))}`,
      `Risco comercial: ${value(candidate.commercialRisk)}`,
      `Dossiê: ${value(candidate.dossierId)} · situação: ${value(candidate.dossierStatus)}`,
      `Plano comercial: ${value(candidate.commercialPlanId)}`,
      `Produto derivado: ${value(candidate.productId)} · ${value(candidate.productName)} · ${value(candidate.productStatus)}`,
      `Próxima ação registrada: ${value(candidate.nextAction)}`,
    );
    for (const stage of candidate.stages) {
      lines.push(
        `Etapa: ${stage.label} (${stage.stageCode}) · ${stage.agent} · ${stage.status} · tarefa ${value(stage.taskId)}`,
        `  Decisão: ${value(stage.decision)} · resumo: ${value(stage.summary)} · bloqueio: ${value(stage.blocker)}`,
      );
    }
    for (const source of candidate.sources) {
      lines.push(
        `Evidência: ${source.sourceType} · ${source.title} · ${value(source.url)} · ${value(source.evidence)}`,
      );
    }
  }
  return lines.join("\n");
}
