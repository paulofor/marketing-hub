import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalSubmission {
  id: string;
  flowSlug: string;
  name: string;
  email: string;
  imageUrl?: string | null;
  createdAt: string;
}

export function useLeadPortalSubmissions() {
  return useQuery<LeadPortalSubmission[], Error>({
    queryKey: ["lead-portal-submissions"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalSubmission[]>(
        "/lead-portal/submissions",
      );
      return data;
    },
    staleTime: 30_000,
  });
}
