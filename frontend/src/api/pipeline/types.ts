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
