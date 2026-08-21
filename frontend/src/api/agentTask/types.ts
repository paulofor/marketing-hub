export type AgentTaskStatus =
  "PENDING" | "IN_PROGRESS" | "COMPLETED" | "BLOCKED" | "CANCELLED";

export interface AgentTaskFailureAudit {
  readiness: "COMPLETE" | "PARTIAL";
  intendedWork: string;
  sourceReference?: string;
  processCode?: string;
  activityId?: string;
  activityName?: string;
  authorityPolicy?: string;
  accessedEvidenceJson?: string;
  producedOutputJson?: string;
  error?: string;
  missingEvidence: string[];
}

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
  resultJson?: string;
  evidenceJson?: string;
  executionError?: string;
  failureAudit?: AgentTaskFailureAudit;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus:
    | "NOT_REPORTED"
    | "NOT_APPLICABLE"
    | "ESTIMATED"
    | "PARTIALLY_ESTIMATED"
    | "PRICING_UNAVAILABLE";
  modelUsageUpdatedAt?: string;
  receivedAt?: string;
  deliveredAt?: string;
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

export type ProcessInstanceOperationalState =
  | "RELEASED"
  | "WAITING_PREDECESSOR"
  | "IN_PROGRESS"
  | "BLOCKED"
  | "COMPLETED"
  | "CANCELLED"
  | "SUPERSEDED_LEGACY";

export interface ProcessInstanceTask {
  taskId: number;
  activityId?: string;
  activityName: string;
  agentKey: string;
  agentNickname: string;
  taskStatus: AgentTaskStatus;
  operationalState: ProcessInstanceOperationalState;
  stateReason: string;
  failureAudit?: AgentTaskFailureAudit;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus: AgentTask["costEstimationStatus"];
  receivedAt?: string;
  deliveredAt?: string;
}

export interface ProcessInstance {
  processDefinitionId: number;
  processCode: string;
  processVersionNumber: number;
  sourceReference: string;
  tasks: ProcessInstanceTask[];
  supersededLegacyTasks: ProcessInstanceTask[];
}
