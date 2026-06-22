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
  technicalAccessesFiltered: number;
  leadsWithImage: number;
  uniqueLeads: LeadPortalExperimentLead[];
  sampleEmailsGenerated: number;
  selectedSampleEmailId?: number | null;
  selectedSampleEmailSubject?: string | null;
  selectedSampleEmailPreviewText?: string | null;
  selectedSampleEmailCallToAction?: string | null;
  selectedSampleEmailUpdatedAt?: string | null;
  packagesWithWatermark: number;
  packagesNotified: number;
  lastPackageNotificationAt?: string | null;
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
