export interface PromptDomainObject {
  type: string;
  slug: string;
  label: string;
  contextKey: string;
}

export interface PromptDomain {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  objects: PromptDomainObject[];
  availableVariables: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PromptDomainPayload {
  code?: string;
  name: string;
  description?: string | null;
  objects: string[];
}
