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
export type SalesVideoJobType = "SCRIPT" | "STORYBOARD" | "RENDER" | "PUBLISH" | "RETRY";
export type SalesVideoScriptStatus = "DRAFT" | "READY_FOR_REVIEW" | "APPROVED" | "ARCHIVED";
export type SalesVideoScriptSource = "MANUAL" | "OPENAI";

export interface SalesVideoScript {
  id: number;
  version: number;
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
  providerFamily: SalesVideoProviderFamily;
  providerName?: string | null;
  providerJobId?: string | null;
  jobType: SalesVideoJobType;
  status: SalesVideoStatus;
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
  videoKind: SalesVideoKind;
  title: string;
  personaName?: string | null;
  personaStyle?: string | null;
  voiceStyle?: string | null;
  language?: string | null;
  targetDurationSeconds?: number | null;
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
