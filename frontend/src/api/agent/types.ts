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
