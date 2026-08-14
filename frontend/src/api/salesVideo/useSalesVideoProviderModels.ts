import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type SalesVideoProviderModel = {
  id: number;
  code: string;
  displayName: string;
  providerName: string;
  providerFamily: "OPENAI" | "EXTERNAL_VIDEO_MODULE";
  adapterKey: string;
  externalModelId: string;
  recommendedUse: string;
  lifecycleStatus: "DRAFT" | "HOMOLOGATION" | "ACTIVE" | "BLOCKED";
  clipDurationSeconds: number;
  maxDirectDurationSeconds: number;
  supportsHeroVideo: boolean;
  supportsSceneAssembly: boolean;
  requiresSourceImage: boolean;
  creditsUrl?: string | null;
  documentationUrl: string;
  adapterVerified: boolean;
  pricingVerified: boolean;
  commercialLicenseVerified: boolean;
  qualityGateVerified: boolean;
  notes?: string | null;
  pricingAmountUsd?: number | null;
  pricingUnit?: "SECOND" | "VIDEO" | "CREDIT" | null;
  pricingQuantity?: number | null;
  pricingResolution?: string | null;
  pricingIncludesAudio?: boolean | null;
  pricingSourceUrl?: string | null;
  pricingObservedAt?: string | null;
  pricingResearchStatus?: "PENDING" | "VERIFIED" | "INCOMPARABLE" | "BLOCKED";
  pricingResearchNotes?: string | null;
  normalizedCostPerSecondUsd?: number | null;
  pricingStale?: boolean;
};

export type UpdateSalesVideoProviderModel = Pick<
  SalesVideoProviderModel,
  | "recommendedUse"
  | "lifecycleStatus"
  | "adapterVerified"
  | "pricingVerified"
  | "commercialLicenseVerified"
  | "qualityGateVerified"
  | "notes"
>;

export function useSalesVideoProviderModels() {
  return useQuery({
    queryKey: ["sales-video-provider-models"],
    queryFn: async () =>
      (
        await axios.get<SalesVideoProviderModel[]>(
          "/api/sales-videos/provider-models",
        )
      ).data,
  });
}

export function useUpdateSalesVideoProviderModel(modelId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateSalesVideoProviderModel) =>
      (
        await axios.patch<SalesVideoProviderModel>(
          `/api/sales-videos/provider-models/${modelId}`,
          payload,
        )
      ).data,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["sales-video-provider-models"],
      }),
  });
}
