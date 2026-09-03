import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type VideoProviderCreditBalance = {
  provider: string;
  status:
    | "AVAILABLE"
    | "LOW"
    | "INSUFFICIENT"
    | "DIVERGENT_PROVIDER_REJECTION"
    | "NO_PURCHASE_RECORDED"
    | "UNKNOWN_CONSUMPTION";
  balanceNature: string;
  purchasedCredits: number;
  estimatedConsumedCredits: number | null;
  estimatedAvailableCredits: number | null;
  referenceModel: string | null;
  referenceClipSeconds: number | null;
  referenceClipCredits: number | null;
  estimatedReferenceClips: number | null;
  lastPurchaseAt: string | null;
  lastCreditFailureAt: string | null;
  lastCreditFailureJobId: number | null;
  lastCreditFailureDetail: string | null;
  knownConsumedCostUsd: number;
  unknownCostAttempts: number;
  acceptedSceneRequests: number;
  sceneRequests: Array<{
    jobId: number;
    productionCycleId: number | null;
    sceneNumber: number;
    plannedSceneCount: number;
    providerTaskId: string | null;
    model: string | null;
    durationSeconds: number | null;
    estimatedCredits: number | null;
    estimatedCostUsd: number | null;
    billedCredits: number | null;
    billedCostUsd: number | null;
    settlementStatus:
      | "CHARGED"
      | "REFUNDED"
      | "CONTRACTUAL_CHARGE"
      | "CONTRACTUAL_REFUND"
      | null;
    settlementBasis:
      "PROVIDER_REPORTED" | "CONTRACTUAL_RATE_CARD" | "UNKNOWN" | null;
    billingEvidence: string | null;
    acceptedAt: string;
  }>;
  creditsUrl: string | null;
  aggregatorName: string | null;
  accountKey: string | null;
  officialSnapshotStatus: string;
  officialBalanceCredits: number | null;
  reservedCredits: number | null;
  officialAvailableCredits: number | null;
  maxMonthlyCreditSpend: number | null;
  quotaSnapshotJson: string | null;
  officialObservedAt: string | null;
  officialExpiresAt: string | null;
  officialSourceUrl: string | null;
};

/** Consulta a fonte financeira transversal dos saldos de provedores de vídeo. */
export function useVideoProviderCreditBalances() {
  return useQuery({
    queryKey: ["financial-video-provider-credit-balances"],
    queryFn: async () => {
      const { data } = await axios.get<VideoProviderCreditBalance[]>(
        "/api/financial-agent/v1/video-providers/credit-balances",
      );
      return data;
    },
    refetchInterval: 60_000,
  });
}
