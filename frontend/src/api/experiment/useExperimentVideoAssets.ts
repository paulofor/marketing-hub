import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentVideoSlot =
  | "AD"
  | "LANDING_HERO"
  | "FORM_EXPLAINER"
  | "PRE_CHECKOUT";

export type ExperimentVideoStatus = "PLANNED" | "GENERATING" | "READY" | "FAILED";
export type ExperimentVideoReviewStatus = "PENDING" | "APPROVED" | "REJECTED";
export type SalesVideoExecutionMode = "TEST" | "PRODUCTION";

export interface ExperimentVideoAsset {
  id: number;
  experimentId: number;
  slot: ExperimentVideoSlot;
  objective: string;
  primaryMetric: string;
  script?: string | null;
  prompt?: string | null;
  provider: string;
  model: string;
  status: ExperimentVideoStatus;
  assetUrl?: string | null;
  thumbnailUrl?: string | null;
  durationSeconds?: number | null;
  aspectRatio?: string | null;
  requestJson?: string | null;
  responseJson?: string | null;
  cost?: number | null;
  reviewStatus: ExperimentVideoReviewStatus;
  requiredForRelease: boolean;
  salesVideoProfileId?: number | null;
  salesVideoJobId?: number | null;
  assetId?: number | null;
  landingVideoSlotId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface RequestExperimentVeoVideoPayload {
  slot: ExperimentVideoSlot;
  title: string;
  objective: string;
  primaryMetric: string;
  personaName?: string;
  personaStyle?: string;
  voiceStyle?: string;
  language?: string;
  targetDurationSeconds?: number;
  scriptText: string;
  hookText?: string;
  ctaText?: string;
  captionText?: string;
  providerName?: string;
  executionMode?: SalesVideoExecutionMode;
  requestedBy: string;
  requiredForRelease: boolean;
}

export function useExperimentVideoAssets(experimentId?: string | number) {
  return useQuery({
    queryKey: ["experiment-video-assets", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentVideoAsset[]>(
        `/api/experiments/${experimentId}/video-assets`,
      );
      return data;
    },
  });
}
