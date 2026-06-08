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

export interface MoisCreateCollectionJobPayload {
  workspaceId: string;
  niche: string;
  marketTheme?: string;
  sources: string[];
  timeWindow: "LAST_7_DAYS" | "LAST_30_DAYS";
  limitPerSource?: number;
  locale?: string;
  country?: string;
  minSuccessScore?: number;
}

export interface MoisCollectionJob {
  jobId: string;
  workspaceId: string;
  niche: string;
  marketTheme?: string;
  status: string;
  timeWindow: string;
  limitPerSource: number;
  minSuccessScore: number;
  sources: string[];
  createdAt: string;
}

export interface MoisCollectionJobListResponse {
  items: MoisCollectionJob[];
}

export interface MoisCollectedReference {
  referenceId: string;
  jobId: string;
  source: string;
  title: string;
  url: string;
  niche: string;
  status: string;
  favorite: boolean;
  importedReferenceId?: string;
  successScore: number;
  successSignal: string;
  confidenceLevel: string;
  rankingPosition: number;
  engagementRelative: number;
  recurrenceScore: number;
  evidenceScore: number;
  collectedAt: string;
  rawMetadata: Record<string, string>;
}

export interface MoisCollectedReferenceListResponse {
  jobId: string;
  items: MoisCollectedReference[];
}

export interface MoisCollectedReferenceActionResponse {
  jobId: string;
  referenceId: string;
  action: string;
  status: string;
  importedReferenceId?: string;
  extractionId?: string;
  generatedLibraryBlockIds: string[];
  updatedAt: string;
}

export interface MoisCollectedReferenceLineageResponse {
  jobId: string;
  referenceId: string;
  sourceUrl: string;
  importedReferenceId?: string;
  extractionId?: string;
  generatedLibraryBlockIds: string[];
  updatedAt: string;
}

export interface MoisSalesLibraryEntry {
  id: number;
  workspaceId: string;
  source: string;
  urlOriginal: string;
  urlCanonical: string;
  title?: string;
  ingestCount: number;
  firstCapturedAt?: string;
  lastCapturedAt?: string;
  updatedAt: string;
}

export interface MoisSalesLibraryEntryPageResponse {
  page: number;
  pageSize: number;
  total: number;
  items: MoisSalesLibraryEntry[];
}

export interface MoisSalesLibraryJob {
  id: number;
  urlIngestId: number;
  status: string;
  attempts: number;
  errorCategory?: string;
  errorMessage?: string;
  nextRetryAt?: string;
  createdAt: string;
  updatedAt: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface MoisSalesLibraryJobPageResponse {
  page: number;
  pageSize: number;
  total: number;
  items: MoisSalesLibraryJob[];
}

export interface MoisSalesLibraryPage {
  pageId: number;
  workspaceId: string;
  source: string;
  urlCanonical: string;
  title?: string;
  currentStage?: string;
  currentStatus?: string;
  captureStatus?: string;
  analysisStatus?: string;
  urlFinal?: string;
  httpStatus?: number;
  htmlSha256?: string;
  htmlBytes: number;
  scoreTotal?: number;
  offerSummary?: string;
  mechanismSummary?: string;
  promiseSummary?: string;
  proofSummary?: string;
  lastErrorCategory?: string;
  lastErrorMessage?: string;
  lastJobExecutionId?: number;
  lastCapturedAt?: string;
  analyzedAt?: string;
  updatedAt: string;
}

export interface MoisSalesLibraryPageSummary {
  workspaceId: string;
  total: number;
  pending: number;
  capturing: number;
  captured: number;
  analyzed: number;
  analysisPending: number;
  analysisRunning: number;
  analysisFailed: number;
  failed: number;
  blockedCooldown: number;
  hotmart: number;
  clickbank: number;
  updatedAt?: string;
}


export interface MoisCollectedReferenceUrlSourceBreakdown {
  source: string;
  uniqueEffectiveUrls: number;
  operationalLibraryUrls: number;
  missingFromOperationalLibrary: number;
}

export interface MoisCollectedReferenceUrlTypeBreakdown {
  urlType: string;
  uniqueUrls: number;
}

export interface MoisCollectedReferenceUrlSummary {
  workspaceId: string;
  uniqueEffectiveUrls: number;
  explicitSalesPageUrls: number;
  fallbackProductUrls: number;
  operationalLibraryUrls: number;
  missingFromOperationalLibrary: number;
  bySource: MoisCollectedReferenceUrlSourceBreakdown[];
  byUrlType: MoisCollectedReferenceUrlTypeBreakdown[];
}

export interface MoisSalesLibraryPageExecution {
  executionId: number;
  pageId: number;
  jobType: string;
  stage: string;
  status: string;
  attempt: number;
  inputUrl?: string;
  finalUrl?: string;
  redirectRootUrl?: string;
  httpStatus?: number;
  contentType?: string;
  rawHtmlBytes: number;
  screenshotBytes: number;
  scoreTotal?: number;
  errorCategory?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MoisSalesLibraryPageListResponse {
  page: number;
  pageSize: number;
  total: number;
  items: MoisSalesLibraryPage[];
}

export interface MoisSalesLibraryPageAnalysis {
  analysisId: number;
  pageId: number;
  jobId?: number;
  status: string;
  scoreTotal?: number;
  parserVersion?: string;
  promptVersion?: string;
  modelName?: string;
  sectionsJson?: string;
  copyJson?: string;
  visualJson?: string;
  imageJson?: string;
  analysisNotes?: string;
  requestPayloadJson?: string;
  analyzedAt?: string;
  updatedAt: string;
}

export interface MoisSalesLibrarySnapshotCaptureItem {
  pageId: number;
  snapshotId?: number;
  urlCanonical: string;
  redirectDestinationUrl?: string;
  redirectRootUrl?: string;
  status: string;
  snapshotHash?: string;
  httpStatus?: number;
  rawHtmlBytes: number;
  screenshotBytes: number;
  errorMessage?: string;
}

export interface MoisSalesLibrarySnapshotCaptureResponse {
  workspaceId: string;
  requestedLimit: number;
  force: boolean;
  processed: number;
  captured: number;
  failed: number;
  items: MoisSalesLibrarySnapshotCaptureItem[];
  capturedAt: string;
}

export interface MoisSalesLibraryPageSnapshot {
  snapshotId: number;
  pageId: number;
  snapshotHash?: string;
  status: string;
  httpStatus?: number;
  contentType?: string;
  redirectDestinationUrl?: string;
  redirectRootUrl?: string;
  rawHtmlBytes: number;
  screenshotBytes: number;
  capturedAt?: string;
  updatedAt?: string;
}

export interface MoisSalesLibraryReanalyzeResponse {
  pageId: number;
  jobId: number;
  status: string;
  createdAt: string;
}

export interface MoisSalesLibraryStatusUpdateResponse {
  pageId: number;
  jobId?: number;
  status: string;
  reason?: string;
  createdAt: string;
}
