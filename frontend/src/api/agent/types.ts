export interface AgentTheme {
  id: number;
  name: string;
  description?: string;
}

export interface AgentThemePayload {
  name: string;
  description?: string;
}

export interface AgentItem {
  id?: number;
  name: string;
  type?: string;
  description?: string;
  orderIndex?: number;
}

export interface Agent {
  id: number;
  name: string;
  nickname: string;
  portraitAssetId?: number;
  portraitUrl?: string;
  agentKey?: string;
  status: string;
  currentVersion: number;
  ownerName?: string;
  businessObjective?: string;
  successMetrics?: string;
  modelName?: string;
  triggerPolicy?: string;
  authorityPolicy?: string;
  responsibilityContract?: string;
  orchestratorPolicy?: string;
  analysisPolicy?: string;
  offeringPolicy?: string;
  promptContractPath?: string;
  schemaContractPath?: string;
  executionMode: string;
  description?: string;
  themeId: number;
  themeName?: string;
  inputs: AgentItem[];
  outputs: AgentItem[];
  internalFunctions: AgentItem[];
  createdAt?: string;
  updatedAt?: string;
  lastContractChangeAt?: string;
  lastWorkflowRunAt?: string;
  workflowName?: string;
  workflowFile?: string;
  workflowConclusion?: string;
  workflowUrl?: string;
}

export interface AgentExecutionResource {
  id: number;
  resourceCode: string;
  name: string;
  description: string;
  resourceType: string;
  executorReference: string;
  usageInstructions: string;
}

export interface AgentHarnessItem {
  key: string;
  label: string;
  value: string;
  description: string;
  sourceReference: string;
}

export interface AgentHarnessSection {
  code: string;
  title: string;
  description: string;
  items: AgentHarnessItem[];
}

export interface AgentHarnessArtifact {
  artifactType: string;
  name: string;
  version: string;
  path: string;
  description: string;
}

export interface AgentHarness {
  status: "COMPLETE" | "NOT_REGISTERED";
  contractVersion: string;
  sourceReference: string;
  sensitiveValuesPolicy: string;
  sections: AgentHarnessSection[];
  artifacts: AgentHarnessArtifact[];
}

export interface AgentDetail extends Agent {
  automaticExecutionEnabled: boolean;
  automaticExecutionChangedAt?: string;
  automaticExecutionChangedBy?: string;
  executionResources: AgentExecutionResource[];
  harness: AgentHarness;
}

export interface AgentPayload {
  name: string;
  nickname: string;
  portraitAssetId?: number;
  portraitUrl?: string;
  agentKey?: string;
  status: string;
  ownerName?: string;
  businessObjective?: string;
  successMetrics?: string;
  modelName?: string;
  triggerPolicy?: string;
  authorityPolicy?: string;
  responsibilityContract?: string;
  orchestratorPolicy?: string;
  analysisPolicy?: string;
  offeringPolicy?: string;
  promptContractPath?: string;
  schemaContractPath?: string;
  executionMode: string;
  description?: string;
  themeId: number;
  inputs: AgentItem[];
  outputs: AgentItem[];
  internalFunctions: AgentItem[];
}
