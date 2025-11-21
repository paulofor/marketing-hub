import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalExperimentMetrics {
  experimentId: number;
  experimentName: string;
  leadsAccessed: number;
  leadsWithImage: number;
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
