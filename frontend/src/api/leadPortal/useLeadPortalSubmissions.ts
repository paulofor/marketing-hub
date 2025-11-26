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
  status: FlowSubmissionImagePackageStatus;
  createdAt: string;
}

export function useLeadPortalImagePackages() {
  return useQuery<LeadPortalImagePackage[], Error>({
    queryKey: ["lead-portal-image-packages"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalImagePackage[]>(
        "/api/lead-portal/submissions",
      );
      return data;
    },
    staleTime: 30_000,
  });
}
