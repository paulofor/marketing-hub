import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { ExperimentAdSetJob } from "./useExperimentAdSetWorkflow";

export interface ExperimentAdSetJobApiLog {
  id: number;
  provider: string;
  endpoint?: string | null;
  httpMethod?: string | null;
  statusCode?: number | null;
  requestPayload?: string | null;
  responsePayload?: string | null;
  errorMessage?: string | null;
  requestedAt?: string | null;
  respondedAt?: string | null;
  createdAt?: string | null;
}

export interface ExperimentAdSetJobDetailDto {
  job: ExperimentAdSetJob;
  apiLogs: ExperimentAdSetJobApiLog[];
}

export function useExperimentAdSetJobDetail(experimentId?: string, jobId?: string) {
  return useQuery<ExperimentAdSetJobDetailDto>({
    queryKey: ["experiment-adset-job-detail", experimentId, jobId],
    enabled: Boolean(experimentId && jobId),
    queryFn: async () => {
      if (!experimentId || !jobId) throw new Error("Parâmetros obrigatórios ausentes");
      const { data } = await axios.get<ExperimentAdSetJobDetailDto>(
        `/api/experiments/${experimentId}/adset-playbook/jobs/${jobId}`,
      );
      return data;
    },
    refetchOnWindowFocus: false,
  });
}
