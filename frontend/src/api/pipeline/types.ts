export interface PipelineStage {
  id: number;
  pipelineId: number;
  position: number;
  name: string;
  code: string;
  description?: string | null;
  required: boolean;
  active: boolean;
  openAiModelId?: number | null;
  openAiModelName?: string | null;
  openAiModelCode?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Pipeline {
  id: number;
  name: string;
  code: string;
  module: string;
  description?: string | null;
  active: boolean;
  stages: PipelineStage[];
  createdAt?: string;
  updatedAt?: string;
}

export type PipelinePayload = Pick<
  Pipeline,
  "name" | "code" | "module" | "description" | "active"
>;

export type PipelineStagePayload = Pick<
  PipelineStage,
  | "position"
  | "name"
  | "code"
  | "description"
  | "required"
  | "active"
  | "openAiModelId"
>;

export interface OfficialPipelineStageMetadata {
  canonicalCode: string;
  operationalCode: string;
  name: string;
  position: number;
  required: boolean;
  configurable: boolean;
  aliases: string[];
}

export interface OfficialPipelineMetadata {
  module: string;
  code: string;
  name: string;
  official: boolean;
  aliases: string[];
  stages: OfficialPipelineStageMetadata[];
}

export interface PipelineMetadata {
  validModules: string[];
  officialPipelines: OfficialPipelineMetadata[];
}

export type PipelineDiagnosticsStatus = "OK" | "ATENÇÃO" | "BLOQUEADO";

export interface PipelineDiagnosticsIssue {
  severity: "ERROR" | "WARN" | "INFO";
  stageCode?: string | null;
  canonicalCode?: string | null;
  message: string;
  rootCause: string;
  recommendedAction: string;
}

export interface PipelineDiagnostics {
  pipelineId: number;
  pipelineCode: string;
  canonicalPipelineCode?: string | null;
  status: PipelineDiagnosticsStatus;
  expectedStages: number;
  configuredStages: number;
  issues: PipelineDiagnosticsIssue[];
}

export interface PipelineSyncResult {
  status: PipelineDiagnosticsStatus;
  synchronizedSafely: boolean;
  pipelineId?: number | null;
  pipelineCode?: string | null;
  canonicalPipelineCode?: string | null;
  expectedStages: number;
  configuredStages: number;
  appliedActions: string[];
  issues: PipelineDiagnosticsIssue[];
}
