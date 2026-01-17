export interface Prompt {
  id: number;
  name: string;
  domain: string;
  template: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromptPayload {
  name: string;
  domain: string;
  template: string;
  active?: boolean;
}

export interface PromptTemplateValidationRequest {
  domain: string;
  template: string;
}

export interface PromptTemplateValidationResponse {
  valid: boolean;
  message: string;
  renderedPrompt?: string | null;
  missingVariables: string[];
  availableVariables: string[];
}
