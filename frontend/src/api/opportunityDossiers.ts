import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type DossierStatus =
  | "RESEARCHING"
  | "UNDER_REVIEW"
  | "READY_FOR_TEST"
  | "APPROVED"
  | "DISCARDED"
  | "CONVERTED_TO_PLAN";
export interface OpportunityDossier {
  id: number;
  title: string;
  ownerAgentKey: string;
  status: DossierStatus;
  targetAudience: string;
  mainPain: string;
  referenceProduct: string;
  aiAdvantage: string;
  proposedOffer?: string;
  preliminaryPrice?: number;
  deliveryModel?: string;
  knownRisks?: string;
  experimentRecommendation?: string;
  convertedPlanId?: number;
  productDiscoveryCycleId?: number;
  evidence: {
    id: number;
    sourceUrl: string;
    summary: string;
    createdBy: string;
    createdAt: string;
  }[];
  reviews: {
    id: number;
    agentKey: string;
    decision?: string;
    rationale?: string;
    risks?: string;
    recommendation?: string;
    executionStatus: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
    errorMessage?: string;
    startedAt?: string;
    completedAt?: string;
  }[];
}
export type CreateDossier = Pick<
  OpportunityDossier,
  | "title"
  | "ownerAgentKey"
  | "targetAudience"
  | "mainPain"
  | "referenceProduct"
  | "aiAdvantage"
  | "proposedOffer"
  | "deliveryModel"
  | "knownRisks"
  | "experimentRecommendation"
> & { preliminaryPrice?: number };

export function useOpportunityDossiers() {
  return useQuery({
    queryKey: ["opportunity-dossiers"],
    queryFn: async () =>
      (await axios.get<OpportunityDossier[]>("/api/opportunity-dossiers")).data,
    refetchInterval: 15_000,
  });
}
export function useCreateOpportunityDossier() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateDossier) =>
      (
        await axios.post<OpportunityDossier>(
          "/api/opportunity-dossiers",
          payload,
        )
      ).data,
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["opportunity-dossiers"] }),
  });
}
export function useDossierAction() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      path,
      payload,
    }: {
      id: number;
      path: string;
      payload: unknown;
    }) =>
      (
        await axios.request<OpportunityDossier>({
          method: path === "status" ? "patch" : "post",
          url: `/api/opportunity-dossiers/${id}/${path}`,
          data: payload,
        })
      ).data,
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["opportunity-dossiers"] }),
  });
}
