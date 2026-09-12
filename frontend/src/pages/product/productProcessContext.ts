import type { ProductProcessActivityExecutionHistory } from "../../api/businessProcess/types";
import type { ProcessAutomation } from "../../api/businessProcess/useProcessAutomation";
import type { CycleProcessContext } from "../../api/learningCycle/useCycleProcessContext";
import type { ProductValueChainPosition } from "../../api/product/useProductValueChainPositions";
import { activityVersion } from "./productActivityContext";
import {
  activityStateLabels,
  automationStateLabels,
  processStateLabels,
} from "./productProcessPresentation";

export type ProcessContext = {
  history: ProductProcessActivityExecutionHistory;
  processSequence?: string;
  cycle?: CycleProcessContext | null;
  cycleId?: number;
  chainId?: number;
  position?: ProductValueChainPosition;
  automation?: ProcessAutomation;
  warnings?: string[];
};

const value = (text?: string | number | null) =>
  typeof text === "string"
    ? text.trim() || "Não informado"
    : (text ?? "Não informado");
const yesNo = (flag: boolean) => (flag ? "Sim" : "Não");
const state = (labels: Record<string, string>, code?: string | null) =>
  code ? `${labels[code] || code} (${code})` : "Não informado";
const cost = (amount?: number | null) =>
  amount == null
    ? "Não informado"
    : `USD ${amount.toLocaleString("pt-BR", { maximumFractionDigits: 8 })}`;

