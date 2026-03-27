export type ProofStage = "AD" | "LANDING" | "SAMPLE" | "SALES";
export type ProofStatus = "DRAFT" | "APPROVED" | "ARCHIVED";

export interface ProofArtifact {
  id: number;
  hypothesisId?: string | null;
  experimentId?: number | null;
  marketNicheId?: number | null;
  marketNicheName?: string | null;
  visualProofId?: number | null;
  visualProofName?: string | null;
  stage: ProofStage;
  stageLabel?: string;
  status: ProofStatus;
  customType?: string | null;
  typeLabel?: string | null;
  assetPlan?: string | null;
  assetUrl?: string | null;
  message?: string | null;
  deliveryNotes?: string | null;
  prompt?: string | null;
  model?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateProofPayload {
  visualProofId?: number | null;
  customType?: string;
  stage?: ProofStage;
  status?: ProofStatus;
  assetPlan?: string;
  assetUrl?: string;
  message?: string;
  deliveryNotes?: string;
  prompt?: string;
  model?: string;
}

export interface UpdateProofPayload extends CreateProofPayload {
  id: number;
}
