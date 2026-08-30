import type {
  AgentTaskAuditLink,
  AgentTaskBlockerGuidance,
  AgentTaskVisualEvidence,
  PsiquePurchaseEmotion,
  PsiqueVisualAudit,
} from "../agentTask/types";

export type ProcessNodeType = "START" | "TASK" | "GATEWAY" | "END";

export type ProcessNode = {
  id: string;
  type: ProcessNodeType;
  label: string;
  owner?: string;
  description?: string;
  executionResourceCode?: string;
  subprocessCode?: string;
  responsibleAgentKeys?: string[];
  documentOutput?: {
    label: string;
  };
};

export type BusinessProcessExecutionResource = {
  id: number;
  resourceCode: string;
  name: string;
  description: string;
  resourceType: string;
  responsibleAgentKey: string;
  executorReference: string;
  usageInstructions: string;
};

export type ProcessFlow = {
  from: string;
  to: string;
  label?: string;
};

export type ProcessDiagram = { nodes: ProcessNode[]; flows: ProcessFlow[] };

export type BusinessProcess = {
  id: number;
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  triggerDescription: string;
  outcomeDescription: string;
  versionNumber: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  technicalReference?: string;
  processType?: "VALUE_PROCESS" | "SUBPROCESS";
  executionScope?: "PRODUCT" | "INDEPENDENT" | "PRODUCT_OR_INDEPENDENT";
  parentProcessCode?: string;
  parentProcessDefinitionId?: number;
  parentProcessName?: string;
  diagram: ProcessDiagram;
  createdAt: string;
  publishedAt?: string;
};

export type BusinessProcessReference = {
  id: number;
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  versionNumber: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  processType: "VALUE_PROCESS" | "SUBPROCESS";
};

export type BusinessProcessComposition = {
  process: BusinessProcessReference;
  parentProcess?: BusinessProcessReference;
  subprocessCount: number;
  subprocesses: BusinessProcessReference[];
};

export type CreateBusinessProcess = Omit<
  BusinessProcess,
  "id" | "status" | "createdAt" | "publishedAt"
>;

export type SaveBusinessProcess = CreateBusinessProcess;

export type IndependentBusinessProcessInputField = {
  key: string;
  label: string;
  controlType: "TEXT" | "TEXTAREA" | "SELECT";
  required: boolean;
  maxLength?: number;
  defaultValue?: string;
  helpText?: string;
  options?: { value: string; label: string }[];
};

export type IndependentBusinessProcessCatalogItem = {
  processDefinitionId: number;
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  triggerDescription: string;
  outcomeDescription: string;
  versionNumber: number;
  executionAvailable: boolean;
  executionAvailabilityReason: string;
  inputFields: IndependentBusinessProcessInputField[];
};

export type IndependentBusinessProcessExecutionSummary = {
  id: number;
  requestKey: string;
  processDefinitionId: number;
  processCode: string;
  processName: string;
  processVersionNumber: number;
  sourceReference: string;
  displayName: string;
  requestedByName: string;
  input: Record<string, string>;
  status:
    | "NOT_STARTED"
    | "PENDING"
    | "IN_PROGRESS"
    | "BLOCKED"
    | "COMPLETED"
    | "CANCELLED";
  activityCount: number;
  completedActivityCount: number;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costCoverage: "COMPLETE" | "PARTIAL" | "NOT_REPORTED";
  latestError?: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
};

export type IndependentBusinessProcessTask = {
  taskId: number;
  status: string;
  assignedAgentKey: string;
  assignedAgentNickname: string;
  title: string;
  result?: unknown;
  evidence?: unknown;
  executionError?: string;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus: string;
  modelCode?: string;
  executionMode?: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
};

export type IndependentBusinessProcessActivity = {
  activityId: string;
  activityName: string;
  status: IndependentBusinessProcessExecutionSummary["status"];
  tasks: IndependentBusinessProcessTask[];
};

export type IndependentBusinessProcessExecution = {
  execution: IndependentBusinessProcessExecutionSummary;
  activities: IndependentBusinessProcessActivity[];
};

export type StartIndependentBusinessProcessExecution = {
  requestKey: string;
  processDefinitionId: number;
  requestedByName: string;
  input: Record<string, string>;
};

export type BusinessProcessActivityDocument = {
  taskId: number;
  title: string;
  sourceReference?: string;
  assignedAgentKey: string;
  assignedAgentNickname: string;
  resultJson?: string;
  evidenceJson?: string;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus: string;
  startedAt?: string;
  finishedAt: string;
  modelCode?: string;
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  reasoningEffort?: string;
  productInternalName?: string;
  promptSent?: string;
  agentPromptPart?: string;
  activityPromptPart?: string;
  accessedUrls?: AgentTaskAuditLink[];
  visualEvidence?: AgentTaskVisualEvidence[];
  visualAudit?: PsiqueVisualAudit;
  purchaseEmotion?: PsiquePurchaseEmotion;
};

