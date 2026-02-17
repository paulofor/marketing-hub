import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentFacebookApiLog {
  id: number;
  jobId?: number | null;
  jobType?: string | null;
  jobWorker?: string | null;
  jobStatus?: string | null;
  workflowId?: number | null;
  resourceId?: number | null;
  provider?: string | null;
  endpoint?: string | null;
  httpMethod?: string | null;
  statusCode?: number | null;
  errorMessage?: string | null;
  requestedAt?: string | null;
  respondedAt?: string | null;
  durationMs?: number | null;
  requestPayload?: string | null;
  responsePayload?: string | null;
  createdAt?: string | null;
}

export function useExperimentFacebookApiLogs(experimentId?: string, limit = 100) {
  return useQuery<ExperimentFacebookApiLog[]>({
    queryKey: ["experiment-facebook-api-logs", experimentId, limit],
    enabled: Boolean(experimentId),
    staleTime: 60_000,
    queryFn: async () => {
      if (!experimentId) throw new Error("Parâmetro experimentId é obrigatório");
      const { data } = await axios.get<ExperimentFacebookApiLog[]>(
        `/api/experiments/${experimentId}/facebook-api-logs`,
        {
          params: { limit },
        },
      );
      return data;
    },
  });
}
