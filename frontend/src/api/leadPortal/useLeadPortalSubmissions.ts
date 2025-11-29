import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type FlowSubmissionImagePackageStatus =
  | "RECEIVED"
  | "RECENT"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

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
  failureReason?: string | null;
  status: FlowSubmissionImagePackageStatus;
  createdAt: string;
  updatedAt: string;
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
