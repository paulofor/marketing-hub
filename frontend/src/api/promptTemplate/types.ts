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
