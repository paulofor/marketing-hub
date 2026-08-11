export interface SystemImprovement {
  id: number;
  requestedByAgentId: number;
  agentKey: string;
  agentNickname: string;
  title: string;
  description: string;
  taskReference?: string;
  status: string;
  requestedAt: string;
}

export interface SystemImprovementPayload {
  agentKey: string;
  title: string;
  description: string;
  taskReference?: string;
}
