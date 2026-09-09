export type SalesFlow = {
  productId: number;
  experimentId: number;
  cycleId: number;
  cycleProcessDefinitionId?: number;
  chainDefinitionId: number;
  modelProcessDefinitionId: number;
  currentActivityId?: string | null;
  currentActivityName?: string | null;
  currentActivitySequenceNumber?: number | null;
  state: string;
  reason: string;
  navigationUrl: string;
  modelUrl: string;
  activities: {
    activityId: string;
    sequenceNumber: number;
    name: string;
    state: string;
    objectiveAchieved: boolean;
    reason: string;
    evidenceReference?: string | null;
    enteredAt?: string | null;
    exitedAt?: string | null;
  }[];
  transitions: {
    eventId: number;
    revision: number;
    action: string;
    flowId?: string | null;
    from: string;
    to: string;
    returnFlow: boolean;
    reason: string;
    responsible: string;
    evidenceReference: string;
    occurredAt: string;
  }[];
};

export const salesActivityStateLabels: Record<string, string> = {
  HISTORICAL: "Referência histórica",
  NOT_APPLICABLE: "Não aplicável neste período",
  RECORDED: "Rodada registrada",
  WAITING: "Aguardando etapa anterior",
  COMPLETED: "Concluída",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
};
