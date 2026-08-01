export type SalesVideoKind = "HERO" | "OBJECTION" | "PROOF";
export type SalesVideoAvatarStrategy =
  | "PLATFORM_TEST_AVATAR"
  | "PROPRIETARY_AVATAR_PLANNED"
  | "PROPRIETARY_AVATAR_READY";
export type SalesVideoStatus =
  | "DRAFT"
  | "SCRIPT_PENDING"
  | "SCRIPT_READY"
  | "STORYBOARD_PENDING"
  | "STORYBOARD_READY"
  | "VIDEO_REQUESTED"
  | "VIDEO_PROCESSING"
  | "VIDEO_READY"
  | "VIDEO_FAILED"
  | "PUBLISHED"
  | "ARCHIVED";
export type SalesVideoProviderFamily = "OPENAI" | "EXTERNAL_VIDEO_MODULE";
export type SalesVideoExecutionMode = "TEST" | "PRODUCTION";
export type SalesVideoJobType =
  "SCRIPT" | "STORYBOARD" | "RENDER" | "POST_PRODUCTION" | "PUBLISH" | "RETRY";
export type SalesVideoRetryReason =
  | "MANUAL_INTERVENTION"
  | "PROVIDER_FAILURE"
  | "ASSET_EXPIRED"
  | "QUALITY_ASSURANCE"
  | "AUTO_RECOVERY"
  | "OTHER";
export type SalesVideoScriptStatus =
  "DRAFT" | "READY_FOR_REVIEW" | "APPROVED" | "ARCHIVED";
export type SalesVideoScriptSource = "MANUAL" | "OPENAI";

export interface SalesVideoScript {
  id: number;
  version: number;
  createdBy?: string | null;
  scriptText?: string | null;
  hookText?: string | null;
  ctaText?: string | null;
  captionText?: string | null;
  storyboardJson?: string | null;
  source: SalesVideoScriptSource;
  model?: string | null;
  prompt?: string | null;
  status: SalesVideoScriptStatus;
  approvedBy?: string | null;
  approvedAt?: string | null;
  createdAt?: string | null;
}

