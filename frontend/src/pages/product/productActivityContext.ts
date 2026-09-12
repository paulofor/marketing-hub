import type {
  ProductProcessActivityExecutionGroup,
  ProductProcessActivityExecutionHistory,
} from "../../api/businessProcess/types";
import type { CycleProcessContext } from "../../api/learningCycle/useCycleProcessContext";

export type ActivityContextProps = {
  history: ProductProcessActivityExecutionHistory;
  activity: ProductProcessActivityExecutionGroup;
  processSequence?: string;
  cycle?: CycleProcessContext | null;
  cycleId?: number;
  chainId?: number;
  loading?: boolean;
};

/** Identifica a definição versionada da atividade sem confundir versão com número ordinal. */
export function activityVersion(
  history: ProductProcessActivityExecutionHistory,
  activity: ProductProcessActivityExecutionGroup,
) {
  if (activity.activityDefinitionId)
    return `v${history.selectedProcessVersionNumber} · definição ID ${activity.activityDefinitionId}`;
  const historicalVersions = [
    ...new Set(
      activity.tasks
        .map((task) => task.processVersionNumber)
        .filter((version): version is number => Number.isInteger(version)),
    ),
  ].sort((left, right) => right - left);
  if (historicalVersions.length)
    return `${historicalVersions.map((version) => `v${version}`).join(", ")} · definição histórica não informada`;
  return "Não informada · definição histórica não disponível";
}

/** Formata apenas identidades recebidas do backend, sem inferir ciclo ou responsável. */
export function activityContext({
  history,
  activity,
  processSequence,
  cycle,
  cycleId,
  chainId,
}: ActivityContextProps) {
  const number = processSequence
    ? `${processSequence}.${activity.sequenceNumber}`
    : `${activity.sequenceNumber} (número do processo não informado)`;
  const executor = activity.executionControl?.executorType;
  const agent =
    executor === "HUMAN"
      ? "Não se aplica (atividade humana)"
      : executor === "BACKEND"
        ? "Não se aplica (execução pelo backend)"
        : activity.activityOwnerName?.trim() || "Não informado";
  const effectiveCycleId = cycle?.cycleId ?? cycleId;
  const effectiveChainId = cycle?.chainDefinitionId ?? chainId;
  const link = new URL(
    `/products/${history.productId}/value-chain-history/processes/${history.selectedProcessDefinitionId}/activities`,
    window.location.origin,
  );
  if (effectiveCycleId)
    link.searchParams.set("learningCycleId", String(effectiveCycleId));
  if (effectiveChainId)
    link.searchParams.set("chainId", String(effectiveChainId));
  link.hash = `activity-${activity.activityId}`;

  return [
    `Processo: ${processSequence || "Número não informado"} — ${history.processName}`,
    `Versão do processo: v${history.selectedProcessVersionNumber} · definição ID ${history.selectedProcessDefinitionId}`,
    `Atividade: ${number} — ${activity.activityName}`,
    `Versão da atividade: ${activityVersion(history, activity)}`,
    `Produto (nome interno): ${history.productInternalName?.trim() || "Não informado"} (ID: ${history.productId})`,
    `Agente (nome interno): ${agent}`,
    ...(executor === "HUMAN" || executor === "BACKEND"
      ? [
          `Responsável: ${activity.activityOwnerName?.trim() || "Não informado"}`,
        ]
      : []),
    ...(cycle
      ? [
          `Ciclo: ${cycle.cycleNumber}º ciclo (ID: ${cycle.cycleId})`,
          `Experimento: #${cycle.experimentId}`,
          `Versão do produto: ${cycle.productVersion}`,
        ]
      : effectiveCycleId
        ? [`Ciclo: ID ${effectiveCycleId} (número não informado)`]
        : []),
    ...(effectiveChainId ? [`Cadeia de valor: #${effectiveChainId}`] : []),
    `Identificador da atividade: ${activity.activityId}`,
    ...(!activity.selectedVersionActivity
      ? ["Registro da atividade: histórico (fora da versão selecionada)"]
      : []),
    `Link da atividade: ${link.href}`,
  ].join("\n");
}