/** Organiza os contratos oficiais para diagnóstico, sem prompts brutos ou comandos de execução. */
export function processContextText(context: ProcessContext, origin: string) {
  const { history: h, cycle, position, automation: receivedRun } = context;
  const cycleId = cycle?.cycleId ?? context.cycleId;
  const chainId = cycle?.chainDefinitionId ?? context.chainId;
  const samePosition =
    position?.productId === h.productId &&
    position.chainDefinitionId === chainId;
  const sequence =
    position && !samePosition ? undefined : context.processSequence;
  const runMatches =
    receivedRun &&
    receivedRun.productId === h.productId &&
    receivedRun.processDefinitionId === h.selectedProcessDefinitionId &&
    receivedRun.chainId === chainId &&
    (receivedRun.learningCycleId ?? null) === (cycleId ?? null) &&
    receivedRun.sourceReference === h.currentExecutionReference;
  const run = runMatches ? receivedRun : undefined;
  const link = (activityId?: string) => {
    const url = new URL(
      `/products/${h.productId}/value-chain-history/processes/${h.selectedProcessDefinitionId}/activities`,
      origin,
    );
    if (chainId) url.searchParams.set("chainId", String(chainId));
    if (cycleId) url.searchParams.set("learningCycleId", String(cycleId));
    if (activityId) url.hash = `activity-${activityId}`;
    return url.href;
  };
  const lines = [
    "CONTEXTO DO PROCESSO — MARKETING HUB",
    ...(context.warnings ?? []).map((warning) => `Atenção: ${warning}`),
    ...(receivedRun && !runMatches
      ? [
          "Atenção: dados de execução fora do contexto selecionado não foram incluídos.",
        ]
      : []),
    "",
    `Produto (nome interno): ${value(h.productInternalName)} (ID: ${h.productId})`,
    `Produto (nome comercial): ${value(h.productName)}`,
    `Situação comercial do produto: ${value(position?.productId === h.productId ? position.commercialStatus : undefined)}`,
    `Plano comercial: ${h.commercialPlanId ? `${value(h.commercialPlanName)} (ID: ${h.commercialPlanId})` : "Não informado"}`,
    `Cadeia de valor: ${chainId ? `#${chainId}` : "Não informada"}${samePosition ? ` · ${value(position.chainName)} · versão ${position.chainVersion == null ? "não informada" : `v${position.chainVersion}`}` : ""}`,
    `Processo: ${sequence || "Número não informado"} — ${h.processName}`,
    `Versão do processo: v${h.selectedProcessVersionNumber} · definição ID ${h.selectedProcessDefinitionId}`,
    `Identificador do processo: ${h.processCode}`,
    `Situação da definição: ${h.selectedProcessStatus}`,
    `Referência de execução: ${value(h.currentExecutionReference)}`,
    `Link do processo: ${link()}`,
    ...(run?.parentProcesses ?? []).map(
      (parent) =>
        `Processo pai: ${parent.processName} · v${parent.processVersion} · definição ID ${parent.processDefinitionId} · atividade ${parent.activityName} (${parent.activityId}) · ${new URL(parent.navigationUrl, origin).href}`,
    ),
    ...(run?.subprocesses ?? []).map(
      (child) =>
        `Subprocesso: ${child.processName} · v${child.processVersion} · definição ID ${child.processDefinitionId} · chamado por ${child.activityName} (${child.activityId}) · ${new URL(child.navigationUrl, origin).href}`,
    ),
    "",
    "CICLO E APRENDIZADOS",
    `Ciclo: ${cycle ? `${cycle.cycleNumber}º ciclo (ID: ${cycle.cycleId})` : cycleId ? `ID ${cycleId} (detalhes não disponíveis)` : "Não informado pelo backend para este processo"}`,
  ];
  if (cycle) {
    lines.push(
      `Experimento: #${cycle.experimentId}`,
      `Versão do produto: ${value(cycle.productVersion)}`,
      `Situação do ciclo: ${value(cycle.status)}`,
      `Etapa do ciclo: ${value(cycle.stageLabel)}`,
      `Hipótese: ${value(cycle.hypothesis)}`,
      `Mudança principal: ${value(cycle.mainChange)}`,
      `Link do ciclo: ${new URL(cycle.cycleUrl, origin).href}`,
      "Aprendizados dos ciclos anteriores:",
    );
    if (!cycle.previousLearning.length)
      lines.push("Nenhum aprendizado anterior informado.");
    for (const learning of cycle.previousLearning)
      lines.push(
        `- Ciclo #${learning.cycleId} · Experimento #${learning.experimentId} · Decisão: ${value(learning.action)}`,
        `  Resumo: ${value(learning.summary)}`,
        `  Aprendizado: ${value(learning.learning)}`,
        `  Evidência: ${value(learning.evidenceReference)}`,
        `  Próxima hipótese: ${value(learning.nextHypothesis)}`,
        `  Limitação: ${value(learning.limitation)}`,
      );
    if (cycle.nextWork) {
      const next = cycle.nextWork;
      lines.push(
        `Próximo trabalho informado pelo ciclo: ${next.processNumber}.${next.activityNumber} — ${next.activityName} (${next.activityId})`,
        `Processo do próximo trabalho: ${next.processName} (definição ID ${next.processDefinitionId})`,
        `Responsável: ${value(next.responsible)} · Situação: ${value(next.state)}`,
        `Motivo: ${value(next.reason)}`,
        `Link do próximo trabalho: ${new URL(next.url, origin).href}`,
      );
    }
  }
  lines.push("", "EXECUÇÃO AUTOMÁTICA");
  if (run) {
    lines.push(
      `Execução: ${run.id == null ? "Ainda não iniciada" : `#${run.id}`}`,
      `Situação: ${state(automationStateLabels, run.status)}`,
      `Motivo / pendência: ${value(run.reason)}`,
      `Progresso: ${run.completedActivities} concluídas · ${run.remainingActivities} restantes · ${run.omittedActivities} dispensadas pelo fluxo · ${run.totalActivities} no total · ${run.completionPercentage}%`,
      `Atividade atual: ${run.currentActivityId ? `${sequence ? `${sequence}.` : ""}${value(run.currentSequence)} — ${value(run.currentActivityName)} (${run.currentActivityId})` : "Nenhuma informada"}`,
      `Responsável atual: ${value(run.currentOwnerName)}`,
      `Execução automática habilitada: ${yesNo(run.automaticExecution)}`,
      `Comandos disponíveis: iniciar ${yesNo(run.canStart)}, pausar ${yesNo(run.canPause)}, retomar ${yesNo(run.canResume)}`,
      `Custo conhecido da execução: ${cost(run.knownCostUsd)} · cobertura: ${value(run.costCoverage)}`,
      `Última conciliação: ${value(run.lastReconciledAt)}`,
      `Última atualização: ${value(run.updatedAt)} · revisão ${run.revision}`,
      ...(run.childRunId
        ? [`Execução de subprocesso: #${run.childRunId}`]
        : []),
      ...(run.navigationUrl
        ? [`Link de acompanhamento: ${new URL(run.navigationUrl, origin).href}`]
        : []),
    );
  } else lines.push("Execução automática: não disponível nesta consulta.");
  lines.push(
    "",
    "SITUAÇÃO DAS ATIVIDADES NO BACKEND",
    `Situação do processo: ${state(processStateLabels, h.operationalState)}`,
    `Objetivo do processo comprovado: ${yesNo(h.objectiveAchieved)}`,
    `Atividades da versão selecionada: ${h.selectedActivityCount} · concluídas: ${h.completedActivityCount} · restantes: ${h.remainingActivityCount} · bloqueadas: ${h.blockedActivityCount}`,
    `Registros de atividades incluindo histórico: ${h.activityCount} · com tarefas: ${h.activitiesWithTasksCount} · tarefas únicas: ${h.uniqueTaskCount}`,
    `Atividade atual registrada: ${h.currentActivityId ? `${value(h.currentActivityName)} (${h.currentActivityId}) · ${state(activityStateLabels, h.currentActivityState)}` : "Nenhuma informada"}`,
    `Motivo da atividade atual: ${value(h.currentActivityStateReason)}`,
    `Custo conhecido das tarefas: ${cost(h.knownEstimatedCostUsd)} · cobertura: ${h.costCoverage}`,
  );
  for (const activity of h.activities) {
    lines.push(
      "",
      `Atividade: ${sequence ? `${sequence}.` : ""}${activity.sequenceNumber} — ${activity.activityName}`,
      `Versão da atividade: ${activityVersion(h, activity)}`,
      `Identificador da atividade: ${activity.activityId}`,
      `Registro da atividade: ${activity.selectedVersionActivity ? "versão selecionada" : "histórico (fora da versão selecionada)"}`,
      `Responsável: ${value(activity.activityOwnerName)} · Tipo de executor: ${value(activity.executionControl?.executorType)}`,
      `Objetivo: ${value(activity.activityObjective)}`,
      `Situação: ${state(activityStateLabels, activity.operationalState)} · Objetivo comprovado: ${yesNo(activity.objectiveAchieved)}`,
      `Motivo / pendência: ${value(activity.stateReason)}`,
      `Origem da comprovação: ${activity.stateEvidence}`,
      ...(activity.activityInstanceId
        ? [
            `Instância da atividade: #${activity.activityInstanceId} · ocorrência: ${value(activity.occurrenceNumber)}`,
          ]
        : []),
      `Orientação de execução: ${value(activity.executionRequestReason)}`,
      `Link da atividade: ${link(activity.activityId)}`,
    );
    const control = activity.executionControl;
    if (control) {
      lines.push(
        `Controle: ${control.interactionType} · ${value(control.description)}`,
        `Ação: ${value(control.actionLabel)} · disponível: ${yesNo(control.actionAvailable)} · confirmação necessária: ${yesNo(control.confirmationRequired)}`,
        `Condição: ${value(control.availabilityReason)}`,
      );
      for (const requirement of control.requirements)
        lines.push(
          `- Critério ${requirement.code} — ${requirement.title}: ${requirement.satisfied ? "Atendido" : "Pendente"}`,
          `  Detalhe: ${value(requirement.detail)}`,
          `  Recomendação: ${value(requirement.recommendation)}`,
        );
      if (control.targetProcessDefinitionId)
        lines.push(
          `Subprocesso: definição ID ${control.targetProcessDefinitionId}`,
        );
      if (control.auditEvidenceReference)
        lines.push(
          `Referência de evidência: ${control.auditEvidenceReference}`,
        );
    }
    if (activity.recoveryAction) {
      const recovery = activity.recoveryAction;
      lines.push(
        `Correção dependente: ${recovery.activityName} (${recovery.activityId}) · ${value(recovery.ownerName)}`,
        `Motivo da dependência: ${value(recovery.availabilityReason)}`,
        `Link da correção: ${link(recovery.activityId)}`,
      );
    }
    lines.push(`Tarefas da atividade: ${activity.taskCount}`);
    for (const task of activity.tasks) {
      lines.push(
        `- Tarefa #${task.taskId}: ${task.title} · ${task.status} · ${value(task.assignedAgentNickname)} (${value(task.assignedAgentKey)})`,
        `  Processo v${task.processVersionNumber} · definição ID ${task.processDefinitionId} · referência: ${value(task.sourceReference)}`,
        `  Criada: ${value(task.createdAt)} · início: ${value(task.startedAt)} · fim: ${value(task.finishedAt)}`,
        `  Modelo: ${value(task.modelCode)} · modo: ${value(task.executionMode)} · esforço: ${value(task.reasoningEffort)}`,
        `  Custo: ${cost(task.estimatedCostUsd)} · cobertura: ${task.costEstimationStatus}`,
        `  Tokens: entrada ${value(task.inputTokens)} · cache ${value(task.cachedInputTokens)} · saída ${value(task.outputTokens)}`,
        ...(task.executionError ? [`  Erro: ${task.executionError}`] : []),
        ...(task.blockerGuidance?.recommendedAction
          ? [`  Orientação: ${task.blockerGuidance.recommendedAction}`]
          : []),
      );
    }
  }
  return lines.join("\n");
}
