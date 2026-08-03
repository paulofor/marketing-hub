import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type SocialPlatform = "YOUTUBE" | "INSTAGRAM" | "TIKTOK";
export type SocialAccountStatus = "SETUP_REQUIRED" | "CONNECTED" | "DISABLED";
export type SocialConnectionMode = "OAUTH" | "MANUAL_REFERENCE";
export type SocialVideoFormat =
  "YOUTUBE_SHORT" | "INSTAGRAM_REELS" | "TIKTOK_DRAFT";
export type SocialVideoPublicationStatus =
  "DRAFT" | "QUEUED" | "BLOCKED" | "PUBLISHING" | "PUBLISHED" | "FAILED";

export interface SocialAccount {
  id: number;
  platform: SocialPlatform;
  displayName: string;
  handle?: string;
  externalAccountId?: string;
  connectionMode: SocialConnectionMode;
  status: SocialAccountStatus;
  requiredScopes?: string;
  setupNotes?: string;
  connectedAt?: string;
}

export interface SocialPublicationMetric {
  id: number;
  views?: number;
  engagedViews?: number;
  averageViewDurationSeconds?: number;
  recurringViewers?: number;
  subscribersGained?: number;
  likes?: number;
  comments?: number;
  shares?: number;
  clicks?: number;
  landingSessions?: number;
  leads?: number;
  checkoutsStarted?: number;
  salesApproved?: number;
  revenue?: number;
  rawPayloadJson?: string;
  capturedAt: string;
}

export interface SocialVideoPublication {
  id: number;
  productId: number;
  productName?: string;
  productSlug?: string;
  assetId?: number;
  growthContentId?: number;
  socialAccountId?: number;
  socialAccountName?: string;
  socialAccountExternalAccountId?: string;
  platform: SocialPlatform;
  videoFormat: SocialVideoFormat;
  status: SocialVideoPublicationStatus;
  title: string;
  caption?: string;
  hashtags?: string;
  videoUrl?: string;
  publishedUrl?: string;
  externalPostId?: string;
  failureReason?: string;
  publishPayloadJson?: string;
  scheduledAt?: string;
  queuedAt?: string;
  publishedAt?: string;
  latestMetric?: SocialPublicationMetric;
}

export interface SaveSocialAccountRequest {
  platform: SocialPlatform;
  displayName: string;
  handle?: string;
  externalAccountId?: string;
  connectionMode: SocialConnectionMode;
  status: SocialAccountStatus;
  setupNotes?: string;
}

export interface CreateSocialVideoPublicationRequest {
  productId: number;
  growthContentId?: number;
  assetId?: number;
  socialAccountId?: number;
  platform: SocialPlatform;
  videoFormat: SocialVideoFormat;
  title: string;
  caption?: string;
  hashtags?: string;
  videoUrl?: string;
  scheduledAt?: string;
}

export interface MarkPublishedRequest {
  publishedUrl: string;
  externalPostId?: string;
}

export interface RecordMetricRequest {
  views?: number;
  likes?: number;
  comments?: number;
  shares?: number;
  clicks?: number;
  engagedViews?: number;
  averageViewDurationSeconds?: number;
  recurringViewers?: number;
  subscribersGained?: number;
  landingSessions?: number;
  leads?: number;
  checkoutsStarted?: number;
  salesApproved?: number;
  revenue?: number;
  rawPayloadJson?: string;
}

export interface SocialGrowthContent {
  id: number;
  contentType: "SHORT" | "LONG_VIDEO";
  pillar: string;
  topic: string;
  funnelStage: string;
  cta: string;
  trackingCode: string;
  trackingUrl: string;
  status: "DRAFT" | "APPROVED" | "PUBLISHED";
  plannedAt?: string;
  publicationId?: number;
}

export interface SocialGrowthPlan {
  id: number;
  productId: number;
  productName: string;
  name: string;
  audience: string;
  commercialHypothesis: string;
  commercialObjective: string;
  primaryCta: string;
  destinationUrl: string;
  utmCampaign: string;
  status: "DRAFT" | "ACTIVE" | "PAUSED" | "COMPLETED";
  startsOn?: string;
  endsOn?: string;
  contents: SocialGrowthContent[];
  performance: {
    views: number;
    engagedViews: number;
    recurringViewers: number;
    landingSessions: number;
    leads: number;
    checkoutsStarted: number;
    salesApproved: number;
    revenue: number;
    decision: string;
    decisionReason: string;
  };
}

