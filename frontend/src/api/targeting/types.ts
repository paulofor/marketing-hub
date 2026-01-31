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

export type TargetingCandidateStatus =
  | "PENDING_FACEBOOK_MATCH"
  | "VALIDATED"
  | "NO_MATCH";

export interface TargetingOption {
  id: number;
  facebook_id: string;
  name: string;
  type: TargetingCandidateType;
  audience_size?: number | null;
  match_score?: number | null;
  path: string[];
  search_locale?: string | null;
  search_country?: string | null;
  search_term?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface TargetingCandidate {
  id: number;
  request_id: string;
  texto_sugerido: string;
  tipo: TargetingCandidateType;
  status: TargetingCandidateStatus;
  idioma?: string | null;
  pais?: string | null;
  origem?: string | null;
  intent_tag?: string | null;
  score?: number | null;
  rationale?: string | null;
  rejection_reason?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  options?: TargetingOption[];
}

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
  candidates?: TargetingCandidate[];
}
