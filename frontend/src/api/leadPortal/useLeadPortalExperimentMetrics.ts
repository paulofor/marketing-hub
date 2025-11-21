import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalExperimentLead {
  displayName: string;
  email?: string;
  phone?: string;
  sentImage: boolean;
}

export interface LeadPortalExperimentMetrics {
  experimentId: number;
  experimentName: string;
  leadsAccessed: number;
  leadsWithImage: number;
  uniqueLeads: LeadPortalExperimentLead[];
}

export function useLeadPortalExperimentMetrics() {
  return useQuery({
    queryKey: ["lead-portal-experiment-metrics"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalExperimentMetrics[]>(
        "/api/lead-portal/metrics/experiments",
      );
      return data;
    },
  });
}