export interface CreateSocialGrowthPlanRequest {
  productId: number;
  name: string;
  audience: string;
  commercialHypothesis: string;
  commercialObjective: string;
  primaryCta: string;
  destinationUrl: string;
  utmCampaign: string;
  startsOn?: string;
  endsOn?: string;
}

export interface CreateSocialGrowthContentRequest {
  contentType: "SHORT" | "LONG_VIDEO";
  pillar: string;
  topic: string;
  funnelStage: string;
  cta?: string;
  plannedAt?: string;
}

export function useSocialAccounts(platform?: SocialPlatform) {
  return useQuery({
    queryKey: ["social-distribution", "accounts", platform ?? "ALL"],
    queryFn: async () => {
      const { data } = await axios.get<SocialAccount[]>(
        "/api/social-distribution/accounts",
        { params: platform ? { platform } : undefined },
      );
      return data;
    },
  });
}

export function useSocialPublications(productId?: string) {
  return useQuery({
    queryKey: ["social-distribution", "publications", productId ?? "ALL"],
    queryFn: async () => {
      const { data } = await axios.get<SocialVideoPublication[]>(
        "/api/social-distribution/publications",
        { params: productId ? { productId } : undefined },
      );
      return data;
    },
  });
}

export function useSocialGrowthPlans(productId?: string) {
  return useQuery({
    queryKey: ["social-distribution", "growth-plans", productId ?? "ALL"],
    queryFn: async () => {
      const { data } = await axios.get<SocialGrowthPlan[]>(
        "/api/social-distribution/growth-plans",
        { params: productId ? { productId } : undefined },
      );
      return data;
    },
  });
}

export function useCreateSocialGrowthPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: CreateSocialGrowthPlanRequest) => {
      const { data } = await axios.post<SocialGrowthPlan>(
        "/api/social-distribution/growth-plans",
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "growth-plans"],
      }),
  });
}

export function useCreateSocialGrowthContent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      planId,
      request,
    }: {
      planId: number;
      request: CreateSocialGrowthContentRequest;
    }) => {
      const { data } = await axios.post<SocialGrowthContent>(
        `/api/social-distribution/growth-plans/${planId}/contents`,
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "growth-plans"],
      }),
  });
}

export function useApproveSocialGrowthContent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      planId,
      contentId,
    }: {
      planId: number;
      contentId: number;
    }) => {
      const { data } = await axios.post<SocialGrowthContent>(
        `/api/social-distribution/growth-plans/${planId}/contents/${contentId}/approve`,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "growth-plans"],
      }),
  });
}

export function useCreateSocialAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: SaveSocialAccountRequest) => {
      const { data } = await axios.post<SocialAccount>(
        "/api/social-distribution/accounts",
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "accounts"],
      }),
  });
}

export function useCreateSocialPublication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: CreateSocialVideoPublicationRequest) => {
      const { data } = await axios.post<SocialVideoPublication>(
        "/api/social-distribution/publications",
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "publications"],
      }),
  });
}

export function useQueueSocialPublication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (publicationId: number) => {
      const { data } = await axios.post<SocialVideoPublication>(
        `/api/social-distribution/publications/${publicationId}/queue`,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "publications"],
      }),
  });
}

export function useMarkSocialPublicationPublished() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      publicationId,
      request,
    }: {
      publicationId: number;
      request: MarkPublishedRequest;
    }) => {
      const { data } = await axios.post<SocialVideoPublication>(
        `/api/social-distribution/publications/${publicationId}/published`,
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "publications"],
      }),
  });
}

export function useRecordSocialPublicationMetric() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      publicationId,
      request,
    }: {
      publicationId: number;
      request: RecordMetricRequest;
    }) => {
      const { data } = await axios.post<SocialPublicationMetric>(
        `/api/social-distribution/publications/${publicationId}/metrics`,
        request,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["social-distribution", "publications"],
      }),
  });
}
