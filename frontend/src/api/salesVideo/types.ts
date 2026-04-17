export type SalesVideoKind = "HERO" | "OBJECTION" | "PROOF";
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
export type SalesVideoJobType = "SCRIPT" | "STORYBOARD" | "RENDER" | "PUBLISH" | "RETRY";
export type SalesVideoRetryReason =
  | "MANUAL_INTERVENTION"
  | "PROVIDER_FAILURE"
  | "ASSET_EXPIRED"
  | "QUALITY_ASSURANCE"
  | "AUTO_RECOVERY"
  | "OTHER";
export type SalesVideoScriptStatus = "DRAFT" | "READY_FOR_REVIEW" | "APPROVED" | "ARCHIVED";
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
