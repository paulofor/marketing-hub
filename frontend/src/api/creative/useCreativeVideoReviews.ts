import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useLocation, matchPath } from "react-router-dom";

export type CreativeVideoReviewStatus = "DRAFT" | "READY" | "REJECTED";
export type CreativeVideoReviewSourceType =
  | "CREATIVE"
  | "EXPERIMENT_VIDEO_ASSET";
export type CreativeAgentReviewStatus =
  | "PENDING"
  | "PROCESSING"
  | "APPROVED"
  | "ADJUST"
  | "REJECTED"
  | "FAILED";

export type VideoReviewState =
  | "AWAITING_REVIEW"
  | "BLOCKED"
  | "HISTORICAL"
  | "APPROVED"
  | "REJECTED";
export interface VideoReviewScope {
  productId?: number;
  experimentId?: number;
}
export interface VideoReviewSummary {
  productId: number | null;
  experimentId: number | null;
  awaitingReviewCount: number;
  blockedCount: number;
  historicalCount: number;
  approvedCount: number;
  rejectedCount: number;
}

/** Extrai somente a identidade da navegação; o backend decide a elegibilidade. */
export function useVideoReviewScope(): VideoReviewScope {
  const { pathname, search } = useLocation();
  const params = new URLSearchParams(search);
  const product = matchPath("/products/:productId/*", pathname)?.params
    .productId;
  const experiment = matchPath("/experiments/:experimentId/*", pathname)?.params
    .experimentId;
  const reviewPage = pathname === "/creative-video-review";
  const positiveId = (value: string | null | undefined) =>
    value && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))
      ? Number(value)
      : undefined;
  return {
    productId: positiveId(
      product ?? (reviewPage ? params.get("productId") : undefined),
    ),
    experimentId: positiveId(
      experiment ??
        (product
          ? params.get("sourceReference")?.match(/^experiment:(\d+)$/)?.[1]
          : reviewPage
            ? params.get("experimentId")
            : undefined),
    ),
  };
}

/** Consulta as contagens oficiais sem contar rascunhos como decisões humanas. */
export function useVideoReviewSummary(scope: VideoReviewScope) {
  return useQuery({
    queryKey: ["creative-video-reviews", "summary", scope],
    queryFn: async () =>
      (
        await axios.get<VideoReviewSummary>(
          "/api/creatives/video-review/summary",
          { params: scope },
        )
      ).data,
    refetchInterval: 30000,
  });
}

export interface CreativeVideoReview {
  id: number;
  productId?: number | null;
  productName?: string | null;
  eligibility: {
    state: VideoReviewState;
    reason: string;
    approvalAvailable: boolean;
    agentReviewRequestAvailable: boolean;
  };
  sourceType: CreativeVideoReviewSourceType;
  funnelSlot?: "AD" | "LANDING_HERO" | "FORM_EXPLAINER" | "PRE_CHECKOUT" | null;
  experimentId: number;
  experimentName: string;
  experimentStatus: string;
  hypothesisId?: string | null;
  hypothesisTitle?: string | null;
  hypothesisStatus?: string | null;
  nicheId?: number | null;
  nicheName?: string | null;
  format: string;
  headline: string;
  primaryText: string;
  videoId?: string | null;
  videoUrl?: string | null;
  description?: string | null;
  cta?: string | null;
  destinationUrl?: string | null;
  status: CreativeVideoReviewStatus;
  agentReviewStatus?: CreativeAgentReviewStatus | null;
  agentReviewSummary?: string | null;
  approvalBlockedReason?: string | null;
  rejectionReason?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
  videoCostUsd?: number | string | null;
  audioCostUsd?: number | string | null;
  totalProductionCostUsd?: number | string | null;
  visualSourceType?: string | null;
  visualSourceKey?: string | null;
  visualSourceDescription?: string | null;
  visualSimilarityOverrideReason?: string | null;
}

export function useCreativeVideoReviews(
  status?: CreativeVideoReviewStatus | "ALL",
  scope: VideoReviewScope = {},
  state?: VideoReviewState,
) {
  return useQuery({
    queryKey: ["creative-video-reviews", status ?? "ALL", scope, state],
    queryFn: async () => {
      const { data } = await axios.get<CreativeVideoReview[]>(
        "/api/creatives/video-review",
        {
          params: {
            ...scope,
            ...(status && status !== "ALL" ? { status } : {}),
            ...(state ? { state } : {}),
          },
        },
      );
      return data;
    },
    refetchInterval: (query) =>
      query.state.data?.some(
        (video) =>
          video.agentReviewStatus === "PENDING" ||
          video.agentReviewStatus === "PROCESSING",
      )
        ? 5000
        : false,
  });
}

export function useUpdateCreativeVideoReviewStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      sourceType,
      status,
      rejectionReason,
    }: {
      id: number;
      sourceType: CreativeVideoReviewSourceType;
      status: CreativeVideoReviewStatus;
      rejectionReason?: string;
    }) => {
      const { data } = await axios.patch(
        `/api/creatives/video-review/${sourceType}/${id}/status`,
        {
          status,
          rejectionReason,
        },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creative-video-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["creatives"] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}

/** Reenvia um anúncio ao parecer independente sem alterar a decisão humana. */
export function useRequestCreativeVideoAgentReview() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const { data } = await axios.post(
        `/api/creatives/${id}/agent-review/request`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creative-video-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["creatives"] });
    },
  });
}
