export interface MoisWorkspaceKpis {
  collections: number;
  extractions: number;
  applications: number;
  tests: number;
}

export interface MoisRecentAnalysis {
  analysisId: string;
  niche: string;
  status: string;
  updatedAt: string;
}

export interface MoisWorkspaceDashboard {
  workspaceId: string;
  kpis: MoisWorkspaceKpis;
  currentStage: string;
  recentAnalyses: MoisRecentAnalysis[];
}

export interface MoisCreateReferencePayload {
  workspaceId: string;
  niche: string;
  sourceUrl: string;
  assetType: string;
  primaryPromise: string;
  awarenessStage: string;
  priceRange?: string;
  formatType?: string;
  notes?: string;
}

export interface MoisReference {
  referenceId: string;
  workspaceId: string;
  niche: string;
  sourceUrl: string;
  assetType: string;
  primaryPromise: string;
  awarenessStage: string;
  priceRange?: string;
  formatType?: string;
  notes?: string;
  createdAt: string;
}

export interface MoisReferenceListResponse {
  items: MoisReference[];
}

export interface MoisExtractionDraftPayload {
  pain: string;
  result: string;
  mechanism: string;
  proof: string;
  offer: string;
  evidenceItems: string[];
}

export interface MoisExtractionDraftResponse {
  extractionId: string;
  referenceId: string;
  status: string;
  updatedAt: string;
}

export interface MoisLibraryBlock {
  blockId: string;
  workspaceId: string;
  type: string;
  summary: string;
  tags: string[];
  score: number;
  origin: string;
  favorite: boolean;
  updatedAt: string;
}

export interface MoisLibraryBlockListResponse {
  items: MoisLibraryBlock[];
}

export interface MoisLibraryActionResponse {
  blockId: string;
  action: string;
  status: string;
  updatedAt: string;
}

export interface MoisComparisonRequest {
  workspaceId: string;
  referenceBaseId: string;
  currentOfferId: string;
}

export interface MoisComparisonDimension {
  dimension: string;
  market: string;
  current: string;
  highlight: string;
}

export interface MoisComparisonScorecard {
  metric: string;
  value: number;
  explanation: string;
}

export interface MoisComparisonImprovement {
  improvementId: string;
  priority: string;
  description: string;
}

export interface MoisComparisonResponse {
  comparisonId: string;
  workspaceId: string;
  dimensions: MoisComparisonDimension[];
  scorecards: MoisComparisonScorecard[];
  improvements: MoisComparisonImprovement[];
}

export interface MoisBuildOfferRequest {
  workspaceId: string;
  currentOfferId: string;
  selectedBlockIds: string[];
  currentVersion: string;
}

export interface MoisBuildOfferResponse {
  offerId: string;
  workspaceId: string;
  status: string;
  proposedVersion: string;
  checklist: Record<string, boolean>;
  updatedAt: string;
}
