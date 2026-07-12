import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentSalesPageAbVariant {
  id: number;
  variantKey: string;
  name: string;
  variantType: "TRADITIONAL" | "HUMAN_VIDEO" | string;
  status: string;
  trafficWeight?: number | string | null;
  salesPageUrl?: string | null;
  checkoutUrl?: string | null;
  adDestinationUrl?: string | null;
  analyticsVariantParam?: string | null;
  publicationAuditId?: number | null;
  experimentVideoAssetId?: number | null;
  requiredCollectorsPresent: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ExperimentSalesPageAbTest {
  id: number;
  experimentId: number;
  name: string;
  status: string;
  hypothesis?: string | null;
  primaryMetric?: string | null;
  secondaryMetrics?: string | null;
  winnerRule?: string | null;
  minimumRuntimeDays?: number | null;
  minimumSampleSize?: number | null;
  metaSplitTestRecommended: boolean;
  notes?: string | null;
  variants: ExperimentSalesPageAbVariant[];
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ExperimentSalesPageAbVariantResult {
  variant: ExperimentSalesPageAbVariant;
  pageViews: number;
  sessions: number;
  checkoutClicks: number;
  purchases: number;
  checkoutClickRate: number | string;
  purchaseRate: number | string;
  lastEventAt?: string | null;
}

export interface ExperimentSalesPageAbTestResult {
  test: ExperimentSalesPageAbTest;
  variants: ExperimentSalesPageAbVariantResult[];
  winnerVariantKey?: string | null;
  status: string;
  recommendation: string;
}

export function useExperimentSalesPageAbResults(experimentId?: string) {
  return useQuery<ExperimentSalesPageAbTestResult[]>({
    queryKey: ["experiment", experimentId, "sales-page-ab-results"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSalesPageAbTestResult[]>(
        `/api/experiments/${experimentId}/sales-page-ab-tests/results`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
