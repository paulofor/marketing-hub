import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  MoisCollectedReferenceActionResponse,
  MoisCollectedReferenceLineageResponse,
  MoisCollectedReferenceListResponse,
  MoisCollectionJob,
  MoisCreateCollectionJobPayload,
} from "./types";

interface MoisCollectionFilters {
  source?: string;
  niche?: string;
  minSuccessScore?: number;
  confidenceLevel?: string;
}

export function useCreateMoisCollectionJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: MoisCreateCollectionJobPayload) => {
      const { data } = await axios.post<MoisCollectionJob>("/api/v1/mois/collection-jobs", payload);
      return data;
    },
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: ["mois", "collection-jobs", data.workspaceId] });
    },
  });
}

export function useMoisCollectedReferences(jobId: string, filters: MoisCollectionFilters) {
  return useQuery({
    queryKey: ["mois", "collection-references", jobId, filters.source, filters.niche, filters.minSuccessScore, filters.confidenceLevel],
    enabled: Boolean(jobId),
    queryFn: async () => {
      const { data } = await axios.get<MoisCollectedReferenceListResponse>(`/api/v1/mois/collection-jobs/${jobId}/references`, {
        params: filters,
      });
      return data.items;
    },
  });
}

function useCollectedReferenceAction(action: "favorite" | "discard" | "import" | "import-and-start-extraction") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ jobId, referenceId }: { jobId: string; referenceId: string }) => {
      const { data } = await axios.post<MoisCollectedReferenceActionResponse>(
        `/api/v1/mois/collection-jobs/${jobId}/references/${referenceId}/${action}`,
      );
      return { data, jobId };
    },
    onSuccess: async ({ jobId }) => {
      await queryClient.invalidateQueries({ queryKey: ["mois", "collection-references", jobId] });
      await queryClient.invalidateQueries({ queryKey: ["mois", "collection-lineage", jobId] });
      await queryClient.invalidateQueries({ queryKey: ["mois", "references"] });
      await queryClient.invalidateQueries({ queryKey: ["mois", "library"] });
    },
  });
}

export const useFavoriteMoisCollectedReference = () => useCollectedReferenceAction("favorite");
export const useDiscardMoisCollectedReference = () => useCollectedReferenceAction("discard");
export const useImportMoisCollectedReference = () => useCollectedReferenceAction("import");
export const useImportAndStartExtractionMoisCollectedReference = () =>
  useCollectedReferenceAction("import-and-start-extraction");

export function useMoisCollectedReferenceLineage(jobId: string, referenceId: string) {
  return useQuery({
    queryKey: ["mois", "collection-lineage", jobId, referenceId],
    enabled: Boolean(jobId) && Boolean(referenceId),
    queryFn: async () => {
      const { data } = await axios.get<MoisCollectedReferenceLineageResponse>(
        `/api/v1/mois/collection-jobs/${jobId}/references/${referenceId}/lineage`,
      );
      return data;
    },
  });
}
