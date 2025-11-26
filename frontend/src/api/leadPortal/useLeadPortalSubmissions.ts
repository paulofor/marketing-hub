import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ImageDeliverableStatus } from "../imageDeliverable/types";

export interface LeadPortalImagePackage {
  id: number;
  leadId: string;
  flowSlug?: string | null;
  name?: string | null;
  email?: string | null;
  phone?: string | null;
  prompt: string;
  status: ImageDeliverableStatus;
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