export interface SalesVideoJob {
  id: number;
  profileId: number;
  scriptId?: number | null;
  tenantId?: string | null;
  providerFamily: SalesVideoProviderFamily;
  executionMode?: SalesVideoExecutionMode | null;
  providerName?: string | null;
  providerJobId?: string | null;
  streamPlaybackUrl?: string | null;
  jobType: SalesVideoJobType;
  status: SalesVideoStatus;
  retryAttempt?: number | null;
  retryReason?: SalesVideoRetryReason | null;
  retryOfJobId?: number | null;
  retryNotes?: string | null;
  progressPercent?: number | null;
  failureCode?: string | null;
  failureDetail?: string | null;
  requestedBy?: string | null;
  requestedAt?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  expiresAt?: string | null;
  assetId?: number | null;
  posterAssetId?: number | null;
  vttAssetId?: number | null;
  metadataJson?: string | null;
  auditSnapshotJson?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SalesVideoProfile {
  id: number;
  productId: number;
  landingPageId?: number | null;
  tenantId?: string | null;
  createdBy?: string | null;
  videoKind: SalesVideoKind;
  title: string;
  avatarStrategy: SalesVideoAvatarStrategy;
  personaName?: string | null;
  personaStyle?: string | null;
  voiceStyle?: string | null;
  language?: string | null;
  targetDurationSeconds?: number | null;
  requiresConsent: boolean;
  consentRecordedBy?: string | null;
  consentRecordedAt?: string | null;
  consentEvidenceUrl?: string | null;
  humanReviewApprovedBy?: string | null;
  humanReviewApprovedAt?: string | null;
  complianceNotes?: string | null;
  status: SalesVideoStatus;
  createdAt?: string | null;
  updatedAt?: string | null;
  latestScript?: SalesVideoScript | null;
  lastJob?: SalesVideoJob | null;
}

export interface SalesVideoJobEvent {
  id: number;
  eventType: string;
  oldStatus?: SalesVideoStatus | null;
  newStatus?: SalesVideoStatus | null;
  message?: string | null;
  detailsJson?: string | null;
  createdAt?: string | null;
}

export interface LandingVideoSlot {
  id: number;
  landingPageId: number;
  profileId: number;
  tenantId?: string | null;
  slotName: string;
  assetId: number;
  posterAssetId?: number | null;
  vttAssetId?: number | null;
  assetUrl?: string | null;
  posterAssetUrl?: string | null;
  vttAssetUrl?: string | null;
  autoplay: boolean;
  muted: boolean;
  loopVideo: boolean;
  controlsEnabled: boolean;
  lazyLoad: boolean;
  publishedAt?: string | null;
  publishedBy?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateSalesVideoProfilePayload {
  videoKind: SalesVideoKind;
  title: string;
  avatarStrategy?: SalesVideoAvatarStrategy;
  personaName?: string;
  personaStyle?: string;
  voiceStyle?: string;
  language?: string;
  targetDurationSeconds?: number;
  landingPageId?: number;
}

export interface GenerateSalesVideoScriptPayload {
  requestedBy: string;
  providerName?: string;
}

export interface ApproveSalesVideoScriptPayload {
  scriptText: string;
  hookText?: string;
  ctaText?: string;
  captionText?: string;
  approvedBy: string;
}

export interface RequestVideoRenderPayload {
  requestedBy: string;
  providerFamily?: SalesVideoProviderFamily;
  providerName?: string;
  executionMode?: SalesVideoExecutionMode;
  metadataJson?: string;
}

export interface RequestSalesVideoPostProductionPayload {
  requestedBy: string;
  sourceVideoUrl?: string;
  voiceOverScript: string;
  captionText: string;
}

export interface RequestSalesVideoMontagePayload {
  requestedBy: string;
  sourceJobIds: number[];
}

export interface UpdateSalesVideoCompliancePayload {
  requiresConsent?: boolean;
  consentRecordedBy?: string;
  consentEvidenceUrl?: string;
  humanReviewApproved?: boolean;
  humanReviewApprovedBy?: string;
  complianceNotes?: string;
}

export interface CreateLandingVideoSlotPayload {
  profileId: number;
  slotName: string;
  assetId: number;
  posterAssetId?: number;
  vttAssetId?: number;
  autoplay?: boolean;
  muted?: boolean;
  loopVideo?: boolean;
  controlsEnabled?: boolean;
  lazyLoad?: boolean;
  publishedBy?: string;
}

export interface UpdateLandingVideoSlotPayload {
  profileId?: number;
  slotName?: string;
  assetId?: number;
  posterAssetId?: number;
  vttAssetId?: number;
  autoplay?: boolean;
  muted?: boolean;
  loopVideo?: boolean;
  controlsEnabled?: boolean;
  lazyLoad?: boolean;
  publishedBy?: string;
}

export interface LandingVideoSlotHistory {
  id: number;
  slotId: number;
  profileId?: number | null;
  landingPageId?: number | null;
  tenantId?: string | null;
  slotName: string;
  assetId?: number | null;
  posterAssetId?: number | null;
  vttAssetId?: number | null;
  autoplay: boolean;
  muted: boolean;
  loopVideo: boolean;
  controlsEnabled: boolean;
  lazyLoad: boolean;
  changeType: string;
  changedBy?: string | null;
  changedAt?: string | null;
  publishedBy?: string | null;
  publishedAt?: string | null;
  notes?: string | null;
}

export type SalesVideoConversionEventType =
  "VIEW" | "LEAD" | "QUALIFIED_LEAD" | "CHECKOUT_STARTED" | "PURCHASE";

export interface SalesVideoCommercialPlaybook {
  id: number;
  profileId: number;
  tenantId?: string | null;
  nicheKey: string;
  variantKey: string;
  objectionText: string;
  ctaText: string;
  funnelRole?: string | null;
  promiseToVisualize?: string | null;
  visualPain?: string | null;
  mainScene?: string | null;
  subjectDescription?: string | null;
  motionDescription?: string | null;
  cameraFraming?: string | null;
  lightingStyle?: string | null;
  expectedEmotion?: string | null;
  transitionOrCta?: string | null;
  qualityConstraints?: string | null;
  cinematicPrompt?: string | null;
  active: boolean;
  createdBy?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateSalesVideoCommercialPlaybookPayload {
  nicheKey: string;
  variantKey: string;
  objectionText: string;
  ctaText: string;
  funnelRole?: string;
  promiseToVisualize?: string;
  visualPain?: string;
  mainScene?: string;
  subjectDescription?: string;
  motionDescription?: string;
  cameraFraming?: string;
  lightingStyle?: string;
  expectedEmotion?: string;
  transitionOrCta?: string;
  qualityConstraints?: string;
  cinematicPrompt?: string;
  active?: boolean;
  createdBy: string;
}

export interface SalesVideoConversionEvent {
  id: number;
  profileId: number;
  scriptId?: number | null;
  jobId?: number | null;
  eventType: SalesVideoConversionEventType;
  eventValue?: number | null;
  currency?: string | null;
  occurredAt: string;
  source?: string | null;
  metadataJson?: string | null;
}

export interface CreateSalesVideoConversionEventPayload {
  scriptId?: number;
  jobId?: number;
  eventType: SalesVideoConversionEventType;
  eventValue?: number;
  currency?: string;
  occurredAt?: string;
  source?: string;
  metadataJson?: string;
}

export interface SalesVideoVariantPerformance {
  variantKey: string;
  scriptId?: number | null;
  providerName?: string | null;
  views: number;
  leads: number;
  qualifiedLeads: number;
  checkoutStarted: number;
  purchases: number;
  revenue: number;
  conversionRatePercent: number;
}

export interface SalesVideoProviderScore {
  providerName: string;
  score: number;
  readyJobs: number;
  failedJobs: number;
  operationalFailedJobs?: number;
  approvedAssets: number;
  rejectedAssets: number;
  leads: number;
  qualifiedLeads: number;
  checkoutStarts: number;
  purchases: number;
  revenue: number;
  recommendation: string;
  riskCategory?: string | null;
  riskMessage?: string | null;
}

export interface SalesVideoPerformanceSummary {
  profileId: number;
  from?: string | null;
  to?: string | null;
  totalViews: number;
  totalLeads: number;
  totalQualifiedLeads: number;
  totalCheckoutStarted: number;
  totalPurchases: number;
  totalRevenue: number;
  variants: SalesVideoVariantPerformance[];
  providerScores: SalesVideoProviderScore[];
}

export type VideoProjectStatus =
  | "DRAFT"
  | "READY_FOR_SCRIPT"
  | "READY_FOR_RENDER"
  | "IN_PRODUCTION"
  | "READY_FOR_REVIEW"
  | "APPROVED"
  | "ARCHIVED";

export interface VideoProject {
  id: number;
  tenantId?: string | null;
  productId?: number | null;
  experimentId?: number | null;
  salesVideoProfileId?: number | null;
  campaignKey?: string | null;
  videoCategory?: string | null;
  contextType: string;
  productionMode: string;
  targetChannel: string;
  format: string;
  title: string;
  objective: string;
  storyText?: string | null;
  funnelStage?: string | null;
  primaryMetric?: string | null;
  hookText?: string | null;
  scriptText?: string | null;
  scenePlan?: string | null;
  visualReferences?: string | null;
  characterBible?: string | null;
  environmentBible?: string | null;
  objectBible?: string | null;
  visualStyleGuide?: string | null;
  imageGenerationPlan?: string | null;
  continuityRules?: string | null;
  voiceoverPlan?: string | null;
  soundtrackPlan?: string | null;
  captionPlan?: string | null;
  ctaText?: string | null;
  targetDurationSeconds?: number | null;
  providerPlan?: string | null;
  editingNotes?: string | null;
  qualityGate?: string | null;
  status: VideoProjectStatus;
  createdBy?: string | null;
  updatedBy?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface VideoProjectPayload {
  productId?: number | null;
  experimentId?: number | null;
  salesVideoProfileId?: number | null;
  campaignKey?: string;
  videoCategory?: string;
  contextType: string;
  productionMode: string;
  targetChannel: string;
  format: string;
  title: string;
  objective: string;
  storyText?: string;
  funnelStage?: string;
  primaryMetric?: string;
  hookText?: string;
  scriptText?: string;
  scenePlan?: string;
  visualReferences?: string;
  characterBible?: string;
  environmentBible?: string;
  objectBible?: string;
  visualStyleGuide?: string;
  imageGenerationPlan?: string;
  continuityRules?: string;
  voiceoverPlan?: string;
  soundtrackPlan?: string;
  captionPlan?: string;
  ctaText?: string;
  targetDurationSeconds?: number | null;
  providerPlan?: string;
  editingNotes?: string;
  qualityGate?: string;
  status?: VideoProjectStatus;
  createdBy?: string;
  updatedBy?: string;
}
