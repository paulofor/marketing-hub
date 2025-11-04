import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalFlowQuestion {
  id: number;
  title: string;
  dataKey: string;
  type: string;
  required: boolean;
  description?: string | null;
  placeholder?: string | null;
  position: number;
  options: string[];
}

export interface LeadPortalFlow {
  id: number;
  name: string;
  slug: string;
  publicUrl?: string | null;
  description?: string | null;
  model?: string | null;
  prompt?: string | null;
  approved: boolean;
  approvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  questions: LeadPortalFlowQuestion[];
}

export function useLeadPortalFlows() {
  return useQuery({
    queryKey: ["lead-portal-flows"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalFlow[]>("/api/lead-portal-flows");
      return data;
    },
  });
}
