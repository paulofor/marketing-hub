import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PdeProductionSlotStatus } from "../experiment/usePostDeployMonitor";

export type PdeVersionLifecycleStepStatus =
  "DONE" | "CURRENT" | "PENDING" | "BLOCKED";

export interface PdeVersionLifecycleStep {
  code: string;
  label: string;
  status: PdeVersionLifecycleStepStatus;
}

export interface ProductPdeVersionOverview {
  id: number;
  slotCode: string;
  name: string;
  experienceVersion: string;
  lifecycleStage:
    "DRAFT" | "CANDIDATE" | "HOMOLOGATED" | "PUBLISHED" | "PAUSED" | "ARCHIVED";
  lifecycleLabel: string;
  operationalStatus: PdeProductionSlotStatus;
  hypothesis?: string | null;
  primaryChange?: string | null;
  sourceExperimentId?: number | null;
  sourceExperimentName?: string | null;
  sourceExperimentStatus?: string | null;
  videoCount: number;
  approvedVideoCount: number;
  priceBrl?: number | null;
  primaryCta?: string | null;
  checkoutUrl?: string | null;
  publicUrl: string;
  validationStatus?: string | null;
  validationSummary?: string | null;
  validationCheckedAt?: string | null;
  homologationSummary: string;
  publishedContract: boolean;
  pendingItems: string[];
  lifecycle: PdeVersionLifecycleStep[];
  updatedAt?: string | null;
}

export function useProductPdeVersions(productId?: string | number) {
  return useQuery<ProductPdeVersionOverview[]>({
    queryKey: ["products", productId, "pde-versions"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductPdeVersionOverview[]>(
        `/api/products/${productId}/pde-versions`,
      );
      return data;
    },
  });
}
