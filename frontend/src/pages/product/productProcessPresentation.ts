import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import { salesActivityStateLabels } from "../../api/learningCycle/salesFlow";

export const activityStateLabels: Record<
  ProductProcessActivityExecutionGroup["operationalState"],
  string
> = {
  HISTORICAL: salesActivityStateLabels.HISTORICAL,
  NOT_APPLICABLE: salesActivityStateLabels.NOT_APPLICABLE,
  RECORDED: salesActivityStateLabels.RECORDED,
  WAITING: salesActivityStateLabels.WAITING,
  NOT_STARTED: "Não iniciada",
  PENDING: "Pendente",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

export const processStateLabels = {
  NOT_RECORDED: "Sem atividades registradas",
  NOT_STARTED: "Não iniciado",
  PENDING: "Aguardando execução",
  IN_PROGRESS: "Em andamento",
  BLOCKED: "Bloqueado",
  COMPLETED: "Concluído",
  CANCELLED: "Cancelado",
} as const;

export const automationStateLabels: Record<string, string> = {
  READY: "Pronto para executar",
  UNAVAILABLE: "Indisponível",
  QUEUED: "Na fila",
  WAITING_ACTIVITY: "Em execução",
  WAITING_INPUT: "Aguardando condições",
  WAITING_HUMAN: "Precisa da sua decisão",
  WAITING_SUBPROCESS: "Subprocesso em execução",
  WAITING_PARENT: "Aguardando processo de origem",
  BLOCKED: "Precisa de atenção",
  PAUSING: "Concluindo a pausa",
  PAUSED: "Pausado",
  COMPLETED: "Processo concluído",
  CLOSED: "Encerrado com pendências",
  ERROR: "Falha técnica",
  REVALIDATION_REQUIRED: "Revalidação necessária",
};
