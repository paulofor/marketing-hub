export type ProcessNodeType = "START" | "TASK" | "GATEWAY" | "END";

export type ProcessNode = {
  id: string;
  type: ProcessNodeType;
  label: string;
  owner?: string;
  description?: string;
  executionResourceCode?: string;
  subprocessCode?: string;
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
  reasoningEffort?: string;
  productInternalName?: string;
  promptSent?: string;
};
