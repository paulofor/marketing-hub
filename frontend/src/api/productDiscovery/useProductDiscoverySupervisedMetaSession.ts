import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ProductDiscoveryMetaAdEvidence = {
  metaAdId: string;
  advertiserName?: string;
  adTexts: string[];
  publisherPlatforms: string[];
  formatTypes: string[];
  destinationUrl?: string;
  snapshotUrl?: string;
  active: boolean;
  commercialSignal: boolean;
  observations: number;
  longevityDays: number;
  sustainedInvestmentSignal: boolean;
  evidenceConfidence: "LOW" | "MEDIUM" | "HIGH";
  firstObservedAt: string;
  lastObservedAt: string;
};

export type ProductDiscoverySupervisedMetaSession = {
  cycleId: number;
  investigationId: number;
  cycleStatus: string;
  query: string;
  country: string;
  publisherPlatform: "INSTAGRAM";
  sourceStatus: string;
  collectionMode: "SUPERVISED";
  collectionReason: string;
  searchUrl: string;
  nextObservationAt?: string;
  adsObserved: number;
  activeAds: number;
  advertisersObserved: number;
  latestObservationAt?: string;
  interpretation: string;
  canRegisterObservation: boolean;
  canResume: boolean;
  resumeReason: string;
  items: ProductDiscoveryMetaAdEvidence[];
};

export type ProductDiscoverySupervisedMetaObservation = {
  adReference: string;
  advertiserName: string;
  adLibraryUrl: string;
  adText: string;
  publisherPlatforms: Array<"INSTAGRAM" | "FACEBOOK">;
  formatType?: string;
  mediaUrl?: string;
  destinationUrl?: string;
  pageActive: boolean;
  commercialSignal: boolean;
};

const sessionKey = (cycleId: number) => [
  "product-discovery",
  "supervised-meta-session",
  cycleId,
];

export function useProductDiscoverySupervisedMetaSession(cycleId: number) {
  return useQuery({
    queryKey: sessionKey(cycleId),
    queryFn: async () =>
      (
        await axios.get<ProductDiscoverySupervisedMetaSession>(
          `/api/product-discovery/v1/cycles/${cycleId}/supervised-meta-session`,
        )
      ).data,
    retry: false,
    refetchInterval: (query) =>
      ["READY_FOR_RESEARCH", "RESEARCHING"].includes(
        query.state.data?.cycleStatus ?? "",
      )
        ? 5000
        : false,
  });
}

export function useObserveProductDiscoveryMetaAd(cycleId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (
      observation: ProductDiscoverySupervisedMetaObservation,
    ) =>
      (
        await axios.post<ProductDiscoverySupervisedMetaSession>(
          `/api/product-discovery/v1/cycles/${cycleId}/supervised-meta-session/observations`,
          observation,
        )
      ).data,
    onSuccess: (session) => client.setQueryData(sessionKey(cycleId), session),
  });
}

export function useResumeProductDiscoveryWithMetaEvidence(cycleId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      (
        await axios.post<ProductDiscoverySupervisedMetaSession>(
          `/api/product-discovery/v1/cycles/${cycleId}/supervised-meta-session/resume`,
        )
      ).data,
    onSuccess: (session) => {
      client.setQueryData(sessionKey(cycleId), session);
      void client.invalidateQueries({
        queryKey: ["independent-business-process-executions"],
      });
    },
  });
}
