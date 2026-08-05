import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface EthicalModelingCard {
  pain: string;
  audience: string;
  mechanism: string;
  offerStructure: string;
  angles: string[];
  patterns: string[];
  prohibitedCopies: string[];
}

export interface MetaAdInvestigation {
  id: number;
  workspaceId: string;
  searchTerms: string;
  countryCode: string;
  status: string;
  gateDecision: "INVESTIGAR" | "MODELAR" | "DESCARTAR";
  evidences: string[];
  gaps: string[];
  ethicalModeling: EthicalModelingCard;
  adsObserved: number;
  createdAt: string;
  updatedAt: string;
}

export function useMoisMetaAdInvestigations(workspaceId: string) {
  return useQuery({
    queryKey: ["mois", "meta-ad-investigations", workspaceId],
    queryFn: async () => {
      const { data } = await axios.get<{ items: MetaAdInvestigation[] }>(
        "/api/v1/mois/meta-ad-investigations",
        { params: { workspaceId } },
      );
      return data.items;
    },
  });
}

export function useCreateMoisMetaAdInvestigation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: {
      workspaceId: string;
      searchTerms: string;
      countryCode: string;
    }) => {
      const { data } = await axios.post<MetaAdInvestigation>(
        "/api/v1/mois/meta-ad-investigations",
        request,
      );
      return data;
    },
    onSuccess: (data) =>
      queryClient.invalidateQueries({
        queryKey: ["mois", "meta-ad-investigations", data.workspaceId],
      }),
  });
}

export interface SupervisedMetaAdObservation {
  adReference: string;
  advertiserName: string;
  adLibraryUrl: string;
  adText: string;
  formatType?: string;
  mediaUrl?: string;
  destinationUrl?: string;
  pageActive: boolean;
  commercialSignal: boolean;
}

export function useRegisterSupervisedMetaAdObservation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      investigationId,
      observation,
    }: {
      investigationId: number;
      observation: SupervisedMetaAdObservation;
    }) => {
      await axios.post(
        `/api/v1/mois/meta-ad-investigations/${investigationId}/observations`,
        observation,
      );
      return investigationId;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["mois", "meta-ad-investigations"],
      }),
  });
}
