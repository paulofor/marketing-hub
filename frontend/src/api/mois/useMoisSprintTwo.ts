import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  MoisBuildOfferRequest,
  MoisBuildOfferResponse,
  MoisComparisonRequest,
  MoisComparisonResponse,
  MoisExtractionDraftPayload,
  MoisExtractionDraftResponse,
  MoisLibraryActionResponse,
  MoisLibraryBlockListResponse,
} from "./types";

export function useMoisExtractionDraft(referenceId: string) {
  return useMutation({
    mutationFn: async (payload: MoisExtractionDraftPayload) => {
      const { data } = await axios.post<MoisExtractionDraftResponse>(
        `/api/v1/mois/references/${referenceId}/extractions`,
        payload,
      );
      return data;
    },
  });
}

export function useMoisLibraryBlocks(workspaceId: string, niche = "", formatType = "") {
  return useQuery({
    queryKey: ["mois", "library", workspaceId, niche, formatType],
    queryFn: async () => {
      const { data } = await axios.get<MoisLibraryBlockListResponse>("/api/v1/mois/library/blocks", {
        params: { workspaceId, niche, formatType },
      });
      return data.items;
    },
  });
}

export function useFavoriteMoisBlock() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (blockId: string) => {
      const { data } = await axios.post<MoisLibraryActionResponse>(`/api/v1/mois/library/blocks/${blockId}/favorite`);
      return data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["mois", "library"] });
    },
  });
}

export function useDuplicateMoisBlock() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (blockId: string) => {
      const { data } = await axios.post<MoisLibraryActionResponse>(`/api/v1/mois/library/blocks/${blockId}/duplicate`);
      return data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["mois", "library"] });
    },
  });
}

export function useCreateMoisComparison() {
  return useMutation({
    mutationFn: async (payload: MoisComparisonRequest) => {
      const { data } = await axios.post<MoisComparisonResponse>("/api/v1/mois/comparisons", payload);
      return data;
    },
  });
}

export function useBuildMoisOffer() {
  return useMutation({
    mutationFn: async (payload: MoisBuildOfferRequest) => {
      const { data } = await axios.post<MoisBuildOfferResponse>("/api/v1/mois/offers/build", payload);
      return data;
    },
  });
}
