export type TargetingElementType = "INTEREST" | "JOB_TITLE" | "BEHAVIOR";

export type TargetingElementStatus =
  | "DRAFT"
  | "NEEDS_REVIEW"
  | "APPROVED"
  | "REJECTED";

export type TargetingElementSource = "MANUAL" | "AI";

export interface TargetingElement {
  id: number;
  marketNicheId: number;
  hypothesisId?: string | null;
  type: TargetingElementType;
  term: string;
  description?: string | null;
  prompt?: string | null;
  model?: string | null;
  source?: TargetingElementSource | null;
  status: TargetingElementStatus;
  notes?: string | null;
  lastReviewedBy?: string | null;
  metaId?: string | null;
  metaKey?: string | null;
  confidence?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type TargetingCandidateType = "INTEREST" | "BEHAVIOR" | "WORK_POSITION";

export type TargetingAudienceType = "PROSPECT" | "REMARKETING";

export interface TargetingRequest {
  id: string;
  descricao: string;
  idioma: string;
  pais: string;
  publico_tipo: TargetingAudienceType;
  status: string;
  origin?: string;
  createdAt?: string;
  updatedAt?: string;
  etaSeconds?: number;
}
