import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type FlowSubmissionImagePackageStatus =
  | "RECEIVED"
  | "RECENT"
  | "PROCESSING"
  | "WATERMARK_PENDING"
  | "WATERMARKING"
  | "COMPLETED"
  | "FAILED";

export type LeadPortalImagePackageLifecycleStatus =
  | FlowSubmissionImagePackageStatus
  | "ZIP_GENERATING"
  | "SAMPLE_EMAIL_SENDING"
  | "SAMPLE_EMAIL_SENT"
  | "SAMPLE_EMAIL_OPENED"
  | "SAMPLE_IMAGES_VIEWED";

export interface LeadPortalImagePackage {
  id: number;
  submissionId: string;
  flowSlug?: string | null;
  name?: string | null;
  email?: string | null;
  phone?: string | null;
  prompt: string;
  model?: string | null;
  plannedOutputs?: number | null;
  freeImages?: number | null;
  generatedImageCount: number;
  watermarkedImageCount: number;
  failureReason?: string | null;
  status: FlowSubmissionImagePackageStatus;
  lifecycleStatus?: LeadPortalImagePackageLifecycleStatus | null;
  createdAt: string;
  updatedAt: string;
  imageModelId?: number | null;
  imageModelName?: string | null;
  imageModelQualityId?: number | null;
  imageModelQualityName?: string | null;
  imageOrientation?: string | null;
  imageWidth?: number | null;
  imageHeight?: number | null;
  imageUnitPriceUsd?: number | null;
  imageTotalPriceUsd?: number | null;
  imageCurrency?: string | null;
}

function buildParams(statuses?: FlowSubmissionImagePackageStatus[]) {
  if (!statuses || statuses.length === 0) {
    return undefined;
  }
  const params = new URLSearchParams();
  statuses.forEach((status) => params.append("status", status));
  return params;
}

export function useLeadPortalImagePackages(statuses?: FlowSubmissionImagePackageStatus[]) {
  const sortedStatuses = statuses ? [...statuses].sort() : [];

  return useQuery<LeadPortalImagePackage[], Error>({
    queryKey: ["lead-portal-image-packages", sortedStatuses],
    queryFn: async () => {
      const params = buildParams(sortedStatuses);
      const { data } = await axios.get<LeadPortalImagePackage[]>(
        "/api/lead-portal/image-packages",
        { params },
      );
      return data;
    },
    staleTime: 30_000,
  });
}
