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