export type BusinessProcessActivityExecution = {
  taskId: number;
  processDefinitionId: number;
  processVersionNumber: number;
  title: string;
  status: string;
  sourceReference?: string;
  assignedAgentKey: string;
  assignedAgentNickname: string;
  comments?: string;
  evidenceJson?: string;
  executionError?: string;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
  modelCode?: string;
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  reasoningEffort?: string;
  productInternalName?: string;
  promptSent?: string;
  agentPromptPart?: string;
  activityPromptPart?: string;
  blockerGuidance?: AgentTaskBlockerGuidance;
  accessedUrls?: AgentTaskAuditLink[];
  visualEvidence?: AgentTaskVisualEvidence[];
  visualAudit?: PsiqueVisualAudit;
  purchaseEmotion?: PsiquePurchaseEmotion;
};

export type BusinessProcessActivityExecutionHistory = {
  selectedProcessDefinitionId: number;
  processCode: string;
  processName: string;
  selectedProcessVersionNumber: number;
  selectedProcessStatus: "DRAFT" | "PUBLISHED" | "RETIRED";
  activityId: string;
  activityName: string;
  activityOwnerName?: string;
  executions: BusinessProcessActivityExecution[];
};

export type ProductProcessActivityExecutionGroup = {
  activityDefinitionId?: number;
  activityId: string;
  activityName: string;
  activityObjective?: string;
  activityOwnerName?: string;
  sequenceNumber: number;
  selectedVersionActivity: boolean;
  operationalState:
    | "NOT_STARTED"
    | "PENDING"
    | "IN_PROGRESS"
    | "BLOCKED"
    | "COMPLETED"
    | "CANCELLED";
  stateReason: string;
  objectiveAchieved: boolean;
  stateEvidence:
    | "DIRECT"
    | "MIXED"
    | "BACKFILLED_FROM_TASKS"
    | "LEGACY_TASK"
    | "COMPOSITE_TASK_COVERAGE"
    | "NOT_RECORDED";
  activityInstanceId?: number;
  occurrenceNumber?: number;
  taskCount: number;
  tasks: BusinessProcessActivityExecution[];
  executionRequestAvailable: boolean;
  executionRequestReason: string;
  executionControl?: ProductProcessActivityExecutionControl;
};

export type ProductProcessActivityRequirement = {
  code: string;
  title: string;
  satisfied: boolean;
  detail: string;
  recommendation: string;
};

export type ProductProcessActivityExecutionControl = {
  executorType: "AGENT" | "BACKEND" | "HUMAN" | "HISTORICAL" | "UNCONFIGURED";
  interactionType:
    | "COMMAND"
    | "WORKSPACE"
    | "APPROVAL"
    | "SUBPROCESS"
    | "AUTOMATIC"
    | "STATUS";
  actionLabel?: string;
  description: string;
  actionAvailable: boolean;
  availabilityReason: string;
  confirmationRequired: boolean;
  confirmationTitle?: string;
  confirmationMessage?: string;
  confirmationToken?: string;
  workspaceCode?: "EXPERIMENT_PREFLIGHT" | "EXPERIMENT_ACTIVATION";
  workspaceReferenceId?: number;
  targetProcessDefinitionId?: number;
  requirements: ProductProcessActivityRequirement[];
};

export type ProductProcessActivityHumanDecision = {
  decision: "APPROVE" | "REJECT";
  operatorName: string;
  justification: string;
  evidenceReference: string;
  confirmationToken: string;
};

export type ProductProcessActivityExecutionRequest = {
  processDefinitionId: number;
  productId: number;
  activityId: string;
  sourceReference: string;
  tasks: unknown[];
  operationalState: ProductProcessActivityExecutionGroup["operationalState"];
  objectiveAchieved: boolean;
  message: string;
};

export type ProductProcessActivityExecutionHistory = {
  productId: number;
  productName?: string;
  productInternalName?: string;
  commercialPlanId?: number;
  commercialPlanName?: string;
  selectedProcessDefinitionId: number;
  processCode: string;
  processName: string;
  selectedProcessVersionNumber: number;
  selectedProcessStatus: "DRAFT" | "PUBLISHED" | "RETIRED";
  currentExecutionReference?: string;
  operationalState:
    | "NOT_RECORDED"
    | "NOT_STARTED"
    | "PENDING"
    | "IN_PROGRESS"
    | "BLOCKED"
    | "COMPLETED"
    | "CANCELLED";
  objectiveAchieved: boolean;
  selectedActivityCount: number;
  completedActivityCount: number;
  remainingActivityCount: number;
  blockedActivityCount: number;
  currentActivityId?: string;
  currentActivityName?: string;
  currentActivityState?: ProductProcessActivityExecutionGroup["operationalState"];
  currentActivityStateReason?: string;
  activityCount: number;
  activitiesWithTasksCount: number;
  uniqueTaskCount: number;
  knownEstimatedCostUsd: number;
  costCoverage: "NO_EXECUTIONS" | "NOT_REPORTED" | "PARTIAL" | "COMPLETE";
  activities: ProductProcessActivityExecutionGroup[];
};
