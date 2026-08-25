import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  AnalyzeVideoReferencePayload,
  VideoReference,
  VideoReferenceAnalysisExecution,
  VideoReferencePayload,
  VideoReferenceUploadPayload,
} from "./types";

export function useVideoReferences() {
  return useQuery({
    queryKey: ["sales-video-references"],
    queryFn: async () => {
      const { data } = await axios.get<VideoReference[]>(
        "/api/sales-videos/reference-videos",
      );
      return data;
    },
  });
}

export function useLatestVideoReferenceAnalysis(referenceId?: string) {
  return useQuery({
    queryKey: ["sales-video-reference-analysis", referenceId],
    enabled: Boolean(referenceId),
    retry: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "QUEUED" || status === "RUNNING" ? 5000 : false;
    },
    queryFn: async () => {
      const { data } = await axios.get<VideoReferenceAnalysisExecution>(
        `/api/sales-videos/reference-analysis/v1/references/${referenceId}/latest`,
      );
      return data;
    },
  });
}

export function useRetryVideoReferenceAnalysis(referenceId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<VideoReferenceAnalysisExecution>(
        `/api/sales-videos/reference-analysis/v1/references/${referenceId}/retry`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["sales-video-reference-analysis", referenceId],
      });
      queryClient.invalidateQueries({ queryKey: ["sales-video-references"] });
    },
  });
}

export function useVideoReference(referenceId?: string) {
  return useQuery({
    queryKey: ["sales-video-reference", referenceId],
    enabled: Boolean(referenceId),
    queryFn: async () => {
      const { data } = await axios.get<VideoReference>(
        `/api/sales-videos/reference-videos/${referenceId}`,
      );
      return data;
    },
  });
}

export function useCreateVideoReference() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (
      payload: VideoReferencePayload | VideoReferenceUploadPayload,
    ) => {
      if ("file" in payload) {
        const formData = new FormData();
        formData.append("file", payload.file);
        formData.append("title", payload.title);
        formData.append("primaryLearningGoal", payload.primaryLearningGoal);
        if (payload.sourcePlatform) {
          formData.append("sourcePlatform", payload.sourcePlatform);
        }
        if (payload.niche) {
          formData.append("niche", payload.niche);
        }
        if (payload.funnelStage) {
          formData.append("funnelStage", payload.funnelStage);
        }
        if (payload.successEvidence) {
          formData.append("successEvidence", payload.successEvidence);
        }
        if (payload.createdBy) {
          formData.append("createdBy", payload.createdBy);
        }

        const { data } = await axios.post<VideoReference>(
          "/api/sales-videos/reference-videos",
          formData,
        );
        return data;
      }

      const { data } = await axios.post<VideoReference>(
        "/api/sales-videos/reference-videos",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-references"] });
    },
  });
}

export function useAnalyzeVideoReference(referenceId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: AnalyzeVideoReferencePayload) => {
      const { data } = await axios.patch<VideoReference>(
        `/api/sales-videos/reference-videos/${referenceId}/analysis`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["sales-video-reference", referenceId],
      });
      queryClient.invalidateQueries({ queryKey: ["sales-video-references"] });
    },
  });
}
