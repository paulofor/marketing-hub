export type AgentTaskStatus =
  "PENDING" | "IN_PROGRESS" | "COMPLETED" | "BLOCKED" | "CANCELLED";

export interface AgentTask {
  id: number;
  assignedAgentId: number;
  assignedAgentKey: string;
  assignedAgentNickname: string;
  requestedByType: "HUMAN" | "AGENT";
  requestedByAgentId?: number;
  requestedByAgentKey?: string;
  requestedByName: string;
  title: string;
  description: string;
  priority: "LOW" | "NORMAL" | "HIGH" | "URGENT";
  status: AgentTaskStatus;
  sourceReference?: string;
  processDefinitionId?: number;
  processCode?: string;
  processVersionNumber?: number;
  processActivityId?: string;
  processActivityName?: string;
  exceptional: boolean;
  exceptionReason?: string;
  taskKind: "WORK" | "GATE_DECISION";
  gateCode?: string;
  gateStatus?: "PENDING" | "APPROVED" | "REJECTED";
  gateDecisionReason?: string;
  gateDecidedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAgentTaskPayload {
  assignedAgentKey: string;
  requestedByName: string;
  title: string;
  description: string;
  priority: AgentTask["priority"];
  sourceReference?: string;
  processDefinitionId?: number;
  processActivityId?: string;
  exceptional: boolean;
  exceptionReason?: string;
}
