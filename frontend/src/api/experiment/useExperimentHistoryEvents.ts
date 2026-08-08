import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentHistoryCategory =
  "OBSERVACAO" | "INCIDENTE" | "DECISAO" | "CORRECAO" | "APRENDIZADO";

export interface ExperimentHistoryEvent {
  id: number;
  category: ExperimentHistoryCategory;
  title: string;
  description: string;
  evidenceJson?: string | null;
  source: string;
  occurredAt: string;
  createdAt: string;
}

export interface CreateExperimentHistoryEvent {
  category: ExperimentHistoryCategory;
  title: string;
  description: string;
  evidenceJson?: string | null;
  source?: string | null;
  occurredAt?: string | null;
}

export function useExperimentHistoryEvents(experimentId: string) {
  return useQuery({
    queryKey: ["experiment-history-events", experimentId],
    queryFn: async () =>
      (
        await axios.get<ExperimentHistoryEvent[]>(
          `/api/experiments/${experimentId}/history-events`,
        )
      ).data,
  });
}

export function useCreateExperimentHistoryEvent(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateExperimentHistoryEvent) =>
      (
        await axios.post<ExperimentHistoryEvent>(
          `/api/experiments/${experimentId}/history-events`,
          payload,
        )
      ).data,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["experiment-history-events", experimentId],
      }),
  });
}
