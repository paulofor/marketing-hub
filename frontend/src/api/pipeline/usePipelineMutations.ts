import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  Pipeline,
  PipelinePayload,
  PipelineStage,
  PipelineStagePayload,
  PipelineSyncResult,
} from "./types";

export function useCreatePipeline() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PipelinePayload) => {
      const { data } = await axios.post<Pipeline>("/api/pipelines", payload);
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useUpdatePipeline() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      payload,
    }: {
      id: number;
      payload: PipelinePayload;
    }) => {
      const { data } = await axios.put<Pipeline>(
        `/api/pipelines/${id}`,
        payload,
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useDeletePipeline() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/pipelines/${id}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useCreatePipelineStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      pipelineId,
      payload,
    }: {
      pipelineId: number;
      payload: PipelineStagePayload;
    }) => {
      const { data } = await axios.post<PipelineStage>(
        `/api/pipelines/${pipelineId}/stages`,
        payload,
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useUpdatePipelineStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      pipelineId,
      stageId,
      payload,
    }: {
      pipelineId: number;
      stageId: number;
      payload: PipelineStagePayload;
    }) => {
      const { data } = await axios.put<PipelineStage>(
        `/api/pipelines/${pipelineId}/stages/${stageId}`,
        payload,
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useDeletePipelineStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      pipelineId,
      stageId,
    }: {
      pipelineId: number;
      stageId: number;
    }) => {
      await axios.delete(`/api/pipelines/${pipelineId}/stages/${stageId}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useRebuildOfficialPipelineStages() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (pipelineId: number) => {
      const { data } = await axios.post<PipelineSyncResult>(
        `/api/pipelines/${pipelineId}/rebuild-official-stages`,
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}

export function useSyncOfficialPipeline() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => {
      const { data } = await axios.post<PipelineSyncResult>(
        `/api/pipelines/official/${encodeURIComponent(code)}/sync`,
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pipelines"] }),
  });
}
