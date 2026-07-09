export interface AiPromptSchemaTemplate {
  templateKey: string;
  pipelineCode: string;
  stageCode: string;
  version: string;
  openAiModel: string;
  schemaName: string;
  promptMarkdownContent: string;
  schemaJson: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateAiPromptSchemaTemplatePayload {
  version: string;
  openAiModel: string;
  schemaName: string;
  promptMarkdownContent: string;
  schemaJson: string;
  active: boolean;
}
