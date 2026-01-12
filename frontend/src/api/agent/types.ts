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
  executionMode: string;
  description?: string;
  themeId: number;
  themeName?: string;
  inputs: AgentItem[];
  outputs: AgentItem[];
  internalFunctions: AgentItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentPayload {
  name: string;
  executionMode: string;
  description?: string;
  themeId: number;
  inputs: AgentItem[];
  outputs: AgentItem[];
  internalFunctions: AgentItem[];
}
